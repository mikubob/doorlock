package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.dto.ResultAccessScope;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.impl.ResultServiceImpl;
import com.hnkjzyxy.ab.service.security.ResultPermissionPolicy;
import com.hnkjzyxy.ab.service.security.RoleAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 子任务成绩权限策略及Service调用顺序验证
 * <p>
 * 使用真实断言、策略及成绩Service，隔离账号、角色、成绩和项目保护接口。
 * 验证三个目标入口的身份、角色矩阵、当前学院及授权拒绝后不查询成绩、不加业务锁、不写分数。
 * 此类验证校验顺序；真实SQL、行锁及事务回滚由隔离MySQL测试负责。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
class ResultPermissionPolicyTest {
    /**
     * 当前数据库账号查询替身
     */
    private UserMapper users;
    /**
     * 全部有效角色码查询替身
     */
    private UserRoleMapper roles;
    /**
     * 成绩查询、用户锁及批量评分接口替身
     */
    private ResultMapper results;
    /**
     * 真实任务归属及结果存在性查询替身
     */
    private ProjectTaskImportMapper tasks;
    /**
     * 项目锁及生命周期保护接口替身
     */
    private ProjectTaskGuard guard;
    /**
     * 实际执行的成绩权限策略
     */
    private ResultPermissionPolicy policy;
    /**
     * 实际执行的当前账号及角色断言组件
     */
    private RoleAssert roleAssert;
    /**
     * 待验证的成绩Service实现
     */
    private ResultServiceImpl service;
    /**
     * 来自认证链路的用户快照，学院刻意设置为旧值
     */
    private User operator;
    /**
     * Mapper返回的当前数据库用户，作为实际状态和学院判定依据
     */
    private User current;

    /**
     * 构造真实权限链路并为每个用例重置接口替身
     */
    @BeforeEach
    void setUp() {
        users = mock(UserMapper.class);
        roles = mock(UserRoleMapper.class);
        results = mock(ResultMapper.class);
        tasks = mock(ProjectTaskImportMapper.class);
        guard = mock(ProjectTaskGuard.class);
        roleAssert = new RoleAssert(roles, users);
        policy = new ResultPermissionPolicy(roleAssert);
        service = new ResultServiceImpl();
        ReflectionTestUtils.setField(service, "resultPermissionPolicy", policy);
        ReflectionTestUtils.setField(service, "resultMapper", results);
        ReflectionTestUtils.setField(service, "projectTaskImportMapper", tasks);
        ReflectionTestUtils.setField(service, "projectTaskGuard", guard);
        operator = user(7, "cached-college");
        current = user(7, "A");
        when(users.selectById(7)).thenReturn(current);
        codes("dean");
    }

    /**
     * 验证空用户、空ID或空工号在账号、角色及业务查询前被拒绝为401
     */
    @Test
    void rejectsMissingIdentityBeforeAnyMapper() {
        for (User invalid : Arrays.asList(null, new User(), user(null, "A"))) {
            rejected(401, () -> service.getList(new SubTaskDto(), invalid));
            rejected(401, () -> service.getLists(new SubTaskIdDto(), invalid));
            rejected(401, () -> service.updateSubTaskScore(null, invalid));
        }
        User noName = user(7, "A");
        noName.setUserName(null);
        rejected(401, () -> policy.requireView(noName));
        verifyNoInteractions(users, roles, results, tasks, guard);
    }

    /**
     * 验证当前数据库账号缺失、禁用或工号不一致时拒绝身份且不继续查询角色
     */
    @Test
    void rejectsMissingDisabledOrMismatchedCurrentAccount() {
        when(users.selectById(7)).thenReturn(null);
        rejected(401, () -> policy.requireView(operator));
        when(users.selectById(7)).thenReturn(current);
        for (Integer status : Arrays.asList(null, 0)) {
            current.setStatus(status);
            rejected(401, () -> policy.requireEdit(operator));
        }
        current.setStatus(1);
        current.setUserName("another-login");
        rejected(401, () -> policy.requireView(operator));
        verifyNoInteractions(roles, results, tasks, guard);
    }

    /**
     * 验证空角色明确返回403，数据库异常按原异常传播且不触发业务读写
     */
    @Test
    void rejectsNullOrEmptyRolesAndPropagatesDatabaseFailure() {
        when(roles.selectActiveRoleCodes(7)).thenReturn(null);
        assertAllThreeDenied();
        when(roles.selectActiveRoleCodes(7)).thenReturn(Collections.emptyList());
        assertAllThreeDenied();
        RuntimeException failure = new RuntimeException("database unavailable");
        when(roles.selectActiveRoleCodes(7)).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class, () -> service.getLists(new SubTaskIdDto(), operator)));
        verifyNoInteractions(results, tasks, guard);
    }

    /**
     * 验证非允许身份及不规范角色码不能通过任一成绩入口
     *
     * @param code 不允许授权或与有效院长编码不精确相等的测试角色码
     */
    @ParameterizedTest
    @ValueSource(strings = {"clerk", "normal", "director", "student", "", "DEAN", "dean ", "dean_test"})
    void deniesDisallowedAndMalformedRoleCodes(String code) {
        codes(code);
        assertAllThreeDenied();
        verifyNoInteractions(results, tasks, guard);
    }

    /**
     * 验证多角色顺序及重复角色不影响院长身份，范围使用当前学院且角色集合不可修改
     */
    @Test
    void exactMultiRoleIdentityUsesCurrentCollegeAndImmutableScope() {
        for (List<String> codes : Arrays.asList(Arrays.asList("clerk", "dean"),
                Arrays.asList("higher", "dean", "dean"), Arrays.asList("dean", "clerk"))) {
            when(roles.selectActiveRoleCodes(7)).thenReturn(codes);
            assertEquals("A", policy.requireView(operator).getCollege());
            assertEquals("A", policy.requireEdit(operator).getCollege());
        }
        current.setCollege("B");
        assertEquals("B", policy.requireView(operator).getCollege());
        assertThrows(UnsupportedOperationException.class,
                () -> roleAssert.requireAnyOf(operator, "dean").getCodes().add("admin"));
        rejected(403, () -> roleAssert.requireAnyOf(operator));
        rejected(403, () -> roleAssert.requireAnyOf(operator, (String[]) null));
        verify(roles, never()).getRoleWeight(any());
    }

    /**
     * 验证管理员统一查看新旧接口，管理员兼院长仍只获得本学院修改范围
     */
    @Test
    void adminMayViewBothQueriesButNeedsDeanToEdit() {
        codes("admin");
        current.setCollege(null);
        when(results.getUsersSubTaskScore(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(results.getUsersSubTaskScoreById(any(), any())).thenReturn(Collections.emptyList());
        assertEquals(0, service.getList(new SubTaskDto(), operator).get("total"));
        assertEquals(Collections.emptyList(), service.getLists(new SubTaskIdDto(), operator).get("list"));
        ArgumentCaptor<ResultAccessScope> scope = ArgumentCaptor.forClass(ResultAccessScope.class);
        verify(results).getUsersSubTaskScore(any(), any(), any(), scope.capture());
        assertTrue(scope.getValue().isAllColleges());
        rejected(403, () -> service.updateSubTaskScore(null, operator));
        codes("admin", "dean");
        current.setCollege("A");
        assertTrue(policy.requireView(operator).isAllColleges());
        assertFalse(policy.requireEdit(operator).isAllColleges());
        assertEquals("A", policy.requireEdit(operator).getCollege());
    }

    /**
     * 验证院长学院为空或含首尾空白时拒绝授权，不生成全学院范围
     *
     * @param college 缺失或不规范的学院配置
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " A", "A "})
    void refusesInvalidDeanCollege(String college) {
        current.setCollege(college);
        assertAllThreeDenied();
        assertThrows(IllegalArgumentException.class, () -> ResultAccessScope.college(7, college));
        verifyNoInteractions(results, tasks, guard);
    }

    /**
     * 验证批量评分先按项目升序加锁，再按去重目标用户升序加锁，最后执行完整批量写入
     */
    @Test
    void locksProjectsThenDistinctTargetsInAscendingOrderBeforeBatchWrite() {
        prepareTask("20", 2);
        prepareTask("10", 1);
        when(results.lockScoreTargetUser(11)).thenReturn(user(11, "A"));
        when(results.lockScoreTargetUser(22)).thenReturn(user(22, "A"));
        List<SubTaskIdDto> rows = Arrays.asList(row("2", "20", "22"), row("1", "10", "11"), row("2", "20", "22"));
        service.updateSubTaskScore(rows, operator);
        InOrder order = inOrder(guard, results);
        order.verify(guard).lock(1);
        order.verify(guard).lock(2);
        order.verify(results).lockScoreTargetUser(11);
        order.verify(results).lockScoreTargetUser(22);
        ArgumentCaptor<ResultAccessScope> scope = ArgumentCaptor.forClass(ResultAccessScope.class);
        order.verify(results).updateBySubTaskName(eq(rows), scope.capture());
        assertEquals("A", scope.getValue().getCollege());
        verify(results, times(1)).lockScoreTargetUser(22);
        verify(tasks, times(3)).scoreResults(anyInt(), anyString(), anyInt());
    }

    /**
     * 验证任一目标越界、学院缺失或用户失效均拒绝整批且不调用更新接口
     */
    @Test
    void crossCollegeOrMissingTargetDeniesEntireBatchWithoutWriting() {
        prepareTask("10", 1);
        when(results.lockScoreTargetUser(11)).thenReturn(user(11, "A"));
        when(results.lockScoreTargetUser(22)).thenReturn(user(22, "B"));
        List<SubTaskIdDto> rows = Arrays.asList(row("1", "10", "11"), row("1", "10", "22"));
        rejected(403, () -> service.updateSubTaskScore(rows, operator));
        when(results.lockScoreTargetUser(22)).thenReturn(user(22, null));
        rejected(403, () -> service.updateSubTaskScore(rows, operator));
        when(results.lockScoreTargetUser(22)).thenReturn(null);
        assertEquals(409, assertThrows(ProjectTaskException.class, () -> service.updateSubTaskScore(rows, operator)).getStatus());
        verify(results, never()).updateBySubTaskName(any(), any());
    }

    /**
     * 验证整批参数在业务查询前校验，并保留持锁后的失效结果检查
     */
    @Test
    void validatesWholeInputBeforeBusinessQueriesAndRetainsInvalidResultGuard() {
        assertThrows(ProjectTaskException.class, () -> service.updateSubTaskScore(null, operator));
        assertThrows(ProjectTaskException.class, () -> service.updateSubTaskScore(Collections.emptyList(), operator));
        assertThrows(RuntimeException.class, () -> service.updateSubTaskScore(Arrays.asList(row("1", "10", "11"), null), operator));
        assertThrows(ProjectTaskException.class, () -> service.updateSubTaskScore(Arrays.asList(row("1", "10", "11"), row("1", "10", "bad")), operator));
        verifyNoInteractions(tasks, results, guard);
        prepareTask("10", 1);
        when(results.lockScoreTargetUser(11)).thenReturn(user(11, "A"));
        when(tasks.scoreResults(1, "10", 11)).thenReturn(0);
        assertEquals(409, assertThrows(ProjectTaskException.class,
                () -> service.updateSubTaskScore(Collections.singletonList(row("1", "10", "11")), operator)).getStatus());
        verify(results, never()).updateBySubTaskName(any(), any());
    }

    /**
     * 断言新旧查询及评分入口均因当前测试角色或学院配置返回403
     */
    private void assertAllThreeDenied() {
        rejected(403, () -> service.getList(new SubTaskDto(), operator));
        rejected(403, () -> service.getLists(new SubTaskIdDto(), operator));
        rejected(403, () -> service.updateSubTaskScore(null, operator));
    }

    /**
     * 设置当前账号的有效角色码查询结果
     *
     * @param values 按给定顺序返回的测试角色码
     */
    private void codes(String... values) { when(roles.selectActiveRoleCodes(7)).thenReturn(Arrays.asList(values)); }

    /**
     * 断言操作抛出指定状态码的专用权限异常
     *
     * @param status 预期的401或403状态码
     * @param action 待执行的校验或业务调用
     */
    private static void rejected(int status, Runnable action) {
        assertEquals(status, assertThrows(AuthPermissionException.class, action::run).getStatus());
    }
    /**
     * 设置任务真实归属及有效结果数量，供合法评分或后续失效场景使用
     *
     * @param id 字符串任务ID
     * @param project 任务所属项目ID
     */
    private void prepareTask(String id, int project) {
        Task task = new Task(); task.setPId(project);
        when(tasks.task(id)).thenReturn(task);
        when(tasks.scoreResults(eq(project), eq(id), anyInt())).thenReturn(1);
    }
    /**
     * 构造具有工号及启用状态的测试用户
     *
     * @param id 用户ID，身份缺失用例允许为 null
     * @param college 测试学院，可为 null 或不规范值
     * @return 仅设置测试所需身份及学院字段的用户
     */
    private static User user(Integer id, String college) {
        User user = new User(); user.setUserId(id); user.setUserName("u" + id); user.setStatus(1); user.setCollege(college); return user;
    }
    /**
     * 构造分数为80的测试评分项
     *
     * @param project 请求中的项目ID
     * @param task 请求中的字符串任务ID
     * @param user 请求中的目标用户ID
     * @return 用于参数、归属或学院范围验证的评分项
     */
    private static SubTaskIdDto row(String project, String task, String user) {
        SubTaskIdDto row = new SubTaskIdDto(); row.setProjectId(project); row.setTaskId(task); row.setUserId(user); row.setScore(80); return row;
    }
}
