package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.controller.AuthController;
import com.hnkjzyxy.ab.controller.ProjectController;
import com.hnkjzyxy.ab.export.ExcelResponseExporter;
import com.hnkjzyxy.ab.handler.GlobalExceptionHandler;
import com.hnkjzyxy.ab.job.EvidenceFileCleanupJob;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectItemImportParam;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.security.user.AccountUser;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.ProjectTaskImportService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.support.EvidenceFileCleanupService;
import com.hnkjzyxy.ab.vo.AssessVo;
import com.hnkjzyxy.ab.vo.ProjectTaskImportResult;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StructureRefactorWebTest {
    @Test
    void editingContactDetailsAcceptsExistingFieldsAndUsesAuthenticatedIdentity() throws Exception {
        UserService users = mock(UserService.class);
        User authenticated = new User();
        authenticated.setUserId(7);
        when(users.getUserByName("1001")).thenReturn(authenticated);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "userService", users);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/edit/user")
                .principal(new UsernamePasswordAuthenticationToken("1001", "unused"))
                .contentType("application/json")
                .content("{\"userName\":\"1001\",\"email\":\"user@example.com\",\"phone\":\"13800138000\",\"userId\":99,\"weight\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(users).updateById(updated.capture());
        assertEquals(7, updated.getValue().getUserId());
        assertEquals("user@example.com", updated.getValue().getEmail());
        assertEquals("13800138000", updated.getValue().getPhone());
    }

    @Test
    void projectItemEndpointsBindSeparateSaveAndSelectionRequests() throws Exception {
        ProjectService projects = mock(ProjectService.class);
        ProjectTaskImportService imports = mock(ProjectTaskImportService.class);
        when(imports.merge(anyList(), any())).thenReturn(new ProjectTaskImportResult());
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "projectService", projects);
        ReflectionTestUtils.setField(controller, "projectTaskImportService", imports);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        AccountUser account = new AccountUser(7, "1001", "unused", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                account, "unused", Collections.emptyList());
        String payload = "{\"id\":8,\"projectId\":3,\"parentId\":1,\"grade\":2,"
                + "\"pname\":\"任务\",\"standard\":\"标准\",\"score\":10}";
        mvc.perform(post("/project/item").principal(authentication)
                .contentType("application/json").content(payload)).andExpect(status().isOk());
        ArgumentCaptor<ProjectItemSaveParam> saved = ArgumentCaptor.forClass(ProjectItemSaveParam.class);
        verify(projects).addOrUpdateProjectItem(saved.capture(), any());
        assertEquals("任务", saved.getValue().getPname());
        assertEquals(10, saved.getValue().getScore());
        mvc.perform(post("/project/item/insertIntoTask").principal(authentication)
                .contentType("application/json").content("[" + payload + "]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<List> selected = ArgumentCaptor.forClass(List.class);
        verify(imports).merge(selected.capture(), any());
        ProjectItemImportParam selection = (ProjectItemImportParam) selected.getValue().get(0);
        assertEquals(8, selection.getId());
        assertEquals(3, selection.getProjectId());
    }

    @Test
    void cleanupJobRemainsDisabledByDefault() {
        EvidenceFileCleanupService service = mock(EvidenceFileCleanupService.class);
        new EvidenceFileCleanupJob(service, false).clearFileJob();
        verifyNoInteractions(service);
        new EvidenceFileCleanupJob(service, true).clearFileJob();
        verify(service).clearFiles();
    }

    @Test
    void exporterStillWritesAnXlsxWithTheOriginalHeaders() throws Exception {
        AssessVo row = new AssessVo();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelResponseExporter.exportAssess(Collections.singletonList(row), response);
        assertTrue(response.getContentType().startsWith("application/vnd.openxmlformats"));
        assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("导出列表", workbook.getSheetName(0));
            assertEquals("项目名称", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }
}
