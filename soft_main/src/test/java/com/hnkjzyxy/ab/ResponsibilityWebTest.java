package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.controller.ProjectController;
import com.hnkjzyxy.ab.controller.SamrtLockController;
import com.hnkjzyxy.ab.export.EvidenceWordExporter;
import com.hnkjzyxy.ab.exception.PdfConversionException;
import com.hnkjzyxy.ab.handler.GlobalExceptionHandler;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectResultSubmitParam;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.support.EvidenceWordService;
import com.hnkjzyxy.ab.service.support.SmartLockDiscoveryService;
import com.hnkjzyxy.ab.smartlock.client.SmartLockDiscoveryClient;
import com.hnkjzyxy.ab.vo.ResultVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.ServletOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 职责拆分 Web 回归测试，验证接口绑定、文件下载及设备发现入口
 */
class ResponsibilityWebTest {
    /**
     * 每个测试使用的临时文件目录
     */
    @TempDir
    Path temporary;

    /**
     * 验证提交及暂存请求绑定与返回对象兼容性
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void resultEndpointsBindNewRequestAndStillReturnVoFromStaging() throws Exception {
        ProjectService projects = mock(ProjectService.class);
        UserService users = mock(UserService.class);
        User user = new User();
        user.setUserId(7);
        when(users.getUserByName("1001")).thenReturn(user);
        ResultVo output = new ResultVo();
        output.setProjectId(3);
        output.setResults(Collections.emptyList());
        when(projects.getProjectStaging(user, 3)).thenReturn(output);
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "projectService", projects);
        ReflectionTestUtils.setField(controller, "userService", users);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("1001", "unused");
        String payload = "{\"projectId\":3,\"isFlag\":1,\"results\":[{\"uid\":99}]}";
        mvc.perform(post("/project/result").principal(auth).contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<ProjectResultSubmitParam> submitted = ArgumentCaptor.forClass(ProjectResultSubmitParam.class);
        verify(projects).resultProject(submitted.capture(), eq(user));
        assertEquals(3, submitted.getValue().getProjectId());
        mvc.perform(post("/project/Staging").principal(auth).contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<ProjectResultSubmitParam> staged = ArgumentCaptor.forClass(ProjectResultSubmitParam.class);
        verify(projects).projectStaging(staged.capture(), eq(user));
        assertEquals(7, staged.getValue().getResults().get(0).getUId());
        mvc.perform(post("/project/result").principal(auth).contentType("application/json")
                .content("{\"results\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(422));
        mvc.perform(get("/project/Staging/3").principal(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.projectId").value(3));
    }

    /**
     * 验证 Word 下载接口保留响应头及文件内容并清理临时文件
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void wordEndpointUsesExporterAndPreservesDownloadHeadersAndBytes() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        File file = temporary.resolve("evidence.docx").toFile();
        Files.write(file.toPath(), bytes);
        EvidenceWordService service = mock(EvidenceWordService.class);
        when(service.generate("[]")).thenReturn(file);
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "evidenceWordExporter", new EvidenceWordExporter(service));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/evidence/exportToWord").contentType("application/json")
                .content("{\"evidence\":\"[]\"}"))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(header().string("Content-Type", "application/x-download;charset=utf8"))
                .andExpect(header().string("Content-Disposition", "attachment;filename=evidence.docx"));
        verify(service).generate("[]");
        assertFalse(file.exists());
    }

    /**
     * 验证生成失败时没有设置附件头或访问下载输出流。
     */
    @Test
    void failedWordGenerationDoesNotBeginDownload() {
        EvidenceWordService service = mock(EvidenceWordService.class);
        PdfConversionException failure = new PdfConversionException(422, "parse", "佐证材料不是有效 PDF");
        when(service.generate("evidence")).thenThrow(failure);
        MockHttpServletResponse response = new MockHttpServletResponse() {
            /** {@inheritDoc} */
            @Override
            public ServletOutputStream getOutputStream() {
                fail("生成失败时不能开始下载");
                return null;
            }
        };
        assertSame(failure, assertThrows(PdfConversionException.class,
                () -> new EvidenceWordExporter(service).export("evidence", response)));
        assertNull(response.getHeader("Content-Disposition"));
        assertNull(response.getContentType());
        assertFalse(response.isCommitted());
    }

    /**
     * 验证下载响应失败时仍删除临时 Word 文件
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void failedWordDownloadStillDeletesTemporaryFile() throws Exception {
        File file = temporary.resolve("evidence.docx").toFile();
        Files.write(file.toPath(), new byte[]{1});
        EvidenceWordService service = mock(EvidenceWordService.class);
        when(service.generate("[]")).thenReturn(file);
        MockHttpServletResponse response = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                throw new IllegalStateException("响应已关闭");
            }
        };
        assertThrows(IllegalStateException.class, () -> new EvidenceWordExporter(service).export("[]", response));
        assertFalse(file.exists());
    }

    /**
     * 验证发现接口转交 Service 且搜索前快照为空
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void discoveryEndpointsDelegateToServiceAndSdkAdapterStartsWithEmptySnapshot() throws Exception {
        assertTrue(new SmartLockDiscoveryClient().discoveredDevices().isEmpty());
        SmartLockDiscoveryService service = mock(SmartLockDiscoveryService.class);
        when(service.registerDiscoveredDevices()).thenReturn(true);
        SamrtLockController controller = new SamrtLockController();
        ReflectionTestUtils.setField(controller, "smartLockDiscoveryService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/smart/lock/searchcheck")).andExpect(status().isOk());
        verify(service).startDiscovery();
        mvc.perform(post("/smart/lock/insertLock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(service).registerDiscoveredDevices();
    }
}
