package com.hnkjzyxy.ab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.config.PdfToWordProperties;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import com.hnkjzyxy.ab.service.support.EvidenceWordService;
import com.sun.management.HotSpotDiagnosticMXBean;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证佐证材料集合、路径约束、失败回收以及实例级并发许可。
 */
class EvidenceWordServiceTest {
    /** 独立的上传及输出目录 */
    @TempDir
    Path temporary;

    /**
     * 验证跨目录一次汇总、首次顺序及重复材料去重，不扫描未选材料。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void combinesSelectedMaterialsAcrossDirectoriesWithoutImportingUnselectedFiles() throws Exception {
        pdf("a/中文 材料.pdf", Color.RED, Color.BLUE, Color.YELLOW);
        pdf("b/second.PDF", Color.GREEN, Color.CYAN);
        pdf("a/unselected.pdf", Color.BLACK);
        byte[] originalPng = {1, 9, 7};
        Files.write(temporary.resolve("a/中文 材料.png"), originalPng);
        EvidenceWordService service = service(properties(), temporary);
        String json = json("/pdf/file/b/second.PDF", "\\pdf\\file\\a\\中文 材料.pdf", "/pdf/file/b/second.PDF");
        File output = service.generate(json);
        try (InputStream input = Files.newInputStream(output.toPath()); XWPFDocument word = new XWPFDocument(input)) {
            assertEquals(5, word.getParagraphs().size());
            Color[] expected = {Color.GREEN, Color.CYAN, Color.RED, Color.BLUE, Color.YELLOW};
            for (int index = 0; index < 5; index++) {
                byte[] bytes = word.getParagraphs().get(index).getRuns().get(0).getEmbeddedPictures()
                        .get(0).getPictureData().getData();
                assertEquals(expected[index].getRGB(), ImageIO.read(new ByteArrayInputStream(bytes)).getRGB(20, 20));
            }
        } finally {
            Files.delete(output.toPath());
        }
        assertArrayEquals(originalPng, Files.readAllBytes(temporary.resolve("a/中文 材料.png")));
        try (Stream<Path> entries = Files.list(temporary.resolve("a"))) {
            assertEquals(3, entries.count());
        }
    }

    /**
     * 验证同一文件的硬链接仍只生成一次。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void deduplicatesHardLinksByFileIdentity() throws Exception {
        Path original = pdf("one.pdf", Color.RED, Color.GREEN);
        Path alias = temporary.resolve("alias.pdf");
        Files.createLink(alias, original);
        File output = service(properties(), temporary).generate(json("/pdf/file/alias.pdf", "/pdf/file/one.pdf"));
        try (InputStream input = Files.newInputStream(output.toPath()); XWPFDocument word = new XWPFDocument(input)) {
            assertEquals(2, word.getParagraphs().size());
        } finally {
            Files.delete(output.toPath());
        }
    }

    /**
     * 验证非法 JSON、非字符串元素与空白输入均在创建输出前拒绝。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void rejectsInvalidJsonTypesAndEmptyInputsBeforeFileIo() throws Exception {
        EvidenceWordService service = service(properties(), temporary.resolve("missing-root"));
        for (String invalid : new String[]{null, "", " ", "null", "[]", "{}", "[1]", "[true]", "[null]",
                "[\" \"]", "[", "[] []", "[\"/pdf/file/a.pdf\"] true"}) {
            assertStatus(400, () -> service.generate(invalid));
        }
        assertFalse(Files.exists(temporary.resolve("missing-root")));
    }

    /**
     * 验证路径前缀、越界段、完整 URL、空路径段等错误不泄漏部署目录。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void rejectsUnsafeAddressFormsAndNonPdfSelections() throws Exception {
        EvidenceWordService service = service(properties(), temporary);
        for (String path : new String[]{"/wrong/file/a.pdf", "/pdf/file/", "/pdf/file/../a.pdf",
                "/pdf/file/a/./b.pdf", "/pdf/file//a.pdf", "/pdf/file/a//b.pdf",
                "https://host/pdf/file/a.pdf", "/pdf/file/C:/a.pdf", "/pdf/file/a\u0000.pdf",
                "/pdf/file/a.txt"}) {
            assertStatus(400, () -> service.generate(json(path)));
        }
        Files.createDirectory(temporary.resolve("folder.pdf"));
        assertStatus(400, () -> service.generate(json("/pdf/file/folder.pdf")));
        assertStatus(404, () -> service.generate(json("/pdf/file/missing.pdf")));
    }

    /**
     * 验证合法路径不会被 URL 解码，文件名中的百分号和加号按原样映射。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void preservesLiteralPercentAndPlusInFileNamesAndAcceptsTrailingRootSeparator() throws Exception {
        pdf("%2F+证据.pdf", Color.RED);
        EvidenceWordService service = new EvidenceWordService(temporary + File.separator, properties());
        File output = service.generate(json("/pdf/file/%2F+证据.pdf"));
        assertTrue(output.length() > 0);
        Files.delete(output.toPath());
    }

    /**
     * 验证目录链接或 Windows 重解析跳转被拒绝，即使目标也在配置根目录内。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void rejectsDirectoryLinksIncludingWindowsJunctions() throws Exception {
        Path original = pdf("real/one.pdf", Color.RED);
        Path link = temporary.resolve("linked");
        if (System.getProperty("os.name").startsWith("Windows")) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                    link.toString(), original.getParent().toString()).redirectErrorStream(true).start();
            Assumptions.assumeTrue(process.waitFor() == 0, "系统不支持测试目录重解析链接");
        } else {
            try {
                Files.createSymbolicLink(link, original.getParent());
            } catch (UnsupportedOperationException | IOException e) {
                Assumptions.assumeTrue(false, "系统不支持测试符号链接");
            }
        }
        try {
            assertStatus(400, () -> service(properties(), temporary).generate(json("/pdf/file/linked/one.pdf")));
        } finally {
            Files.deleteIfExists(link);
        }
    }

    /**
     * 验证原始数组超限优先于文件 I/O，重复路径仍计入原始项数。
     *
     * @throws Exception JSON 编码失败时抛出
     */
    @Test
    void countsDuplicateRawEntriesBeforeDeduplication() throws Exception {
        PdfToWordProperties properties = properties();
        properties.setMaxFiles(1);
        EvidenceWordService service = service(properties, temporary.resolve("missing-root"));
        assertStatus(413, () -> service.generate(json("/pdf/file/missing.pdf", "/pdf/file/missing.pdf")));
    }

    /**
     * 验证转换失败的请求文件被删除，随后同实例仍可成功生成。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void removesFailedOutputAndReleasesPermit() throws Exception {
        Files.write(temporary.resolve("bad.pdf"), "not pdf".getBytes("UTF-8"));
        pdf("good.pdf", Color.GREEN);
        AtomicReference<File> created = new AtomicReference<>();
        EvidenceWordService service = new EvidenceWordService(temporary.toString(), properties()) {
            /** {@inheritDoc} */
            @Override
            protected File createTemporaryWord() throws IOException {
                File file = File.createTempFile("evidence-test-", ".docx", temporary.toFile());
                created.set(file);
                return file;
            }
        };
        assertStatus(422, () -> service.generate(json("/pdf/file/bad.pdf")));
        assertNotNull(created.get());
        assertFalse(created.get().exists());
        File good = service.generate(json("/pdf/file/good.pdf"));
        assertTrue(good.length() > 0);
        Files.delete(good.toPath());
    }

    /**
     * 验证输出故障回收请求文件且不会影响输入 PDF。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void outputFailureClosesResourcesAndDeletesOnlyItsOwnTemporaryFile() throws Exception {
        Path input = pdf("good.pdf", Color.RED);
        byte[] before = Files.readAllBytes(input);
        Path invalidOutput = Files.createDirectory(temporary.resolve("invalid-output.docx"));
        EvidenceWordService service = new EvidenceWordService(temporary.toString(), properties()) {
            /** {@inheritDoc} */
            @Override
            protected File createTemporaryWord() {
                return invalidOutput.toFile();
            }
        };
        assertStatus(400, () -> service.generate(json("/pdf/file/good.pdf")));
        assertFalse(Files.exists(invalidOutput));
        assertArrayEquals(before, Files.readAllBytes(input));
        Files.delete(input);
    }

    /**
     * 验证实例许可占满时立即拒绝，正在转换的请求仍能正常完成。
     *
     * @throws Exception 测试并发或文件处理失败时抛出
     */
    @Test
    void rejectsBusyInstanceWithoutQueueAndReleasesPermitAfterSuccess() throws Exception {
        pdf("one.pdf", Color.RED);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        EvidenceWordService service = new EvidenceWordService(temporary.toString(), properties()) {
            /** {@inheritDoc} */
            @Override
            protected File createTemporaryWord() throws IOException {
                entered.countDown();
                try {
                    if (!resume.await(10, TimeUnit.SECONDS)) {
                        throw new IOException("测试等待超时");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
                return super.createTemporaryWord();
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<File> first = executor.submit(() -> service.generate(json("/pdf/file/one.pdf")));
        try {
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            PdfConversionException busy = assertStatus(503, () -> service.generate(json("/pdf/file/one.pdf")));
            assertEquals("busy", busy.getStage());
            resume.countDown();
            Files.delete(first.get(10, TimeUnit.SECONDS).toPath());
            Files.delete(service.generate(json("/pdf/file/one.pdf")).toPath());
        } finally {
            resume.countDown();
            executor.shutdownNow();
        }
    }

    /**
     * 验证专用双许可实例产生不同输出文件，不相互覆盖或混入材料。
     *
     * @throws Exception 测试并发或文件处理失败时抛出
     */
    @Test
    void concurrentRequestsHaveIndependentOutputs() throws Exception {
        pdf("red.pdf", Color.RED);
        pdf("blue.pdf", Color.BLUE);
        PdfToWordProperties properties = properties();
        properties.setMaxConcurrentExports(2);
        EvidenceWordService service = service(properties, temporary);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<File> red = executor.submit(() -> service.generate(json("/pdf/file/red.pdf")));
            Future<File> blue = executor.submit(() -> service.generate(json("/pdf/file/blue.pdf")));
            File first = red.get(10, TimeUnit.SECONDS);
            File second = blue.get(10, TimeUnit.SECONDS);
            try {
                assertNotEquals(first.toPath(), second.toPath());
                for (File file : Arrays.asList(first, second)) {
                    try (InputStream input = Files.newInputStream(file.toPath()); XWPFDocument word = new XWPFDocument(input)) {
                        byte[] png = word.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures()
                                .get(0).getPictureData().getData();
                        int expected = file.equals(first) ? Color.RED.getRGB() : Color.BLUE.getRGB();
                        assertEquals(expected, ImageIO.read(new ByteArrayInputStream(png)).getRGB(20, 20));
                    }
                }
            } finally {
                Files.delete(first.toPath());
                Files.delete(second.toPath());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 验证堆门槛与部署存储故障仅拒绝当前请求。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void classifiesHeapAndStorageFailuresWithoutBreakingServiceConstruction() throws Exception {
        pdf("one.pdf", Color.RED);
        PdfToWordProperties properties = properties();
        properties.setMinJvmMaxHeapMb(Long.MAX_VALUE / (1024 * 1024));
        EvidenceWordService tooSmall = service(properties, temporary);
        assertStatus(503, () -> tooSmall.generate(json("/pdf/file/one.pdf")));
        EvidenceWordService missingRoot = service(properties(), temporary.resolve("missing"));
        assertStatus(500, () -> missingRoot.generate(json("/pdf/file/one.pdf")));
        String previous = System.getProperty("java.io.tmpdir");
        EvidenceWordService storage = service(properties(), temporary);
        try {
            System.setProperty("java.io.tmpdir", temporary.resolve("missing-tmp").toString());
            assertStatus(500, () -> storage.generate(json("/pdf/file/one.pdf")));
        } finally {
            System.setProperty("java.io.tmpdir", previous);
        }
        Files.delete(storage.generate(json("/pdf/file/one.pdf")).toPath());
    }

    /**
     * 验证方案规定的 1GiB 最大堆配置不会因 Java 8 GC 的报告差异而被误拒绝。
     *
     * @throws Exception 测试文件处理失败时抛出
     */
    @Test
    void acceptsDefaultHeapGateWhenConfiguredHeapMeetsOneGiB() throws Exception {
        HotSpotDiagnosticMXBean diagnostic = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        Assumptions.assumeTrue(diagnostic != null);
        Assumptions.assumeTrue(Long.parseLong(diagnostic.getVMOption("MaxHeapSize").getValue()) >= 1024L * 1024 * 1024);
        pdf("one.pdf", Color.RED);
        EvidenceWordService service = new EvidenceWordService(temporary.toString(), new PdfToWordProperties());
        Files.delete(service.generate(json("/pdf/file/one.pdf")).toPath());
    }

    /**
     * 创建使用测试堆门槛的参数；生产默认配置不变。
     *
     * @return 测试参数
     */
    private PdfToWordProperties properties() {
        PdfToWordProperties properties = new PdfToWordProperties();
        properties.setMinJvmMaxHeapMb(1);
        return properties;
    }

    /**
     * 创建独立服务实例。
     *
     * @param properties 测试参数
     * @param root 测试上传根目录
     * @return 生成服务
     */
    private EvidenceWordService service(PdfToWordProperties properties, Path root) {
        return new EvidenceWordService(root.toString(), properties);
    }

    /**
     * 编码材料访问路径数组。
     *
     * @param paths 材料访问路径
     * @return JSON 字符串
     * @throws Exception JSON 编码失败时抛出
     */
    private String json(String... paths) throws Exception {
        return new ObjectMapper().writeValueAsString(paths);
    }

    /**
     * 生成带颜色标记的真实 PDF。
     *
     * @param relative 相对文件名
     * @param colors 每页颜色
     * @return PDF 文件路径
     * @throws Exception 测试文件处理失败时抛出
     */
    private Path pdf(String relative, Color... colors) throws Exception {
        Path file = temporary.resolve(relative);
        Files.createDirectories(file.getParent());
        try (PDDocument pdf = new PDDocument()) {
            for (Color color : colors) {
                PDPage page = new PDPage(new PDRectangle(80, 80));
                pdf.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                    content.setNonStrokingColor(color);
                    content.addRect(0, 0, 80, 80);
                    content.fill();
                }
            }
            pdf.save(file.toFile());
        }
        return file;
    }

    /**
     * 验证公开状态与信息不会泄漏临时目录。
     *
     * @param expected 预期状态
     * @param action 被测动作
     * @return 转换异常
     */
    private PdfConversionException assertStatus(int expected, Executable action) {
        PdfConversionException error = assertThrows(PdfConversionException.class, action);
        assertEquals(expected, error.getStatus());
        assertFalse(error.getMessage().contains(temporary.toString()));
        return error;
    }
}
