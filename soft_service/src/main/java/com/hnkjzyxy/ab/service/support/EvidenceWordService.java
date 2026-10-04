package com.hnkjzyxy.ab.service.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.config.PdfToWordProperties;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import com.hnkjzyxy.ab.utils.PdfToWordConverter;
import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 佐证材料 Word 生成服务。
 * <p>
 * 仅汇总 evidence 明确选中的材料；成功文件由下载组件回收，失败文件由本服务回收。
 * </p>
 */
@Slf4j
@Service
public class EvidenceWordService {
    /** 佐证材料存储根目录 */
    private final String fileRoot;
    /** 版式与生成限制 */
    private final PdfToWordProperties properties;
    /** 每实例共享的转换许可，下载过程不占用 */
    private final Semaphore exportPermits;
    /** 严格解析材料数组，拒绝 JSON 后的额外内容 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * 初始化生成服务。
     *
     * @param fileRoot upload.fileUrl 配置指定的根目录
     * @param properties Word 生成参数
     */
    public EvidenceWordService(@Value("${upload.fileUrl}") String fileRoot, PdfToWordProperties properties) {
        properties.validate();
        this.fileRoot = fileRoot;
        this.properties = properties;
        this.exportPermits = new Semaphore(properties.getMaxConcurrentExports());
    }

    /**
     * 校验所选路径并一次生成完整的临时 Word。
     *
     * @param evidences /pdf/file/ 开头的材料访问路径 JSON 字符串数组
     * @return 已完成写入并关闭的 Word，调用方负责删除
     * @throws PdfConversionException 输入、资源不足或转换失败时抛出
     */
    public File generate(String evidences) {
        long startedAt = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        if (!exportPermits.tryAcquire()) {
            throw new PdfConversionException(503, "busy", "Word 导出繁忙，请稍后重试");
        }
        File temporary = null;
        boolean success = false;
        int fileCount = 0;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("evidenceWordRequest", requestId)) {
            List<String> paths = parseEvidence(evidences);
            fileCount = paths.size();
            PdfToWordConverter.checkBudget(startedAt, properties);
            if (configuredMaxHeapBytes() < properties.getMinJvmMaxHeapMb() * 1024L * 1024L) {
                throw new PdfConversionException(503, "heap", "Word 导出资源不足，请联系管理员");
            }
            List<Path> files = resolveEvidence(paths, startedAt);
            probeTemporaryStorage();
            PdfToWordConverter.checkBudget(startedAt, properties);
            temporary = createTemporaryWord();
            PdfToWordConverter.convertPdfFilesToWord(files, temporary.toPath(), properties, startedAt);
            success = true;
            log.info("Word 生成成功：请求={}，材料项数={}，耗时={}ms", requestId, fileCount,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            return temporary;
        } catch (PdfConversionException e) {
            log.warn("Word 生成失败：请求={}，材料项数={}，阶段={}，材料={}，页码={}，耗时={}ms",
                    requestId, fileCount, e.getStage(), e.getMaterial(), e.getPageNumber(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), e);
            throw e;
        } catch (Exception e) {
            log.error("Word 生成失败：请求={}，材料项数={}，耗时={}ms", requestId, fileCount,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), e);
            throw new PdfConversionException(500, "storage", null, null,
                    "Word 文件生成失败，请联系管理员", e);
        } finally {
            if (!success && temporary != null) {
                try {
                    Files.deleteIfExists(temporary.toPath());
                } catch (IOException | SecurityException e) {
                    log.warn("生成失败后的临时 Word 删除失败：请求={}，文件={}", requestId, temporary, e);
                }
            }
            exportPermits.release();
        }
    }

    /**
     * 使用树模型严格校验 JSON 数组与每个字符串元素。
     *
     * @param evidences 原始 JSON
     * @return 保持原顺序的路径列表
     */
    private List<String> parseEvidence(String evidences) {
        if (evidences == null || evidences.trim().isEmpty()) {
            throw new PdfConversionException(400, "input", "请选择至少一份 PDF 佐证材料");
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(evidences);
        } catch (IOException e) {
            throw new PdfConversionException(400, "input", null, null, "佐证材料列表格式不合法", e);
        }
        if (node == null || !node.isArray()) {
            throw new PdfConversionException(400, "input", "佐证材料列表必须是字符串数组");
        }
        if (node.size() == 0) {
            throw new PdfConversionException(400, "input", "请选择至少一份 PDF 佐证材料");
        }
        if (node.size() > properties.getMaxFiles()) {
            throw new PdfConversionException(413, "input", "佐证材料超过 " + properties.getMaxFiles() + " 项上限，请分批导出");
        }
        List<String> paths = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.isTextual() || value.textValue().trim().isEmpty()) {
                throw new PdfConversionException(400, "input", "佐证材料路径不合法");
            }
            paths.add(value.textValue());
        }
        return paths;
    }

    /**
     * 映射历史正反斜杠路径并检查真实文件仍位于上传根目录内。
     *
     * @param paths 保持原始顺序的访问路径
     * @param startedAt 请求开始时间
     * @return 待转换的本地 PDF 路径
     * @throws IOException 上传根目录读取失败时抛出
     */
    private List<Path> resolveEvidence(List<String> paths, long startedAt) throws IOException {
        List<Path> relatives = new ArrayList<>();
        for (String address : paths) {
            String normalized = address.replace('\\', '/');
            String prefix = "/pdf/file/";
            if (!normalized.startsWith(prefix)) {
                throw new PdfConversionException(400, "input", "佐证材料路径不合法");
            }
            String relative = normalized.substring(prefix.length());
            for (String segment : relative.split("/", -1)) {
                if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                        || segment.indexOf(':') >= 0 || segment.indexOf('\0') >= 0) {
                    throw new PdfConversionException(400, "input", "佐证材料路径不合法");
                }
            }
            try {
                Path path = Paths.get(relative);
                if (path.isAbsolute()) {
                    throw new PdfConversionException(400, "input", "佐证材料路径不合法");
                }
                relatives.add(path);
            } catch (InvalidPathException e) {
                throw new PdfConversionException(400, "input", null, null, "佐证材料路径不合法", e);
            }
        }
        Path root = Paths.get(fileRoot).toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || !Files.isReadable(root)) {
            throw new PdfConversionException(500, "input", "佐证材料存储目录不可读取，请联系管理员");
        }
        Path realRoot = root.toRealPath();
        List<Path> files = new ArrayList<>();
        for (Path relative : relatives) {
            PdfToWordConverter.checkBudget(startedAt, properties);
            Path resolved = root.resolve(relative).normalize();
            if (!resolved.startsWith(root)) {
                throw new PdfConversionException(400, "input", "佐证材料路径不合法");
            }
            Path real = PdfToWordConverter.validatePdfInput(resolved);
            if (!real.startsWith(realRoot)) {
                throw new PdfConversionException(400, "input", "佐证材料路径不合法");
            }
            files.add(real);
        }
        return files;
    }

    /**
     * 探测 JVM 临时目录的创建、写入与删除权限，不使用上传目录。
     *
     * @throws IOException 临时存储不可用时抛出
     */
    private void probeTemporaryStorage() throws IOException {
        Path directory = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
        Path probe = Files.createTempFile(directory, "evidence-word-probe", ".tmp");
        try {
            Files.write(probe, new byte[]{0});
        } finally {
            Files.deleteIfExists(probe);
        }
    }

    /**
     * 读取实际最大堆配置，避免 Java 8 Parallel GC 扣除 Survivor 后误拒绝 -Xmx1024m。
     * <p>
     * HotSpot 使用 MaxHeapSize；其他 JVM 或诊断接口不可用时保守采用运行时报告值。
     * </p>
     *
     * @return JVM 最大堆字节数
     */
    private long configuredMaxHeapBytes() {
        try {
            HotSpotDiagnosticMXBean diagnostic = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            if (diagnostic != null) {
                return Long.parseLong(diagnostic.getVMOption("MaxHeapSize").getValue());
            }
        } catch (RuntimeException | LinkageError e) {
            log.debug("无法读取 JVM 最大堆配置，使用运行时报告值", e);
        }
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 创建本请求独占的输出文件，保留原下载文件名前缀。
     *
     * @return 独占的临时 DOCX
     * @throws IOException 临时目录故障时抛出
     */
    protected File createTemporaryWord() throws IOException {
        return File.createTempFile("tempFile", ".docx");
    }
}
