package com.hnkjzyxy.ab;

import com.hnkjzyxy.ab.controller.FlowController;
import com.hnkjzyxy.ab.handler.GlobalExceptionHandler;
import com.hnkjzyxy.ab.mapper.FlowTaskMapper;
import com.hnkjzyxy.ab.mapper.ResultItemMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.NoticeService;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ResultItemService;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.impl.FlowServiceImpl;
import com.hnkjzyxy.ab.vo.ApproveVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 审批提交的HTTP拒绝响应及服务端字段边界验证
 * <p>
 * 使用真实Controller、审批Service及全局异常处理器，仅替换数据访问与通知依赖。
 * 不加载认证过滤器、缓存切面或数据库；事务和行锁由独立实库测试验证。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
class FlowApproveHttpTest {
    /**
     * 独立MVC请求执行器
     */
    private MockMvc mvc;
    /**
     * 当前用户对应的流程节点查询替身
     */
    private FlowTaskMapper flowTasks;
    /**
     * 有效审批记录计数替身
     */
    private ResultItemService items;
    /**
     * 审批记录新增接口替身
     */
    private ResultItemMapper itemMapper;
    /**
     * 共享考核结果更新替身
     */
    private ResultService results;
    /**
     * 项目锁与任务归属校验替身
     */
    private ProjectTaskGuard guard;
    /**
     * 打回通知替身，用于验证拒绝请求无通知
     */
    private NoticeService notices;
    /**
     * 测试工号对应的认证主体
     */
    private UsernamePasswordAuthenticationToken auth;

    /**
     * 组装真实审批HTTP调用链，默认允许用户7在步骤2审批
     */
    @BeforeEach
    void setUp() {
        flowTasks = mock(FlowTaskMapper.class);
        items = mock(ResultItemService.class);
        itemMapper = mock(ResultItemMapper.class);
        results = mock(ResultService.class);
        guard = mock(ProjectTaskGuard.class);
        notices = mock(NoticeService.class);
        UserRoleMapper roles = mock(UserRoleMapper.class);
        UserService users = mock(UserService.class);
        User operator = new User(); operator.setUserId(7); operator.setUserName("u7");
        when(users.getUserByName("u7")).thenReturn(operator);
        when(roles.getRoles(7)).thenReturn(Collections.emptyList());
        ApproveVo node = new ApproveVo(); node.setSort(2); node.setUId("[7,8]"); node.setRoleId("[]");
        when(flowTasks.getFlowStepList("APPROVAL", 1)).thenReturn(Collections.singletonList(node));
        when(flowTasks.getFlowMaxSort("APPROVAL", 1)).thenReturn(2);
        when(items.count(any())).thenReturn(0);
        when(results.updateBatchById(anyCollection())).thenReturn(true);
        when(itemMapper.insert(any(ResultItem.class))).thenReturn(1);
        FlowServiceImpl service = new FlowServiceImpl();
        ReflectionTestUtils.setField(service, "projectTaskGuard", guard);
        ReflectionTestUtils.setField(service, "userRoleMapper", roles);
        ReflectionTestUtils.setField(service, "flowTaskMapper", flowTasks);
        ReflectionTestUtils.setField(service, "resultItemService", items);
        ReflectionTestUtils.setField(service, "resultItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "resultService", results);
        ReflectionTestUtils.setField(service, "noticeService", notices);
        FlowController controller = new FlowController();
        ReflectionTestUtils.setField(controller, "flowService", service);
        ReflectionTestUtils.setField(controller, "userService", users);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        auth = new UsernamePasswordAuthenticationToken("u7", "unused", Collections.emptyList());
    }

    /**
     * 首次审批保留成功响应，客户端主键、状态、审批人及步骤不能决定新增记录
     *
     * @throws Exception 请求执行或断言失败时抛出
     */
    @Test
    void successUsesServerFieldsAndPreservesResultProgress() throws Exception {
        mvc.perform(post("/submit/approve").principal(auth).contentType("application/json").content(body(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("审批成功！"));
        ArgumentCaptor<ResultItem> saved = ArgumentCaptor.forClass(ResultItem.class);
        verify(itemMapper).insert(saved.capture());
        ResultItem item = saved.getValue();
        assertNull(item.getId()); assertEquals(1, item.getStatus());
        assertEquals(7, item.getUserId()); assertEquals(2, item.getStep());
        assertEquals(2, item.getResults().get(0).getStep());
        assertEquals(1, item.getResults().get(0).getIsFinish());
        org.mockito.InOrder order = inOrder(guard, items, results, itemMapper);
        order.verify(guard).lock(1);
        order.verify(guard).validateTasks(eq(1), eq(11), anyList(), eq(true));
        order.verify(items).count(any());
        order.verify(results).updateBatchById(anyCollection());
        order.verify(itemMapper).insert(any(ResultItem.class));
        verifyNoInteractions(notices);
    }

    /**
     * 有效记录重复返回实际HTTP409，拒绝后无结果、记录及通知写入
     *
     * @throws Exception 请求执行或断言失败时抛出
     */
    @Test
    void duplicateReturnsHttp409WithoutWrites() throws Exception {
        when(items.count(any())).thenReturn(1);
        mvc.perform(post("/submit/approve").principal(auth).contentType("application/json").content(body(0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("已进行审批，不能再重复审批！"));
        verifyNoInteractions(results, itemMapper, notices);
    }

    /**
     * 无对应审批节点返回实际HTTP403，伪造审批人和步骤不能取得权限
     *
     * @throws Exception 请求执行或断言失败时抛出
     */
    @Test
    void missingPermissionReturnsHttp403WithoutWrites() throws Exception {
        when(flowTasks.getFlowStepList("APPROVAL", 1)).thenReturn(Collections.emptyList());
        mvc.perform(post("/submit/approve").principal(auth).contentType("application/json").content(body(0)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("没有审批权限！"));
        verifyNoInteractions(items, results, itemMapper, notices);
    }

    /**
     * 非0/1打回标志返回实际HTTP400，在取得项目锁和业务写入之前拒绝
     *
     * @param flag 非法打回标志
     * @throws Exception 请求执行或断言失败时抛出
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, 2, 2147483647})
    void invalidReturnFlagIsHttp400WithoutSideEffects(int flag) throws Exception {
        mvc.perform(post("/submit/approve").principal(auth).contentType("application/json").content(body(flag)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("是否打回仅允许0或1"));
        verifyNoInteractions(guard, items, results, itemMapper, notices);
    }

    /**
     * 结果更新失败沿用技术异常处理，不能被归类为审批冲突或继续插入记录
     *
     * @throws Exception 请求执行或断言失败时抛出
     */
    @Test
    void failedResultUpdateIsNotReportedAsConflict() throws Exception {
        when(results.updateBatchById(anyCollection())).thenReturn(false);
        mvc.perform(post("/submit/approve").principal(auth).contentType("application/json").content(body(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("审批结果更新失败，已回滚"));
        verifyNoInteractions(itemMapper, notices);
    }

    /**
     * 构造故意携带客户端主键、无效状态、伪造步骤和审批人的合法任务请求
     *
     * @param flag 是否打回
     * @return 审批提交JSON
     */
    private static String body(int flag) {
        return "{\"pId\":1,\"uId\":11,\"id\":693,\"status\":0,\"userId\":99,\"step\":99,\"isFlag\":"
                + flag + ",\"results\":[{\"id\":1,\"pId\":1,\"uId\":11,\"taskId\":\"10\",\"score\":80}]}";
    }
}
