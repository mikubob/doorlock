package com.hnkjzyxy.ab;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.handler.MyMetaObjectHandler;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.FlowService;
import com.hnkjzyxy.ab.service.NoticeService;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.impl.FlowServiceImpl;
import com.hnkjzyxy.ab.service.impl.ProjectTaskGuardImpl;
import com.hnkjzyxy.ab.service.impl.ResultItemServiceImpl;
import com.hnkjzyxy.ab.service.impl.ResultServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 审批有效记录、打回再审及项目行锁的隔离MySQL验证
 * <p>
 * 只允许FLOW_APPROVE_TEST_URL指向flow_approve_test专用库，用户及密码分别由
 * FLOW_APPROVE_TEST_USER、FLOW_APPROVE_TEST_PASSWORD提供，未配置URL时明确跳过。
 * 八张测试表的DDL取自softmanage.sql，数据夹具与断言使用JDBC；业务调用使用真实
 * MyBatis-Plus、Mapper XML、自动填充处理器及Spring事务代理，不使用应用数据源。
 * 通知依赖使用替身，不验证实际消息持久化、缓存切面、认证过滤器或客户端行为。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
class FlowApproveConcurrencyTest {
    /**
     * 专用测试库数据源
     */
    private static DriverManagerDataSource dataSource;
    /**
     * 夹具初始化和持久化状态断言接口
     */
    private static JdbcTemplate jdbc;
    /**
     * 与MyBatis共用数据源的真实事务管理器
     */
    private static DataSourceTransactionManager transactions;
    /**
     * 加载项目现有Mapper XML的会话模板
     */
    private static SqlSessionTemplate session;
    /**
     * 仓库原始建表脚本文本
     */
    private static String structure;
    /**
     * 独立实库事务保护的审批服务
     */
    private FlowService service;
    /**
     * 使用真实MyBatis-Plus批量写入的考核结果服务
     */
    private ResultServiceImpl resultService;
    /**
     * 使用真实MyBatis-Plus新增审批记录的Mapper
     */
    private ResultItemMapper items;
    /**
     * 用于断言打回次数和注入通知故障的替身
     */
    private NoticeService notices;

    /**
     * 校验数据库隔离边界并加载实际数据库映射和全局配置
     *
     * @throws Exception 连接、建表脚本或Mapper加载失败时抛出
     */
    @BeforeAll
    static void connectDedicatedDatabase() throws Exception {
        String url = System.getenv("FLOW_APPROVE_TEST_URL");
        Assumptions.assumeTrue(url != null && !url.isEmpty(), "未配置隔离MySQL，T12并发与回滚未验证");
        if (!url.matches("jdbc:mysql://[^/]+/flow_approve_test(?:\\?.*)?")) {
            throw new IllegalArgumentException("只允许使用flow_approve_test专用数据库");
        }
        String username = System.getenv("FLOW_APPROVE_TEST_USER");
        if (username == null) throw new IllegalArgumentException("必须显式指定测试库用户");
        String password = System.getenv("FLOW_APPROVE_TEST_PASSWORD");
        dataSource = new DriverManagerDataSource(url, username, password == null ? "" : password);
        jdbc = new JdbcTemplate(dataSource);
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8.0."));
        transactions = new DataSourceTransactionManager(dataSource);
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true); config.setCacheEnabled(false);
        GlobalConfig global = new GlobalConfig(); global.setBanner(false);
        global.setMetaObjectHandler(new MyMetaObjectHandler());
        GlobalConfig.DbConfig db = new GlobalConfig.DbConfig(); db.setTablePrefix("sys_");
        db.setIdType(IdType.AUTO); db.setLogicDeleteValue("0"); db.setLogicNotDeleteValue("1");
        global.setDbConfig(db);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource); factory.setConfiguration(config); factory.setGlobalConfig(global);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        session = new SqlSessionTemplate(factory.getObject());
        Path root = Paths.get("").toAbsolutePath();
        if (!Files.exists(root.resolve("soft_main/src/sql/softmanage.sql"))) root = root.getParent();
        structure = new String(Files.readAllBytes(root.resolve("soft_main/src/sql/softmanage.sql")), StandardCharsets.UTF_8);
    }

    /**
     * 重建专用测试表，配置两名候选审批人和一个待审批结果
     */
    @BeforeEach
    void resetFixture() {
        for (String table : Arrays.asList("sys_project", "sys_task", "sys_result", "sys_result_item",
                "sys_flow", "sys_flow_task", "sys_role", "sys_user_role")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
            Matcher ddl = Pattern.compile("CREATE TABLE `" + table + "`[\\s\\S]*?;(?=\\r?\\n)").matcher(structure);
            assertTrue(ddl.find(), table); jdbc.execute(ddl.group());
        }
        jdbc.execute("INSERT INTO sys_project(id,title,create_name,status) VALUES (1,'审批测试','u7',1)");
        jdbc.execute("INSERT INTO sys_task(id,p_id,task_name,standard,score) VALUES ('10',1,'任务一','标准',100)");
        jdbc.execute("INSERT INTO sys_result(id,p_id,u_id,task_id,score,step,is_finish) VALUES (1,1,11,'10',50,0,0)");
        jdbc.execute("INSERT INTO sys_flow(id,p_id,user_id,flow_name,status) VALUES (1,1,7,'测试流程',1)");
        jdbc.execute("INSERT INTO sys_flow_task(id,parent_id,type,sort,u_id,role_id,status) "
                + "VALUES (1,1,'APPROVAL',2,'[7,8]','[]',1)");
        notices = mock(NoticeService.class);
        resultService = new ResultServiceImpl();
        ReflectionTestUtils.setField(resultService, "baseMapper", session.getMapper(ResultMapper.class));
        items = session.getMapper(ResultItemMapper.class);
        service = createService(resultService, items, createGuard());
    }

    /**
     * 为真实任务归属与项目锁服务添加MANDATORY事务代理
     *
     * @return 参与外层审批事务的保护服务
     */
    private ProjectTaskGuard createGuard() {
        ProjectTaskGuardImpl guard = new ProjectTaskGuardImpl();
        ReflectionTestUtils.setField(guard, "mapper", session.getMapper(ProjectTaskImportMapper.class));
        return (ProjectTaskGuard) transactional(guard);
    }

    /**
     * 组装审批业务及注解事务代理，可替换单个写入依赖注入故障
     *
     * @param writes 考核结果写入服务
     * @param inserts 审批记录新增接口
     * @param guard 项目事务保护服务
     * @return 使用真实审批事务注解的服务代理
     */
    private FlowService createService(ResultService writes, ResultItemMapper inserts, ProjectTaskGuard guard) {
        ResultItemServiceImpl itemService = new ResultItemServiceImpl();
        ReflectionTestUtils.setField(itemService, "baseMapper", items);
        FlowServiceImpl target = new FlowServiceImpl();
        ReflectionTestUtils.setField(target, "projectTaskGuard", guard);
        ReflectionTestUtils.setField(target, "userRoleMapper", session.getMapper(UserRoleMapper.class));
        ReflectionTestUtils.setField(target, "flowTaskMapper", session.getMapper(FlowTaskMapper.class));
        ReflectionTestUtils.setField(target, "resultItemService", itemService);
        ReflectionTestUtils.setField(target, "resultItemMapper", inserts);
        ReflectionTestUtils.setField(target, "resultService", writes);
        ReflectionTestUtils.setField(target, "noticeService", notices);
        return (FlowService) transactional(target);
    }

    /**
     * 使用实际Transactional注解创建Spring事务代理
     *
     * @param target 待代理的服务实现
     * @return 具有注解事务拦截器的接口代理
     */
    private static Object transactional(Object target) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }

    /**
     * 请求携带历史ID和无效状态仍新增有效记录，再次提交拒绝且共享结果不变
     */
    @Test
    void serverFieldsCreateNewActiveRecordAndRejectDuplicate() {
        jdbc.execute("INSERT INTO sys_result_item(id,p_id,u_id,user_id,step,status,is_flag) VALUES (100,1,11,7,2,0,1)");
        ResultItem request = request(0, 80); request.setId(100); request.setStatus(0);
        service.submitApprove(request, operator(7));
        assertNotEquals(100, request.getId());
        assertEquals(1, count("status=1 AND user_id=7 AND step=2"));
        assertEquals(1, count("id=100 AND status=0"));
        assertEquals(80, score()); assertEquals(2, resultField("step")); assertEquals(1, resultField("is_finish"));
        ProjectTaskException error = assertThrows(ProjectTaskException.class,
                () -> service.submitApprove(request(0, 99), operator(7)));
        assertEquals(409, error.getStatus()); assertEquals(80, score()); assertEquals(2, count("1=1"));
        verifyNoInteractions(notices);
    }

    /**
     * 保持五维有效记录判重，其他项目、对象、审批人或步骤的记录不阻止首次审批
     */
    @Test
    void duplicateDimensionsAndMultipleApproversRemainIndependent() {
        jdbc.execute("INSERT INTO sys_result_item(p_id,u_id,user_id,step,status,is_flag) VALUES "
                + "(2,11,7,2,1,0),(1,22,7,2,1,0),(1,11,9,2,1,0),(1,11,7,1,1,0)");
        service.submitApprove(request(0, 80), operator(7));
        service.submitApprove(request(0, 90), operator(8));
        assertEquals(2, count("p_id=1 AND u_id=11 AND step=2 AND user_id IN (7,8) AND status=1"));
        assertEquals(90, score()); assertEquals(1, resultField("is_finish"));
    }

    /**
     * 无流程权限及非法打回标志在真实入口均拒绝，结果和审批记录保持基线
     */
    @Test
    void permissionAndFlagRejectionsHaveNoDatabaseSideEffects() {
        ProjectTaskException forbidden = assertThrows(ProjectTaskException.class,
                () -> service.submitApprove(request(0, 80), operator(9)));
        assertEquals(403, forbidden.getStatus());
        for (Integer flag : Arrays.asList(null, -1, 2)) {
            ResultItem invalid = request(0, 80); invalid.setIsFlag(flag);
            ProjectTaskException error = assertThrows(ProjectTaskException.class,
                    () -> service.submitApprove(invalid, operator(7)));
            assertEquals(400, error.getStatus());
        }
        assertBaseline(); verifyNoInteractions(notices);
    }

    /**
     * 角色候选权限选取首个匹配节点，非最终节点保留未完成状态
     */
    @Test
    void roleCandidateUsesFirstMatchingStep() {
        jdbc.execute("INSERT INTO sys_role(role_id,role_name,role_code,role_status,weight) VALUES (8,'审批角色','approver',1,9)");
        jdbc.execute("INSERT INTO sys_user_role(user_id,role_id) VALUES (9,8)");
        jdbc.execute("UPDATE sys_flow_task SET role_id='[8]'");
        jdbc.execute("INSERT INTO sys_flow_task(parent_id,type,sort,u_id,role_id,status) "
                + "VALUES (1,'APPROVAL',3,'[9]','[]',1)");
        service.submitApprove(request(0, 80), operator(9));
        assertEquals(1, count("user_id=9 AND step=2 AND status=1"));
        assertEquals(2, resultField("step")); assertEquals(0, resultField("is_finish"));
    }

    /**
     * 打回失效全部有效历史，模拟重新提交后的结果状态验证再审批，并记录直接重放仍可执行的边界
     */
    @Test
    void returnedHistoryAllowsReapprovalAndDirectReplayRemainsPossible() {
        service.submitApprove(request(0, 80), operator(8));
        service.submitApprove(request(1, 60), operator(7));
        assertEquals(0, count("status=1")); assertEquals(2, count("status=0"));
        assertEquals(3, resultField("is_finish")); assertEquals(0, resultField("step"));
        service.submitApprove(request(1, 60), operator(7));
        assertEquals(3, count("status=0")); verify(notices, times(2)).noticeReturn(any());
        jdbc.update("UPDATE sys_result SET step=0,is_finish=0,score=70 WHERE id=1");
        service.submitApprove(request(0, 90), operator(7));
        assertEquals(3, count("status=0")); assertEquals(1, count("status=1")); assertEquals(90, score());
    }

    /**
     * 结果更新后报告失败，已有实际写入随外层审批事务回滚
     */
    @Test
    void resultFailureRollsBackActualBatchUpdate() {
        ResultService failing = mock(ResultService.class);
        doAnswer(call -> {
            resultService.updateBatchById(call.getArgument(0)); return false;
        }).when(failing).updateBatchById(anyCollection());
        FlowService failed = createService(failing, items, createGuard());
        assertThrows(IllegalStateException.class, () -> failed.submitApprove(request(0, 80), operator(7)));
        assertBaseline();
        service.submitApprove(request(0, 90), operator(7)); assertEquals(90, score());
    }

    /**
     * 审批记录实际插入后报告失败，结果更新与记录新增在同一事务中整体回滚
     */
    @Test
    void insertFailureRollsBackResultAndRecord() {
        ResultItemMapper failing = mock(ResultItemMapper.class);
        when(failing.insert(any(ResultItem.class))).thenAnswer(call -> {
            items.insert(call.getArgument(0)); return 0;
        });
        FlowService failed = createService(resultService, failing, createGuard());
        assertThrows(IllegalStateException.class, () -> failed.submitApprove(request(0, 80), operator(7)));
        assertBaseline(); verifyNoInteractions(notices);
    }

    /**
     * 打回通知抛出技术异常时，结果、记录新增及历史失效均整体回滚
     */
    @Test
    void notificationFailureRollsBackReturnChanges() {
        service.submitApprove(request(0, 80), operator(8));
        doThrow(new IllegalStateException("测试通知故障")).when(notices).noticeReturn(any());
        assertThrows(IllegalStateException.class, () -> service.submitApprove(request(1, 60), operator(7)));
        assertEquals(80, score()); assertEquals(2, resultField("step")); assertEquals(1, resultField("is_finish"));
        assertEquals(1, count("status=1 AND user_id=8")); assertEquals(1, count("1=1"));
    }

    /**
     * 两个独立服务代理和连接竞争同一项目锁，首次提交成功后后者冲突，首次失败后后者成功
     *
     * @param failFirst 是否在首次持锁写入后制造回滚
     * @throws Exception 并发执行、连接检查或超时断言失败时抛出
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void independentTransactionsSerializeAndReleaseLock(boolean failFirst) throws Exception {
        CountDownLatch firstWritten = new CountDownLatch(1), releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        Set<Connection> connections = ConcurrentHashMap.newKeySet();
        AtomicInteger attempts = new AtomicInteger();
        ProjectTaskGuardImpl guarded = spy(new ProjectTaskGuardImpl());
        ReflectionTestUtils.setField(guarded, "mapper", session.getMapper(ProjectTaskImportMapper.class));
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            Connection connection = DataSourceUtils.getConnection(dataSource);
            connections.add(connection);
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, connection.getTransactionIsolation());
            if (attempts.incrementAndGet() == 2) secondAttempted.countDown();
            return call.callRealMethod();
        }).when(guarded).lock(1);
        ProjectTaskGuard observed = (ProjectTaskGuard) transactional(guarded);
        ResultService held = mock(ResultService.class);
        doAnswer(call -> {
            boolean updated = resultService.updateBatchById(call.getArgument(0));
            firstWritten.countDown();
            assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
            return failFirst ? false : updated;
        }).when(held).updateBatchById(anyCollection());
        FlowService first = createService(held, items, observed);
        FlowService second = createService(resultService, items, observed);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> initial = workers.submit(() -> submitStatus(first, 80));
            assertTrue(firstWritten.await(5, TimeUnit.SECONDS));
            Future<Integer> next = workers.submit(() -> submitStatus(second, 90));
            assertTrue(secondAttempted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> next.get(300, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            assertEquals(failFirst ? 500 : 200, initial.get(5, TimeUnit.SECONDS));
            assertEquals(failFirst ? 200 : 409, next.get(5, TimeUnit.SECONDS));
            assertEquals(2, connections.size());
            assertEquals(1, count("status=1")); assertEquals(1, count("1=1"));
            assertEquals(failFirst ? 90 : 80, score());
        } finally {
            releaseFirst.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * 执行并发测试审批，保留业务冲突和注入故障的不同结果
     *
     * @param flow 独立事务代理
     * @param score 待审批分数
     * @return 成功200、业务异常状态码或测试注入故障500
     */
    private static int submitStatus(FlowService flow, int score) {
        try {
            flow.submitApprove(request(0, score), operator(7)); return 200;
        } catch (ProjectTaskException error) {
            return error.getStatus();
        } catch (IllegalStateException error) {
            return 500;
        }
    }

    /**
     * 构造故意携带伪造审批人和步骤的合法任务请求
     *
     * @param flag 是否打回
     * @param score 审批分数
     * @return 独立请求实体
     */
    private static ResultItem request(int flag, int score) {
        Result result = new Result(); result.setId(1); result.setTaskId("10"); result.setScore(score);
        ResultItem request = new ResultItem(); request.setPId(1); request.setUId(11);
        request.setUserId(99); request.setStep(99); request.setIsFlag(flag);
        request.setResults(Collections.singletonList(result)); return request;
    }

    /**
     * 构造认证审批人快照
     *
     * @param id 当前审批人ID
     * @return 用户快照
     */
    private static User operator(int id) {
        User user = new User(); user.setUserId(id); user.setUserName("u" + id); return user;
    }

    /**
     * 统计包含逻辑删除历史在内的审批记录
     *
     * @param condition 仅由本测试常量构成的SQL条件
     * @return 记录数
     */
    private static int count(String condition) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_result_item WHERE " + condition, Integer.class);
    }

    /**
     * 读取共享结果的审批分数
     *
     * @return 当前分数
     */
    private static int score() {
        return resultField("score");
    }

    /**
     * 读取本测试固定列名对应的共享结果字段
     *
     * @param field 本测试使用的固定列名
     * @return 字段值
     */
    private static int resultField(String field) {
        return jdbc.queryForObject("SELECT " + field + " FROM sys_result WHERE id=1", Integer.class);
    }

    /**
     * 断言拒绝或失败后的数据库仍为初始基线
     */
    private static void assertBaseline() {
        assertEquals(50, score()); assertEquals(0, resultField("step"));
        assertEquals(0, resultField("is_finish")); assertEquals(0, count("1=1"));
    }
}
