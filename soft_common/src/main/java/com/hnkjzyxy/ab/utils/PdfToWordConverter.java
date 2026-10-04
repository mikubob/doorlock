package com.hnkjzyxy.ab.utils;

import com.hnkjzyxy.ab.config.PdfToWordProperties;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.slf4j.MDC;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.xml.namespace.QName;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 将 PDF 的全部页面渲染为图片并等比插入 Word，不提取可编辑文字。
 */
@Slf4j
public class PdfToWordConverter {
    /** WordprocessingML 命名空间 */
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    /** MiB 对应的字节数 */
    private static final long MIB = 1024L * 1024L;

    /**
     * 将目录本层的普通 PDF 文件按文件名字典序汇总到一个 Word。
     *
     * @param path PDF 所在目录，不递归
     * @param wordFilePath Word 输出路径
     * @throws Exception 目录、输入或转换失败时抛出
     */
    public static void pdfFilesToWordFile(String path, String wordFilePath) throws Exception {
        if (path == null || path.trim().isEmpty() || wordFilePath == null || wordFilePath.trim().isEmpty()) {
            throw new PdfConversionException(400, "input", "PDF 目录或 Word 输出路径不合法");
        }
        Path directory;
        Path output;
        try {
            directory = Paths.get(path);
            output = Paths.get(wordFilePath);
        } catch (InvalidPathException e) {
            throw failure(400, "input", null, null, "PDF 目录或 Word 输出路径不合法", e);
        }
        if (!Files.isDirectory(directory) || !Files.isReadable(directory)) {
            throw new PdfConversionException(400, "input", "PDF 目录不存在或不可读取");
        }
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)
                        && Files.isReadable(entry) && isPdf(entry)) {
                    files.add(entry);
                }
            }
        } catch (IOException | DirectoryIteratorException e) {
            throw failure(500, "input", null, null, "PDF 目录读取失败，请联系管理员", e);
        }
        files.sort((left, right) -> {
            String a = left.getFileName().toString();
            String b = right.getFileName().toString();
            int compared = a.compareToIgnoreCase(b);
            return compared == 0 ? a.compareTo(b) : compared;
        });
        convertPdfFilesToWord(files, output, new PdfToWordProperties());
    }

    /**
     * 按显式列表顺序转换，真实文件身份重复时保留首次出现位置。
     *
     * @param pdfFiles 选中的 PDF 文件
     * @param output Word 输出文件
     * @param properties 版式与资源限制
     * @throws PdfConversionException 输入、预算或转换失败时抛出
     */
    public static void convertPdfFilesToWord(List<Path> pdfFiles, Path output, PdfToWordProperties properties) {
        convertPdfFilesToWord(pdfFiles, output, properties, System.nanoTime());
    }

    /**
     * 使用调用方的请求起始时间，使路径校验也计入生成预算。
     *
     * @param pdfFiles 选中的 PDF 文件
     * @param output Word 输出文件
     * @param properties 版式与资源限制
     * @param startedAt 请求开始时的 System.nanoTime 值
     * @throws PdfConversionException 输入、预算或转换失败时抛出
     */
    public static void convertPdfFilesToWord(List<Path> pdfFiles, Path output,
                                            PdfToWordProperties properties, long startedAt) {
        properties.validate();
        checkBudget(startedAt, properties);
        List<Path> inputs = validateInputs(pdfFiles, properties, startedAt);
        Path target = validateOutput(output, inputs);
        List<Integer> pageCounts = preflight(inputs, properties, startedAt);
        long imageBytes = 0;
        long actualInputBytes = 0;
        int inserted = 0;
        try (XWPFDocument word = new XWPFDocument()) {
            configurePage(word, properties);
            for (int fileIndex = 0; fileIndex < inputs.size(); fileIndex++) {
                Path file = validatePdfInput(inputs.get(fileIndex));
                checkBudget(startedAt, properties);
                actualInputBytes = validateSize(file, actualInputBytes, properties);
                try (PDDocument pdf = loadPdf(file, properties)) {
                    int pages = validatePages(pdf, file, properties);
                    if (pages != pageCounts.get(fileIndex)) {
                        throw failure(500, "read", file, null, "佐证材料在生成期间发生变化，请重试", null);
                    }
                    PDFRenderer renderer = new PDFRenderer(pdf);
                    for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
                        checkBudget(startedAt, properties);
                        validatePixels(pdf.getPage(pageIndex), file, pageIndex + 1, properties);
                        imageBytes += appendPage(word, renderer, file, pageIndex, inserted > 0,
                                properties, properties.getMaxTotalImageSizeMb() * MIB - imageBytes, startedAt);
                        inserted++;
                        checkBudget(startedAt, properties);
                    }
                } catch (PdfConversionException e) {
                    throw e;
                } catch (IOException e) {
                    throw failure(500, "read", file, null, "佐证材料读取失败，请联系管理员", e);
                }
            }
            checkBudget(startedAt, properties);
            try (OutputStream bounded = new LimitedOutputStream(Files.newOutputStream(target),
                    properties.getMaxOutputSizeMb() * MIB, "output", startedAt, properties)) {
                word.write(bounded);
            }
            if (Files.size(target) > properties.getMaxOutputSizeMb() * MIB) {
                throw new PdfConversionException(413, "output", "Word 文件超过大小上限，请分批导出");
            }
        } catch (Exception e) {
            PdfConversionException classified = findConversionCause(e);
            if (classified != null) {
                throw classified;
            }
            throw failure(500, "output", null, null, "Word 文件生成失败，请联系管理员", e);
        }
        checkBudget(startedAt, properties);
        log.info("PDF 转 Word 完成：请求={}，文件数={}，总页数={}，图片字节={}，耗时={}ms",
                MDC.get("evidenceWordRequest"), inputs.size(), inserted, imageBytes,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    /**
     * 校验普通 PDF、可读性和所有路径组件，禁止符号链接及重解析跳转。
     *
     * @param file 本地 PDF 路径
     * @return 规范化的真实文件路径
     * @throws PdfConversionException 路径无效、材料缺失或读取失败时抛出
     */
    public static Path validatePdfInput(Path file) {
        if (file == null || !isPdf(file)) {
            throw new PdfConversionException(400, "input", "请选择 PDF 佐证材料");
        }
        Path absolute = file.toAbsolutePath().normalize();
        try {
            Path current = absolute.getRoot();
            for (Path part : absolute) {
                current = current.resolve(part);
                BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (attributes.isSymbolicLink() || attributes.isOther()
                        || !current.toRealPath().equals(current)) {
                    throw failure(400, "input", file, null, "佐证材料路径不合法", null);
                }
            }
            if (!Files.isRegularFile(absolute, LinkOption.NOFOLLOW_LINKS)) {
                throw failure(400, "input", file, null, "佐证材料必须是普通 PDF 文件", null);
            }
            if (!Files.isReadable(absolute)) {
                throw failure(500, "input", file, null, "佐证材料读取失败，请联系管理员", null);
            }
            return absolute.toRealPath();
        } catch (NoSuchFileException e) {
            throw failure(404, "input", file, null,
                    "佐证材料 " + file.getFileName() + " 不存在，请重新上传", e);
        } catch (IOException | SecurityException e) {
            throw failure(500, "input", file, null, "佐证材料读取失败，请联系管理员", e);
        }
    }

    /**
     * 在阶段边界检查超时及中断，不强制打断第三方渲染调用。
     *
     * @param startedAt 请求开始时的单调时钟值
     * @param properties 生成参数
     * @throws PdfConversionException 时间预算用尽或线程已中断时抛出
     */
    public static void checkBudget(long startedAt, PdfToWordProperties properties) {
        if (Thread.currentThread().isInterrupted()) {
            throw new PdfConversionException(503, "budget", "Word 生成已中断，请重试");
        }
        if (System.nanoTime() - startedAt > TimeUnit.SECONDS.toNanos(properties.getMaxGenerationSeconds())) {
            throw new PdfConversionException(503, "budget", "Word 生成超时，请拆分材料后重试");
        }
    }

    /**
     * 校验原始项数并按文件身份去重。
     *
     * @param files 原始输入
     * @param properties 生成参数
     * @param startedAt 请求开始时间
     * @return 去重后的真实路径列表
     */
    private static List<Path> validateInputs(List<Path> files, PdfToWordProperties properties, long startedAt) {
        if (files == null || files.isEmpty()) {
            throw new PdfConversionException(400, "input", "请选择至少一份 PDF 佐证材料");
        }
        if (files.size() > properties.getMaxFiles()) {
            throw new PdfConversionException(413, "input", "佐证材料超过 " + properties.getMaxFiles() + " 项上限，请分批导出");
        }
        List<Path> unique = new ArrayList<>();
        for (Path candidate : files) {
            checkBudget(startedAt, properties);
            Path real = validatePdfInput(candidate);
            boolean duplicate = false;
            for (Path previous : unique) {
                try {
                    if (Files.isSameFile(previous, real)) {
                        duplicate = true;
                        break;
                    }
                } catch (IOException e) {
                    throw failure(500, "input", candidate, null, "佐证材料读取失败，请联系管理员", e);
                }
            }
            if (!duplicate) {
                unique.add(real);
            }
        }
        return unique;
    }

    /**
     * 校验输出父目录与输入文件冲突，避免覆盖 PDF。
     *
     * @param output 输出路径
     * @param inputs 输入真实路径
     * @return 输出绝对路径
     */
    private static Path validateOutput(Path output, List<Path> inputs) {
        if (output == null) {
            throw new PdfConversionException(400, "output", "Word 输出路径不合法");
        }
        Path target = output.toAbsolutePath().normalize();
        if (!Files.isDirectory(target.getParent()) || !Files.isWritable(target.getParent())) {
            throw new PdfConversionException(500, "output", "Word 输出目录不可写，请联系管理员");
        }
        try {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || !Files.isWritable(target)) {
                    throw new PdfConversionException(400, "output", "Word 输出路径不合法");
                }
                for (Path input : inputs) {
                    if (Files.isSameFile(target, input)) {
                        throw new PdfConversionException(400, "output", "Word 输出不能覆盖输入 PDF");
                    }
                }
            }
            return target;
        } catch (IOException e) {
            throw failure(500, "output", null, null, "Word 输出路径检查失败，请联系管理员", e);
        }
    }

    /**
     * 在分配页面位图前校验输入大小、页数与全部页面像素预算。
     *
     * @param inputs 去重后的材料
     * @param properties 生成参数
     * @param startedAt 请求开始时间
     * @return 每份材料的预检页数
     */
    private static List<Integer> preflight(List<Path> inputs, PdfToWordProperties properties, long startedAt) {
        long inputBytes = 0;
        long totalPages = 0;
        List<Integer> counts = new ArrayList<>();
        for (Path file : inputs) {
            checkBudget(startedAt, properties);
            try {
                inputBytes = validateSize(file, inputBytes, properties);
                try (PDDocument pdf = loadPdf(file, properties)) {
                    int pages = validatePages(pdf, file, properties);
                    totalPages += pages;
                    if (totalPages > properties.getMaxTotalPages()) {
                        throw failure(413, "preflight", file, null, "本次材料共 " + totalPages + " 页，超过 "
                                + properties.getMaxTotalPages() + " 页上限，请分批导出", null);
                    }
                    for (int index = 0; index < pages; index++) {
                        checkBudget(startedAt, properties);
                        validatePixels(pdf.getPage(index), file, index + 1, properties);
                    }
                    counts.add(pages);
                }
            } catch (IOException e) {
                throw failure(500, "preflight", file, null, "佐证材料预检失败，请联系管理员", e);
            }
            checkBudget(startedAt, properties);
        }
        return counts;
    }

    /**
     * 检查单文件及累计输入字节数，采用剩余预算避免加法溢出。
     *
     * @param file 当前材料
     * @param accumulated 已累计大小
     * @param properties 生成参数
     * @return 加入当前文件后的累计大小
     * @throws IOException 读取文件大小失败时抛出
     */
    private static long validateSize(Path file, long accumulated, PdfToWordProperties properties) throws IOException {
        long size = Files.size(file);
        if (size > properties.getMaxFileSizeMb() * MIB
                || size > properties.getMaxTotalInputSizeMb() * MIB - accumulated) {
            throw failure(413, "preflight", file, null, "佐证材料超过输入大小上限，请分批导出", null);
        }
        return accumulated + size;
    }

    /**
     * 检查格式标记，并使用有界临时存储打开 PDF；未知 I/O 故障不猜测为格式错误。
     *
     * @param file 输入 PDF
     * @param properties 生成参数
     * @return 由调用方关闭的 PDF 文档
     * @throws IOException 未分类的底层读取故障
     */
    private static PDDocument loadPdf(Path file, PdfToWordProperties properties) throws IOException {
        byte[] header = new byte[1024];
        int length = 0;
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while (length < header.length && (read = input.read(header, length, header.length - length)) > 0) {
                length += read;
            }
        }
        if (!new String(header, 0, length, StandardCharsets.ISO_8859_1).contains("%PDF-")) {
            throw failure(422, "parse", file, null, "佐证材料 " + file.getFileName() + " 不是有效 PDF", null);
        }
        try {
            return PDDocument.load(file.toFile(),
                    MemoryUsageSetting.setupTempFileOnly(properties.getMaxPdfScratchSizeMb() * MIB));
        } catch (InvalidPasswordException e) {
            throw failure(422, "parse", file, null,
                    "佐证材料 " + file.getFileName() + " 已加密，请提交未加密 PDF", e);
        }
    }

    /**
     * 检查加密、零页及单文档页数上限。
     *
     * @param pdf 已打开的文档
     * @param file 输入路径
     * @param properties 生成参数
     * @return 文档页数
     */
    private static int validatePages(PDDocument pdf, Path file, PdfToWordProperties properties) {
        if (pdf.isEncrypted()) {
            throw failure(422, "parse", file, null,
                    "佐证材料 " + file.getFileName() + " 已加密，请提交未加密 PDF", null);
        }
        int pages = pdf.getNumberOfPages();
        if (pages == 0) {
            throw failure(422, "parse", file, null, "佐证材料 " + file.getFileName() + " 没有页面", null);
        }
        if (pages > properties.getMaxPagesPerPdf()) {
            throw failure(413, "preflight", file, null, "佐证材料 " + file.getFileName() + " 共 " + pages
                    + " 页，超过 " + properties.getMaxPagesPerPdf() + " 页上限，请分批导出", null);
        }
        return pages;
    }

    /**
     * 按 PDFBox 2.0.24 的 CropBox 与 float DPI 尺度保守估算像素；旋转不改变总像素数。
     *
     * @param page PDF 页面
     * @param file 材料路径
     * @param number 从 1 开始的页码
     * @param properties 生成参数
     */
    private static void validatePixels(PDPage page, Path file, int number, PdfToWordProperties properties) {
        PDRectangle box = page.getCropBox();
        float width = box.getWidth();
        float height = box.getHeight();
        if (!Float.isFinite(width) || !Float.isFinite(height) || width <= 0 || height <= 0) {
            throw failure(422, "preflight", file, number, "佐证材料页面尺寸不合法", null);
        }
        float scale = properties.getRenderDpi() / 72f;
        double pixelsWide = Math.max(1, Math.ceil(width * scale));
        double pixelsHigh = Math.max(1, Math.ceil(height * scale));
        if (pixelsWide > Integer.MAX_VALUE || pixelsHigh > Integer.MAX_VALUE
                || pixelsWide * pixelsHigh > properties.getMaxPagePixels()
                || pixelsWide * pixelsHigh > Integer.MAX_VALUE) {
            throw failure(413, "preflight", file, number,
                    "佐证材料 " + file.getFileName() + " 第 " + number + " 页超过像素上限，请缩小页面后重试", null);
        }
    }

    /**
     * 逐页渲染、有限编码并嵌图；所有页面位图与流仅在本方法内存活。
     *
     * @param word 目标 Word
     * @param renderer 当前 PDF 渲染器
     * @param file 材料路径
     * @param index 从 0 开始的页码
     * @param pageBreak 是否设置段前分页
     * @param properties 生成参数
     * @param remainingBytes 剩余图片字节预算
     * @param startedAt 请求开始时间
     * @return 本页 PNG 编码大小
     */
    private static long appendPage(XWPFDocument word, PDFRenderer renderer, Path file, int index,
                                   boolean pageBreak, PdfToWordProperties properties,
                                   long remainingBytes, long startedAt) {
        BufferedImage image = null;
        String stage = "render";
        try {
            image = renderer.renderImageWithDPI(index, properties.getRenderDpi(), ImageType.RGB);
            checkBudget(startedAt, properties);
            stage = "encode";
            try (ByteArrayOutputStream png = new ByteArrayOutputStream();
                 LimitedOutputStream bounded = new LimitedOutputStream(png, remainingBytes,
                         "image", startedAt, properties);
                 LimitedImageOutputStream encoded = new LimitedImageOutputStream(bounded, remainingBytes,
                         startedAt, properties)) {
                if (!ImageIO.write(image, "PNG", encoded)) {
                    throw new IOException("PNG 编码器不可用");
                }
                encoded.flush();
                checkBudget(startedAt, properties);
                stage = "embed";
                XWPFParagraph paragraph = word.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                paragraph.setSpacingBefore(0);
                paragraph.setSpacingAfter(0);
                paragraph.setSpacingBetween(1.0);
                paragraph.setPageBreak(pageBreak);
                XWPFRun run = paragraph.createRun();
                run.setFontSize(1);
                double width = image.getWidth() * 72.0 / properties.getRenderDpi();
                double height = image.getHeight() * 72.0 / properties.getRenderDpi();
                double margin = Math.round(properties.getMarginPt() * 20) / 20.0;
                double availableWidth = Math.round(properties.getPageWidthPt() * 20) / 20.0 - 2 * margin;
                double availableHeight = Math.round(properties.getPageHeightPt() * 20) / 20.0 - 2 * margin
                        - properties.getParagraphReservePt();
                double scale = Math.min(1, Math.min(availableWidth / width, availableHeight / height));
                try (ByteArrayInputStream input = new ByteArrayInputStream(png.toByteArray())) {
                    run.addPicture(input, XWPFDocument.PICTURE_TYPE_PNG,
                            file.getFileName() + "-" + (index + 1) + ".png",
                            Units.toEMU(width * scale), Units.toEMU(height * scale));
                }
                return bounded.getCount();
            }
        } catch (Exception e) {
            PdfConversionException classified = findConversionCause(e);
            if (classified != null) {
                throw failure(classified.getStatus(), classified.getStage(), file, index + 1,
                        classified.getMessage(), classified);
            }
            throw failure(500, stage, file, index + 1, "佐证材料 " + file.getFileName()
                    + " 第 " + (index + 1) + " 页转换失败，请联系管理员", e);
        } finally {
            if (image != null) {
                image.flush();
            }
        }
    }

    /**
     * 同步写入纸张及页边距，使用 XMLBeans 游标兼容项目精简 schemas 包。
     *
     * @param word 目标 Word
     * @param properties 版式参数，pt 转 twip
     */
    private static void configurePage(XWPFDocument word, PdfToWordProperties properties) {
        XmlCursor cursor = word.getDocument().getBody().addNewSectPr().newCursor();
        try {
            cursor.toEndToken();
            cursor.beginElement(new QName(WORD_NS, "pgSz", "w"));
            cursor.insertAttributeWithValue(new QName(WORD_NS, "w", "w"),
                    Long.toString(Math.round(properties.getPageWidthPt() * 20)));
            cursor.insertAttributeWithValue(new QName(WORD_NS, "h", "w"),
                    Long.toString(Math.round(properties.getPageHeightPt() * 20)));
        } finally {
            cursor.dispose();
        }
        cursor = word.getDocument().getBody().getSectPr().newCursor();
        try {
            cursor.toEndToken();
            cursor.beginElement(new QName(WORD_NS, "pgMar", "w"));
            for (String edge : new String[]{"top", "bottom", "left", "right"}) {
                cursor.insertAttributeWithValue(new QName(WORD_NS, edge, "w"),
                        Long.toString(Math.round(properties.getMarginPt() * 20)));
            }
            for (String edge : new String[]{"header", "footer", "gutter"}) {
                cursor.insertAttributeWithValue(new QName(WORD_NS, edge, "w"), "0");
            }
        } finally {
            cursor.dispose();
        }
    }

    /**
     * 判断最后一个扩展名是否为 PDF。
     *
     * @param file 输入路径
     * @return 后缀为 PDF 时为 true
     */
    private static boolean isPdf(Path file) {
        return file.getFileName() != null
                && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    /**
     * 创建仅公开文件名的故障对象。
     *
     * @param status HTTP 状态码
     * @param stage 失败阶段
     * @param file 材料路径，可为空
     * @param page 页码，可为空
     * @param message 公开提示
     * @param cause 内部故障
     * @return 专用转换异常
     */
    private static PdfConversionException failure(int status, String stage, Path file, Integer page,
                                                   String message, Throwable cause) {
        return new PdfConversionException(status, stage, file == null ? null : file.getFileName().toString(),
                page, message, cause);
    }

    /**
     * 提取被 ImageIO 或 POI 包装的预算异常，保持原来的 HTTP 分类。
     *
     * @param cause 第三方异常
     * @return 已分类的异常；不存在时为 null
     */
    private static PdfConversionException findConversionCause(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof PdfConversionException) {
                return (PdfConversionException) current;
            }
        }
        return null;
    }

    /** 限制 ImageIO 内部缓存的写入位置，避免编码完成后才检查底层流上限 */
    private static class LimitedImageOutputStream extends MemoryCacheImageOutputStream {
        /** 当前页面剩余图片字节预算 */
        private final long limit;
        /** 请求开始时间 */
        private final long startedAt;
        /** 生成参数 */
        private final PdfToWordProperties properties;

        /**
         * 创建仅使用内存缓存且具有编码位置上限的流。
         *
         * @param output 有界底层流
         * @param limit 当前页面剩余字节预算
         * @param startedAt 请求开始时间
         * @param properties 生成参数
         */
        private LimitedImageOutputStream(OutputStream output, long limit, long startedAt,
                                         PdfToWordProperties properties) {
            super(output);
            this.limit = limit;
            this.startedAt = startedAt;
            this.properties = properties;
        }

        /**
         * 在 ImageIO 扩大缓存之前检查本次写入范围。
         *
         * @param length 本次写入长度
         * @throws IOException 流位置不可读取时抛出
         */
        private void reserve(int length) throws IOException {
            checkBudget(startedAt, properties);
            if (length > limit - getStreamPosition()) {
                throw new PdfConversionException(413, "image", "页面图片超过累计大小上限，请分批导出");
            }
        }

        /** {@inheritDoc} */
        @Override
        public void write(int value) throws IOException {
            reserve(1);
            super.write(value);
        }

        /** {@inheritDoc} */
        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            reserve(length);
            super.write(bytes, offset, length);
        }

        /**
         * 防止编码器通过 seek 扩展到剩余预算以外。
         *
         * @param position 目标位置
         * @throws IOException 底层流定位失败时抛出
         */
        @Override
        public void seek(long position) throws IOException {
            checkBudget(startedAt, properties);
            if (position > limit) {
                throw new PdfConversionException(413, "image", "页面图片超过累计大小上限，请分批导出");
            }
            super.seek(position);
        }
    }

    /** 有界输出流，在写入内存或文件前检查大小及生成预算 */
    private static class LimitedOutputStream extends OutputStream {
        /** 实际输出流，由本包装流关闭 */
        private final OutputStream delegate;
        /** 最大允许写入字节数 */
        private final long limit;
        /** 输出阶段 */
        private final String stage;
        /** 请求开始时间 */
        private final long startedAt;
        /** 生成参数 */
        private final PdfToWordProperties properties;
        /** 已写入字节数 */
        @Getter
        private long count;

        /**
         * 初始化有界流。
         *
         * @param delegate 底层流
         * @param limit 字节上限
         * @param stage 输出阶段
         * @param startedAt 请求开始时间
         * @param properties 生成参数
         */
        private LimitedOutputStream(OutputStream delegate, long limit, String stage,
                                    long startedAt, PdfToWordProperties properties) {
            this.delegate = delegate;
            this.limit = limit;
            this.stage = stage;
            this.startedAt = startedAt;
            this.properties = properties;
        }

        /**
         * 检查本次写入不会超出剩余预算。
         *
         * @param length 本次字节数
         */
        private void reserve(int length) {
            checkBudget(startedAt, properties);
            if (length > limit - count) {
                throw new PdfConversionException(413, stage, "image".equals(stage)
                        ? "页面图片超过累计大小上限，请分批导出" : "Word 文件超过大小上限，请分批导出");
            }
        }

        /** {@inheritDoc} */
        @Override
        public void write(int value) throws IOException {
            reserve(1);
            delegate.write(value);
            count++;
        }

        /** {@inheritDoc} */
        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            reserve(length);
            delegate.write(bytes, offset, length);
            count += length;
        }

        /** {@inheritDoc} */
        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
