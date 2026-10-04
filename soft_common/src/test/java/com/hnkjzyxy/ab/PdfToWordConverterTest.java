package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.config.PdfToWordProperties;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import com.hnkjzyxy.ab.utils.PdfToWordConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用真实 PDF 验证页面完整性、版式、文件保护及资源拒绝规则。
 */
class PdfToWordConverterTest {
    /** 每个用例独立的测试目录 */
    @TempDir
    Path temporary;
    /** 图片尺寸节点的命名空间 */
    private static final String DRAWING_NS = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing";
    /** Word 页面属性命名空间 */
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /**
     * 验证跨文件多页合并、列表顺序、空白页和去重，不以 media 文件数代替页面引用数。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void preservesEveryPageInListOrderIncludingBlankPagesAndDeduplicates() throws Exception {
        Path a = pdf("a.pdf", new PDRectangle(200, 300), Color.RED, null, Color.BLUE);
        Path b = pdf("b.pdf", new PDRectangle(300, 200), Color.GREEN, Color.YELLOW);
        Path output = temporary.resolve("all.docx");
        PdfToWordConverter.convertPdfFilesToWord(Arrays.asList(b, a, b), output, new PdfToWordProperties());
        try (InputStream input = Files.newInputStream(output); XWPFDocument word = new XWPFDocument(input)) {
            assertEquals(5, word.getParagraphs().size());
            Color[] expected = {Color.GREEN, Color.YELLOW, Color.RED, Color.WHITE, Color.BLUE};
            for (int index = 0; index < expected.length; index++) {
                XWPFParagraph paragraph = word.getParagraphs().get(index);
                assertEquals(index > 0, paragraph.isPageBreak());
                assertEquals(0, paragraph.getSpacingBefore());
                assertEquals(0, paragraph.getSpacingAfter());
                assertEquals(1, paragraph.getRuns().size());
                assertEquals(1, paragraph.getRuns().get(0).getEmbeddedPictures().size());
                assertEquals(expected[index].getRGB(), image(paragraph).getRGB(20, 20));
            }
            assertFalse(word.getParagraphs().get(4).getText().contains("\n"));
        }
    }

    /**
     * 验证实际图片比例、只缩小规则及写入 Word 的纸张和页边距。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void keepsLandscapeSquareAndSmallPageRatiosWithinConfiguredPage() throws Exception {
        Path landscape = pdf("landscape.pdf", new PDRectangle(842, 595), Color.RED);
        Path square = pdf("square.pdf", new PDRectangle(600, 600), Color.GREEN);
        Path small = pdf("small.pdf", new PDRectangle(100, 120), Color.BLUE);
        PdfToWordProperties properties = new PdfToWordProperties();
        Path output = temporary.resolve("ratio.docx");
        PdfToWordConverter.convertPdfFilesToWord(Arrays.asList(landscape, square, small), output, properties);
        try (InputStream input = Files.newInputStream(output); XWPFDocument word = new XWPFDocument(input)) {
            NodeList sizes = ((Element) word.getDocument().getDomNode().getFirstChild())
                    .getElementsByTagNameNS(WORD_NS, "pgSz");
            assertEquals(1, sizes.getLength());
            Element size = (Element) sizes.item(0);
            assertEquals("11906", size.getAttributeNS(WORD_NS, "w"));
            assertEquals("16838", size.getAttributeNS(WORD_NS, "h"));
            Element margin = (Element) ((Element) word.getDocument().getDomNode().getFirstChild())
                    .getElementsByTagNameNS(WORD_NS, "pgMar").item(0);
            for (String edge : new String[]{"top", "bottom", "left", "right"}) {
                assertEquals("720", margin.getAttributeNS(WORD_NS, edge));
            }
            for (XWPFParagraph paragraph : word.getParagraphs()) {
                BufferedImage rendered = image(paragraph);
                Element extent = (Element) ((Element) paragraph.getCTP().getDomNode())
                        .getElementsByTagNameNS(DRAWING_NS, "extent").item(0);
                long width = Long.parseLong(extent.getAttribute("cx"));
                long height = Long.parseLong(extent.getAttribute("cy"));
                double ratio = width / (double) height;
                assertEquals(rendered.getWidth() / (double) rendered.getHeight(), ratio, ratio * 0.001);
                assertTrue(width <= Units.toEMU(523.3));
                assertTrue(height <= Units.toEMU(751.9));
                assertTrue(width <= Units.toEMU(rendered.getWidth() * 72.0 / 150));
                assertTrue(height <= Units.toEMU(rendered.getHeight() * 72.0 / 150));
            }
        }
    }

    /**
     * 验证旋转及裁剪框使用渲染后的真实宽高。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void respectsRotatedCropBox() throws Exception {
        Path file = temporary.resolve("rotated.pdf");
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(600, 800));
            page.setCropBox(new PDRectangle(200, 300));
            page.setRotation(90);
            pdf.addPage(page);
            pdf.save(file.toFile());
        }
        Path output = temporary.resolve("rotated.docx");
        PdfToWordProperties properties = new PdfToWordProperties();
        properties.setRenderDpi(72);
        PdfToWordConverter.convertPdfFilesToWord(Collections.singletonList(file), output, properties);
        try (InputStream input = Files.newInputStream(output); XWPFDocument word = new XWPFDocument(input)) {
            BufferedImage rendered = image(word.getParagraphs().get(0));
            assertEquals(300, rendered.getWidth());
            assertEquals(200, rendered.getHeight());
        }
    }

    /**
     * 验证目录筛选、大小写后缀、字典序以及既有 PNG 完全不变。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void filtersAndSortsDirectoryWithoutTouchingOriginalFiles() throws Exception {
        Path first = pdf("10.PDF", new PDRectangle(80, 80), Color.RED);
        Path second = pdf("2.pdf", new PDRectangle(80, 80), Color.BLUE);
        Path png = temporary.resolve("2.png");
        Files.write(png, new byte[]{9, 8, 7});
        Files.write(temporary.resolve("unrelated.docx"), new byte[]{1});
        Files.createDirectory(temporary.resolve("folder.pdf"));
        byte[] beforeFirst = Files.readAllBytes(first);
        byte[] beforeSecond = Files.readAllBytes(second);
        Path output = temporary.resolve("result.docx");
        for (int attempt = 0; attempt < 2; attempt++) {
            PdfToWordConverter.pdfFilesToWordFile(temporary.toString(), output.toString());
            try (InputStream input = Files.newInputStream(output); XWPFDocument word = new XWPFDocument(input)) {
                assertEquals(2, word.getParagraphs().size());
                assertEquals(Color.RED.getRGB(), image(word.getParagraphs().get(0)).getRGB(20, 20));
                assertEquals(Color.BLUE.getRGB(), image(word.getParagraphs().get(1)).getRGB(20, 20));
            }
        }
        assertArrayEquals(beforeFirst, Files.readAllBytes(first));
        assertArrayEquals(beforeSecond, Files.readAllBytes(second));
        assertArrayEquals(new byte[]{9, 8, 7}, Files.readAllBytes(png));
        try (Stream<Path> entries = Files.list(temporary)) {
            assertEquals(6, entries.count());
        }
    }

    /**
     * 验证空列表、不存在目录、非 PDF、输出冲突均明确拒绝。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void rejectsInvalidInputsAndOutputAliasesWithoutOverwritingPdf() throws Exception {
        PdfToWordProperties properties = new PdfToWordProperties();
        Path output = temporary.resolve("output.docx");
        assertStatus(400, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.emptyList(), output, properties));
        assertStatus(400, () -> PdfToWordConverter.pdfFilesToWordFile(temporary.resolve("missing").toString(), output.toString()));
        assertStatus(400, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(temporary.resolve("file.txt")), output, properties));
        assertStatus(404, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(temporary.resolve("missing.pdf")), output, properties));
        Path file = pdf("input.pdf", new PDRectangle(50, 50), Color.RED);
        byte[] before = Files.readAllBytes(file);
        assertStatus(400, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.singletonList(file), file, properties));
        assertArrayEquals(before, Files.readAllBytes(file));
        assertFalse(Files.exists(output));
    }

    /**
     * 验证加密文档、零页及伪 PDF 分类，未识别的解析 I/O 保留服务器错误。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void classifiesEncryptedZeroPageFakeAndUnclassifiedBrokenPdfs() throws Exception {
        Path empty = pdf("empty.pdf", new PDRectangle(50, 50));
        Path fake = temporary.resolve("fake.pdf");
        Files.write(fake, "not pdf".getBytes("UTF-8"));
        Path broken = temporary.resolve("broken.pdf");
        Files.write(broken, "%PDF-1.7\nbroken".getBytes("UTF-8"));
        for (String password : new String[]{"", "password"}) {
            Path encrypted = temporary.resolve("encrypted" + password + ".pdf");
            try (PDDocument pdf = new PDDocument()) {
                pdf.addPage(new PDPage());
                pdf.protect(new StandardProtectionPolicy("owner", password, new AccessPermission()));
                pdf.save(encrypted.toFile());
            }
            assertStatus(422, () -> PdfToWordConverter.convertPdfFilesToWord(
                    Collections.singletonList(encrypted), temporary.resolve("encrypted.docx"), new PdfToWordProperties()));
        }
        assertStatus(422, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(empty), temporary.resolve("empty.docx"), new PdfToWordProperties()));
        assertStatus(422, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(fake), temporary.resolve("fake.docx"), new PdfToWordProperties()));
        assertStatus(500, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(broken), temporary.resolve("broken.docx"), new PdfToWordProperties()));
    }

    /**
     * 验证单份及累计页数、原始项数与像素预算在输出前拒绝。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void rejectsPageFileCountAndPixelBudgetsBeforeOutput() throws Exception {
        Path a = pdf("a.pdf", new PDRectangle(50, 50), Color.RED, Color.GREEN);
        Path b = pdf("b.pdf", new PDRectangle(50, 50), Color.BLUE);
        Path huge = pdf("huge.pdf", new PDRectangle(20000, 20000), Color.WHITE);
        Path output = temporary.resolve("limit.docx");
        PdfToWordProperties properties = new PdfToWordProperties();
        properties.setMaxPagesPerPdf(2);
        properties.setMaxTotalPages(2);
        assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(Arrays.asList(a, b), output, properties));
        properties.setMaxPagesPerPdf(1);
        assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.singletonList(a), output, properties));
        properties.setMaxFiles(1);
        assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(Arrays.asList(b, b), output, properties));
        assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.singletonList(huge), output, properties));
        assertFalse(Files.exists(output));
    }

    /**
     * 验证图片编码和 DOCX 输出在写入过程中执行累计上限。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void boundsImageEncodingAndDocxWriting() throws Exception {
        Path noise = noisyPdf();
        PdfToWordProperties properties = new PdfToWordProperties();
        properties.setRenderDpi(72);
        properties.setMaxTotalImageSizeMb(1);
        PdfConversionException imageFailure = assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(noise), temporary.resolve("image.docx"), properties));
        assertEquals("image", imageFailure.getStage());
        assertEquals(Integer.valueOf(1), imageFailure.getPageNumber());
        assertFalse(Files.exists(temporary.resolve("image.docx")));
        properties.setMaxTotalImageSizeMb(64);
        properties.setMaxOutputSizeMb(1);
        PdfConversionException outputFailure = assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(
                Collections.singletonList(noise), temporary.resolve("output.docx"), properties));
        assertEquals("output", outputFailure.getStage());
        assertTrue(Files.size(temporary.resolve("output.docx")) <= 1024 * 1024);
        Files.delete(temporary.resolve("output.docx"));
    }

    /**
     * 验证字节阈值允许等于，超过则拒绝；失败后输入句柄已经关闭。
     *
     * @throws Exception 测试文件读写失败时抛出
     */
    @Test
    void acceptsExactInputSizeLimitAndRejectsOneByteOver() throws Exception {
        Path file = pdf("size.pdf", new PDRectangle(50, 50), Color.RED);
        byte[] padded = Arrays.copyOf(Files.readAllBytes(file), 1024 * 1024);
        Files.write(file, padded);
        PdfToWordProperties properties = new PdfToWordProperties();
        properties.setMaxFileSizeMb(1);
        properties.setMaxTotalInputSizeMb(1);
        Path output = temporary.resolve("size.docx");
        PdfToWordConverter.convertPdfFilesToWord(Collections.singletonList(file), output, properties);
        Files.write(file, Arrays.copyOf(padded, padded.length + 1));
        assertStatus(413, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.singletonList(file), output, properties));
        Files.delete(file);
    }

    /**
     * 验证预算超时及中断不输出残缺材料，也不清除线程的中断标记。
     */
    @Test
    void rejectsExpiredBudgetAndPreservesInterruptFlag() {
        PdfToWordProperties properties = new PdfToWordProperties();
        assertStatus(503, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.emptyList(),
                temporary.resolve("expired.docx"), properties, System.nanoTime() - TimeUnit.SECONDS.toNanos(61)));
        Thread.currentThread().interrupt();
        try {
            assertStatus(503, () -> PdfToWordConverter.convertPdfFilesToWord(Collections.emptyList(),
                    temporary.resolve("interrupted.docx"), properties));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * 验证配置约束与参数关系校验，避免非法版式启动后才失败。
     */
    @Test
    void validatesPropertiesIncludingNonFiniteAndOverflowValues() {
        PdfToWordProperties properties = new PdfToWordProperties();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(properties).isEmpty());
            properties.setRenderDpi(300);
            assertFalse(factory.getValidator().validate(properties).isEmpty());
            assertThrows(IllegalArgumentException.class, properties::validate);
            properties.setRenderDpi(150);
            properties.setPageWidthPt(Double.NaN);
            assertFalse(factory.getValidator().validate(properties).isEmpty());
            properties.setPageWidthPt(595.3);
            properties.setMaxPdfScratchSizeMb(Long.MAX_VALUE);
            assertFalse(factory.getValidator().validate(properties).isEmpty());
            properties.setMaxPdfScratchSizeMb(256);
            properties.setMaxPagesPerPdf(51);
            assertFalse(factory.getValidator().validate(properties).isEmpty());
        }
    }

    /**
     * 生成每页具有可识别颜色的 PDF，null 颜色代表空白页。
     *
     * @param name 文件名
     * @param size 页面尺寸
     * @param colors 按页排列的颜色
     * @return PDF 路径
     * @throws Exception 测试文件写入失败时抛出
     */
    private Path pdf(String name, PDRectangle size, Color... colors) throws Exception {
        Path file = temporary.resolve(name);
        try (PDDocument pdf = new PDDocument()) {
            for (Color color : colors) {
                PDPage page = new PDPage(size);
                pdf.addPage(page);
                if (color != null) {
                    try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                        content.setNonStrokingColor(color);
                        content.addRect(0, 0, size.getWidth(), size.getHeight());
                        content.fill();
                    }
                }
            }
            pdf.save(file.toFile());
        }
        return file;
    }

    /**
     * 生成不可高度压缩的扫描页，用于真实编码预算验证。
     *
     * @return 测试 PDF
     * @throws Exception 测试文件写入失败时抛出
     */
    private Path noisyPdf() throws Exception {
        BufferedImage image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(7);
        for (int y = 0; y < 700; y++) {
            for (int x = 0; x < 700; x++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        Path file = temporary.resolve("noise.pdf");
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(700, 700));
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.drawImage(LosslessFactory.createFromImage(pdf, image), 0, 0, 700, 700);
            }
            pdf.save(file.toFile());
        } finally {
            image.flush();
        }
        return file;
    }

    /**
     * 从绘图引用读取 PNG，不受 POI 复用相同 media 文件的影响。
     *
     * @param paragraph 页面图片所在段落
     * @return 实际嵌入的图像
     * @throws Exception PNG 解码失败时抛出
     */
    private BufferedImage image(XWPFParagraph paragraph) throws Exception {
        XWPFPicture picture = paragraph.getRuns().get(0).getEmbeddedPictures().get(0);
        return ImageIO.read(new ByteArrayInputStream(picture.getPictureData().getData()));
    }

    /**
     * 检查专用异常状态。
     *
     * @param expected 预期状态
     * @param action 被测动作
     * @return 捕获的异常
     */
    private PdfConversionException assertStatus(int expected, org.junit.jupiter.api.function.Executable action) {
        PdfConversionException error = assertThrows(PdfConversionException.class, action);
        assertEquals(expected, error.getStatus());
        assertFalse(error.getMessage().contains(temporary.toString()));
        return error;
    }
}
