package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.controller.ProjectController;
import com.hnkjzyxy.ab.dto.ProjectAssessRow;
import com.hnkjzyxy.ab.dto.ProjectAssessScope;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.handler.GlobalExceptionHandler;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.impl.ProjectServiceImpl;
import com.hnkjzyxy.ab.service.utils.ProjectAssessScopeResolver;
import com.hnkjzyxy.ab.vo.ProjectVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 项目考核列表服务及HTTP分页契约回归
 * <p>
 * 验证当前页排名、宽数值边界、认证身份来源和局部参数校验。
 * SQL排序、接收人关系及事务快照由隔离MySQL测试验证。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
class ProjectAssessPaginationTest {
    /**
     * 当前页数据访问替身
     */
    private ProjectMapper projects;
    /**
     * 有效最高角色查询替身
     */
    private UserRoleMapper roles;
    /**
     * 实际考核列表服务
     */
    private ProjectServiceImpl service;
    /**
     * 已认证的普通教师
     */
    private User operator;

    /**
     * 装配真实服务与范围解析器，默认使用本人已有结果分支
     */
    @BeforeEach
    void prepareService() {
        projects = mock(ProjectMapper.class);
        roles = mock(UserRoleMapper.class);
        service = new ProjectServiceImpl();
        ReflectionTestUtils.setField(service, "projectMapper", projects);
        ReflectionTestUtils.setField(service, "projectAssessScopeResolver", new ProjectAssessScopeResolver(roles));
        operator = new User();
        operator.setUserId(7);
        Role role = new Role(); role.setWeight(1);
        when(roles.getRoleWeight(7)).thenReturn(role);
    }

    /**
     * 验证首页、中页、末页的全局顺序编号与顶层兼容字段
     *
     * @param page 请求页码
     * @param count 当前页条数
     * @param firstRank 本页首条排名
     */
    @ParameterizedTest
    @CsvSource({"1,10,1", "2,10,11", "3,5,21"})
    void assignsRanksOnlyToCurrentPage(int page, int count, int firstRank) {
        ProjectQueryParam param = new ProjectQueryParam(); param.setPage(page);
        when(projects.countAssessList(eq(param), any())).thenReturn(25L);
        List<ProjectAssessRow> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ProjectAssessRow row = new ProjectAssessRow(); row.setTotalScore(90L);
            row.setProjectId(46); row.setUserId(100 + i); rows.add(row);
        }
        when(projects.selectAssessListPage(eq(param), any(), eq((long) firstRank - 1), eq(10))).thenReturn(rows);
        ApiResult result = service.getProjectAssessList(param, operator);
        assertEquals(25L, result.get("total")); assertEquals(3L, result.get("totalPage"));
        assertEquals(page, result.get("page")); assertEquals(10, result.get("limit"));
        assertFalse(result.containsKey("data"));
        List<?> list = (List<?>) result.get("list"); assertEquals(count, list.size());
        for (int i = 0; i < count; i++) {
            ProjectVo vo = (ProjectVo) list.get(i);
            assertEquals(ProjectVo.class, vo.getClass());
            assertEquals(firstRank + i, vo.getRank().intValue()); assertEquals(90, vo.getScore().intValue());
        }
        verify(projects, never()).getProjectAssessFlow(any(), any(), any());
        verify(roles, times(1)).getRoleWeight(7);
    }

    /**
     * 空结果及极大页码不读取数据页，不发生int偏移溢出
     */
    @Test
    void preservesTotalsAndSkipsDataForEmptyAndOutOfRangePages() {
        ProjectQueryParam param = new ProjectQueryParam(); param.setPage(Integer.MAX_VALUE); param.setLimit(100);
        when(projects.countAssessList(eq(param), any())).thenReturn(20L);
        ApiResult result = service.getProjectAssessList(param, operator);
        assertEquals(20L, result.get("total")); assertEquals(1L, result.get("totalPage"));
        assertTrue(((List<?>) result.get("list")).isEmpty());
        when(projects.countAssessList(eq(param), any())).thenReturn(0L);
        assertEquals(0L, service.getProjectAssessList(param, operator).get("totalPage"));
        verify(projects, never()).selectAssessListPage(any(), any(), anyLong(), anyInt());
    }

    /**
     * 原VO总分及排名容量不足时返回受控异常，不截断宽数值
     */
    @Test
    void rejectsScoreAndRankOverflow() {
        ProjectQueryParam param = new ProjectQueryParam();
        when(projects.countAssessList(any(), any())).thenReturn(Long.MAX_VALUE);
        ProjectAssessRow row = new ProjectAssessRow(); row.setTotalScore((long) Integer.MAX_VALUE + 1);
        when(projects.selectAssessListPage(any(), any(), anyLong(), anyInt())).thenReturn(Collections.singletonList(row));
        assertEquals(500, assertThrows(ProjectTaskException.class,
                () -> service.getProjectAssessList(param, operator)).getStatus());
        row.setTotalScore(0L); param.setPage(Integer.MAX_VALUE); param.setLimit(100);
        assertEquals(500, assertThrows(ProjectTaskException.class,
                () -> service.getProjectAssessList(param, operator)).getStatus());
    }

    /**
     * 非法页码或页大小在任何查询之前拒绝
     *
     * @param page 请求页码
     * @param limit 请求页大小
     */
    @ParameterizedTest
    @CsvSource({"0,10", "-1,10", "1,0", "1,-1", "1,101"})
    void rejectsInvalidPaginationBeforeQuery(int page, int limit) {
        ProjectQueryParam param = new ProjectQueryParam(); param.setPage(page); param.setLimit(limit);
        assertEquals(400, assertThrows(ProjectTaskException.class,
                () -> service.getProjectAssessList(param, operator)).getStatus());
        verifyNoInteractions(projects, roles);
    }

    /**
     * 非法筛选和倒置区间在查询之前拒绝
     */
    @Test
    void rejectsInvalidFilters() {
        ProjectQueryParam param = new ProjectQueryParam(); param.setStatus(4);
        assertThrows(ProjectTaskException.class, () -> service.getProjectAssessList(param, operator));
        param.setStatus(null); param.setProjectId(0);
        assertThrows(ProjectTaskException.class, () -> service.getProjectAssessList(param, operator));
        param.setProjectId(null); param.setRoleId(-1);
        assertThrows(ProjectTaskException.class, () -> service.getProjectAssessList(param, operator));
        param.setRoleId(null); param.setStartTime(new Date(2)); param.setEndTime(new Date(1));
        assertThrows(ProjectTaskException.class, () -> service.getProjectAssessList(param, operator));
        verifyNoInteractions(projects, roles);
    }

    /**
     * 缺失角色受控拒绝；权重10保留原有空页，不扩大管理员权限
     */
    @Test
    void deniesMissingRoleAndKeepsUncoveredWeightEmpty() {
        when(roles.getRoleWeight(7)).thenReturn(null);
        assertEquals(403, assertThrows(AuthPermissionException.class,
                () -> service.getProjectAssessList(new ProjectQueryParam(), operator)).getStatus());
        Role role = new Role(); when(roles.getRoleWeight(7)).thenReturn(role);
        assertThrows(AuthPermissionException.class, () -> service.getProjectAssessList(new ProjectQueryParam(), operator));
        role.setWeight(10);
        assertEquals(0L, service.getProjectAssessList(new ProjectQueryParam(), operator).get("total"));
        verifyNoInteractions(projects);
    }

    /**
     * HTTP默认可选参数、绑定错误及伪造userId保持局部校验和认证来源
     *
     * @throws Exception HTTP测试执行失败时抛出
     */
    @Test
    void verifiesHttpDefaultsErrorsAndAuthenticatedIdentity() throws Exception {
        ProjectController controller = new ProjectController();
        UserService users = mock(UserService.class); when(users.getUserByName("teacher")).thenReturn(operator);
        ReflectionTestUtils.setField(controller, "projectService", service);
        ReflectionTestUtils.setField(controller, "userService", users);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("teacher", "ignored");
        mvc.perform(get("/project/assess/list").principal(auth).param("userId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.totalPage").value(0)).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.limit").value(10)).andExpect(jsonPath("$.data").doesNotExist());
        verify(projects).countAssessList(any(), argThat(s -> s.getOperatorId() == 7
                && s.getType() == ProjectAssessScope.Type.SELF_RESULTS));
        mvc.perform(get("/project/assess/list").principal(auth).param("limit", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/project/assess/list").principal(auth).param("roleId", "invalid"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
