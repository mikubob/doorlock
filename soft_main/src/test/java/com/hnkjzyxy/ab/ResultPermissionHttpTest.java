package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.controller.ProjectController;
import com.hnkjzyxy.ab.handler.GlobalExceptionHandler;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.impl.ResultServiceImpl;
import com.hnkjzyxy.ab.service.security.ResultPermissionPolicy;
import com.hnkjzyxy.ab.service.security.RoleAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 子任务成绩接口的身份、权限及响应契约验证
 * <p>
 * 使用真实Controller、Service、权限策略及全局异常处理器，仅替换外部数据接口。
 * 通过独立MockMvc验证实际HTTP状态、业务码和成功结构，不启动应用数据源或认证过滤器。
 * 未登录请求在全局Security过滤链中的响应不属于本类的验证范围。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
class ResultPermissionHttpTest {
    /**
     * 独立MVC请求执行器，未加载全局Security过滤链
     */
    private MockMvc mvc;
    /**
     * 按认证主体工号取得用户快照的服务替身
     */
    private UserService userService;
    /**
     * 重新读取当前数据库账号的接口替身
     */
    private UserMapper users;
    /**
     * 有效角色查询接口替身
     */
    private UserRoleMapper roles;
    /**
     * 成绩查询及更新接口替身
     */
    private ResultMapper results;
    /**
     * 任务归属及结果有效性查询替身
     */
    private ProjectTaskImportMapper tasks;
    /**
     * 项目锁及状态保护服务替身
     */
    private ProjectTaskGuard guard;
    /**
     * 测试账号的当前数据库资料
     */
    private User current;
    /**
     * 已认证的测试主体，与测试用户工号一致
     */
    private UsernamePasswordAuthenticationToken auth;

    /**
     * 构造真实MVC权限链路并重置各测试替身
     */
    @BeforeEach
    void setUp() {
        users = mock(UserMapper.class); roles = mock(UserRoleMapper.class);
        results = mock(ResultMapper.class); tasks = mock(ProjectTaskImportMapper.class);
        guard = mock(ProjectTaskGuard.class); userService = mock(UserService.class);
        current = new User(); current.setUserId(7); current.setUserName("u7");
        current.setStatus(1); current.setCollege("A");
        when(userService.getUserByName("u7")).thenReturn(current);
        when(users.selectById(7)).thenReturn(current);
        when(roles.selectActiveRoleCodes(7)).thenReturn(Collections.singletonList("dean"));
        when(results.getUsersSubTaskScore(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(results.getUsersSubTaskScoreById(any(), any())).thenReturn(Collections.emptyList());
        ResultServiceImpl service = new ResultServiceImpl();
        ReflectionTestUtils.setField(service, "resultPermissionPolicy", new ResultPermissionPolicy(new RoleAssert(roles, users)));
        ReflectionTestUtils.setField(service, "resultMapper", results);
        ReflectionTestUtils.setField(service, "projectTaskImportMapper", tasks);
        ReflectionTestUtils.setField(service, "projectTaskGuard", guard);
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "resultService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        auth = new UsernamePasswordAuthenticationToken("u7", "unused", Collections.emptyList());
    }

    /**
     * 验证无有效角色或不允许身份在三个入口均返回真实HTTP403且不读写成绩
     *
     * @param code 测试角色码，{@code none} 表示无有效角色
     * @throws Exception MVC请求执行或响应断言失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"none", "clerk", "normal", "director", "DEAN", "dean ", "dean_test"})
    void forbiddenIsHttp403AndNeverReadsOrWritesBusinessData(String code) throws Exception {
        when(roles.selectActiveRoleCodes(7)).thenReturn("none".equals(code) ? Collections.emptyList() : Collections.singletonList(code));
        for (MockHttpServletRequestBuilder request : requests()) {
            mvc.perform(request.principal(auth)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403)).andExpect(jsonPath("$.msg").isNotEmpty())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
        verifyNoInteractions(results, tasks, guard);
    }

    /**
     * 验证缺失、未认证、匿名主体及无法解析的操作人均返回真实HTTP401
     *
     * @throws Exception MVC请求执行或响应断言失败时抛出
     */
    @Test
    void missingUnauthenticatedAnonymousOrUnresolvedOperatorIsHttp401() throws Exception {
        for (MockHttpServletRequestBuilder request : requests()) {
            mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        }
        UsernamePasswordAuthenticationToken unauthenticated = new UsernamePasswordAuthenticationToken("u7", "unused");
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        for (MockHttpServletRequestBuilder request : requests()) {
            mvc.perform(request.principal(unauthenticated)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/project/subTaskScores").principal(anonymous)).andExpect(status().isUnauthorized());
        verifyNoInteractions(userService, users, roles, results, tasks, guard);
        when(userService.getUserByName("u7")).thenReturn(null);
        for (MockHttpServletRequestBuilder request : requests()) {
            mvc.perform(request.principal(auth)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        }
        verifyNoInteractions(results, tasks, guard);
    }

    /**
     * 验证认证主体仍有效但当前数据库账号禁用时，三个入口均拒绝为401
     *
     * @throws Exception MVC请求执行或响应断言失败时抛出
     */
    @Test
    void disabledCurrentAccountIs401DespiteAuthenticatedPrincipal() throws Exception {
        current.setStatus(0);
        for (MockHttpServletRequestBuilder request : requests()) {
            mvc.perform(request.principal(auth)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        }
        verifyNoInteractions(roles, results, tasks, guard);
    }

    /**
     * 验证管理员新旧查询保留total/list结构，但仅管理员身份调用评分返回403
     *
     * @throws Exception MVC请求执行或响应断言失败时抛出
     */
    @Test
    void adminCanViewBothEndpointsWithExistingStructureButCannotScore() throws Exception {
        when(roles.selectActiveRoleCodes(7)).thenReturn(Collections.singletonList("admin"));
        for (String path : Arrays.asList("/project/subTaskScore", "/project/subTaskScores")) {
            mvc.perform(get(path).principal(auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.list").isArray());
        }
        mvc.perform(scoreRequest().principal(auth)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        verify(results, never()).updateBySubTaskName(any(), any());
        verifyNoInteractions(tasks, guard);
    }

    /**
     * 验证院长合法评分保留成功提示，越界目标返回403且不会追加更新调用
     *
     * @throws Exception MVC请求执行或响应断言失败时抛出
     */
    @Test
    void deanScoringPreservesSuccessResponseAndCrossCollegeIs403() throws Exception {
        Task task = new Task(); task.setPId(1);
        when(tasks.task("10")).thenReturn(task);
        when(tasks.scoreResults(1, "10", 11)).thenReturn(1);
        User target = new User(); target.setUserId(11); target.setCollege("A");
        when(results.lockScoreTargetUser(11)).thenReturn(target);
        mvc.perform(scoreRequest().principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.msg").value("修改成功"));
        verify(results, times(1)).updateBySubTaskName(any(), any());
        target.setCollege("B");
        mvc.perform(scoreRequest().principal(auth)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("无权限修改其他学院成绩"));
        verify(results, times(1)).updateBySubTaskName(any(), any());
    }

    /**
     * 构造三个目标成绩入口的独立请求
     *
     * @return 旧查询、新查询及批量评分请求，调用方按用例设置认证主体
     */
    private static MockHttpServletRequestBuilder[] requests() {
        return new MockHttpServletRequestBuilder[]{get("/project/subTaskScore"), get("/project/subTaskScores"), scoreRequest()};
    }
    /**
     * 构造目标用户11、项目1、任务10且分数为80的评分请求
     *
     * @return 使用JSON请求体的批量评分PUT请求
     */
    private static MockHttpServletRequestBuilder scoreRequest() {
        return put("/project/subTaskScore").contentType("application/json")
                .content("[{\"projectId\":\"1\",\"taskId\":\"10\",\"userId\":\"11\",\"score\":80}]");
    }
}
