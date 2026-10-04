package com.hnkjzyxy.ab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.config.PdfToWordProperties;
import com.hnkjzyxy.ab.controller.ProjectController;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import com.hnkjzyxy.ab.export.EvidenceWordExporter;
import com.hnkjzyxy.ab.handler.GlobalExceptionHandler;
import com.hnkjzyxy.ab.service.support.EvidenceWordService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证 Word 导出完整 HTTP 链路、专用错误协议及配置绑定。
 */
class EvidenceWordHttpTest {
    /** 测试上传及输出目录 */
    @TempDir
    Path temporary;

    /**
     * 验证各专用错误返回相同 HTTP 和业务码，且没有附件响应头。
     *
     * @param code 预期错误状态
     * @throws Exception HTTP 测试失败时抛出
     */
    @ParameterizedTest
    @ValueSource(ints = {400, 404, 413, 422, 500, 503})
    void specializedErrorsHaveMatchingHttpAndJsonCodes(int code) throws Exception {
        EvidenceWordService service = mock(EvidenceWordService.class);
        when(service.generate("evidence")).thenThrow(new PdfConversionException(code, "test", "安全公开提示"));
        mvc(service).perform(post("/evidence/exportToWord").contentType(MediaType.APPLICATION_JSON)
                .content(payload("evidence")))
                .andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.msg").value("安全公开提示"))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().doesNotExist("Retry-After"));
    }

    /**
     * 验证真实服务对空列表、缺失、格式错误及页数超限的响应。
     *
     * @throws Exception HTTP 测试或文件处理失败时抛出
     */
    @Test
    void realInputFailuresReturnJsonBeforeDownloadBegins() throws Exception {
        PdfToWordProperties properties = properties();
        properties.setMaxPagesPerPdf(1);
        properties.setMaxTotalPages(1);
        Files.write(temporary.resolve("fake.pdf"), new byte[]{1, 2, 3});
        pdf("two.pdf", 2);
        MockMvc mvc = mvc(new EvidenceWordService(temporary.toString(), properties));
        assertError(mvc, "[]", 400);
        assertError(mvc, "[1]", 400);
        assertError(mvc, "[\"/pdf/file/../one.pdf\"]", 400);
        assertError(mvc, "[\"/pdf/file/missing.pdf\"]", 404);
        assertError(mvc, "[\"/pdf/file/fake.pdf\"]", 422);
        assertError(mvc, "[\"/pdf/file/two.pdf\"]", 413);
        assertError(mvc(new EvidenceWordService(temporary.resolve("absent").toString(), properties)),
                "[\"/pdf/file/one.pdf\"]", 500);
    }

    /**
     * 验证许可繁忙返回 Retry-After，释放许可后生成仍成功。
     *
     * @throws Exception HTTP 测试或文件处理失败时抛出
     */
    @Test
    void realBusyResponseIncludesRetryAfter() throws Exception {
        pdf("one.pdf", 1);
        EvidenceWordService service = new EvidenceWordService(temporary.toString(), properties());
        Semaphore permits = (Semaphore) ReflectionTestUtils.getField(service, "exportPermits");
        assertNotNull(permits);
        assertTrue(permits.tryAcquire());
        try {
            mvc(service).perform(post("/evidence/exportToWord").contentType(MediaType.APPLICATION_JSON)
                    .content(payload("[\"/pdf/file/one.pdf\"]")))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value(503))
                    .andExpect(jsonPath("$.msg").value("Word 导出繁忙，请稍后重试"))
                    .andExpect(header().string("Retry-After", "5"))
                    .andExpect(header().doesNotExist("Content-Disposition"));
        } finally {
            permits.release();
        }
        File output = service.generate("[\"/pdf/file/one.pdf\"]");
        Files.delete(output.toPath());
    }

    /**
     * 验证真实转换生成五页绘图引用，下载后请求文件已回收且下载契约不变。
     *
     * @throws Exception HTTP 测试或文件处理失败时抛出
     */
    @Test
    void realSuccessfulDownloadContainsEverySelectedPageAndCleansTemporaryDocx() throws Exception {
        pdf("a.pdf", 3);
        pdf("b.pdf", 2);
        AtomicReference<File> generated = new AtomicReference<>();
        EvidenceWordService service = new EvidenceWordService(temporary.toString(), properties()) {
            /** {@inheritDoc} */
            @Override
            protected File createTemporaryWord() throws IOException {
                File file = File.createTempFile("tempFile", ".docx", temporary.toFile());
                generated.set(file);
                return file;
            }
        };
        MvcResult result = mvc(service).perform(post("/evidence/exportToWord")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("[\"/pdf/file/a.pdf\",\"/pdf/file/b.pdf\"]")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/x-download;charset=utf8"))
                .andReturn();
        assertEquals("attachment;filename=" + generated.get().getName(),
                result.getResponse().getHeader("Content-Disposition"));
        assertFalse(generated.get().exists());
        try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertEquals(5, word.getParagraphs().size());
            assertEquals(5, word.getParagraphs().stream()
                    .mapToInt(paragraph -> paragraph.getRuns().stream()
                            .mapToInt(run -> run.getEmbeddedPictures().size()).sum()).sum());
        }
    }

    /**
     * 验证 ConfigurationProperties 注册与 Bean Validation 在启动阶段生效。
     */
    @Test
    void bindsConfigurationAndRejectsInvalidStartupValues() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("evidence.word.render-dpi=200", "evidence.word.max-files=4")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(200, context.getBean(PdfToWordProperties.class).getRenderDpi());
                    assertEquals(4, context.getBean(PdfToWordProperties.class).getMaxFiles());
                });
        for (String invalid : new String[]{"evidence.word.render-dpi=300", "evidence.word.max-files=0",
                "evidence.word.margin-pt=400", "evidence.word.max-pages-per-pdf=51"}) {
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class)
                    .withPropertyValues(invalid)
                    .run(context -> assertNotNull(context.getStartupFailure()));
        }
    }

    /**
     * 构造与现有控制器、Exporter 和异常处理器相同的 MVC 链路。
     *
     * @param service 被测生成服务
     * @return MVC 测试客户端
     */
    private MockMvc mvc(EvidenceWordService service) {
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "evidenceWordExporter", new EvidenceWordExporter(service));
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 编码原接口的 evidence 字符串字段。
     *
     * @param evidence 材料路径 JSON 字符串
     * @return 请求体 JSON
     * @throws Exception 请求编码失败时抛出
     */
    private String payload(String evidence) throws Exception {
        return new ObjectMapper().writeValueAsString(Collections.singletonMap("evidence", evidence));
    }

    /**
     * 验证真实请求失败没有开始附件下载。
     *
     * @param mvc MVC 测试客户端
     * @param evidence 材料列表
     * @param code 预期状态
     * @throws Exception HTTP 验证失败时抛出
     */
    private void assertError(MockMvc mvc, String evidence, int code) throws Exception {
        MvcResult result = mvc.perform(post("/evidence/exportToWord")
                .contentType(MediaType.APPLICATION_JSON).content(payload(evidence)))
                .andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains(temporary.toString()));
    }

    /**
     * 使用小页面创建真实 PDF，空白页仍必须保留。
     *
     * @param name 文件名
     * @param pages 页数
     * @throws Exception 文件生成失败时抛出
     */
    private void pdf(String name, int pages) throws Exception {
        try (PDDocument pdf = new PDDocument()) {
            for (int index = 0; index < pages; index++) {
                pdf.addPage(new PDPage(new PDRectangle(80, 80)));
            }
            pdf.save(temporary.resolve(name).toFile());
        }
    }

    /**
     * 测试实例使用独立堆门槛，生产配置不变。
     *
     * @return 测试参数
     */
    private PdfToWordProperties properties() {
        PdfToWordProperties properties = new PdfToWordProperties();
        properties.setMinJvmMaxHeapMb(1);
        return properties;
    }

    /** 仅注册 Word 参数的测试配置，不连接数据库或外部服务 */
    @Configuration
    @EnableConfigurationProperties(PdfToWordProperties.class)
    static class PropertiesConfiguration {
    }
}
