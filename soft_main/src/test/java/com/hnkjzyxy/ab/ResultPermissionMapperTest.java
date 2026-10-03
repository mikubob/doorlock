package com.hnkjzyxy.ab;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.hnkjzyxy.ab.dto.ResultAccessScope;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.*;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.ResultService;
import com.hnkjzyxy.ab.service.impl.ProjectTaskGuardImpl;
import com.hnkjzyxy.ab.service.impl.ResultServiceImpl;
import com.hnkjzyxy.ab.service.security.ResultPermissionPolicy;
import com.hnkjzyxy.ab.service.security.RoleAssert;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 子任务成绩权限SQL、行锁及评分事务的隔离MySQL验证
 * <p>
 * 只连接显式配置的独立测试库，加载真实Mapper XML，并为成绩Service创建注解事务代理。
 * 使用 {@code RESULT_PERMISSION_TEST_URL}（库名必须为 {@code result_permission_test}）、
 * {@code RESULT_PERMISSION_TEST_USER} 和 {@code RESULT_PERMISSION_TEST_PASSWORD} 提供连接资料。
 * 缺少URL时明确跳过并报告数据库验证未完成，不回退到dev或prd配置。
 * </p>
 * <p>
 * 每个用例重建专用库中的六张测试表，只保留权限场景所需字段，禁止指定业务库。
 * 并发用例另建连接调整目标学院，验证用户行锁实际阻塞及事务结束后的后续请求范围。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
class ResultPermissionMapperTest {
    /**
     * 仅连接显式配置的专用MySQL测试库的数据源
     */
    private static DriverManagerDataSource dataSource;
    /**
     * 专用测试表初始化、故障注入及持久化结果核对接口
     */
    private static JdbcTemplate jdbc;
    /**
     * 与真实Mapper共用数据源的事务管理器
     */
    private static DataSourceTransactionManager transactions;
    /**
     * 加载真实XML的成绩Mapper代理
     */
    private static ResultMapper results;
    /**
     * 加载真实XML的有效角色查询代理
     */
    private static UserRoleMapper roles;
    /**
     * 已应用实际Transactional注解的成绩Service事务代理
     */
    private static ResultService service;
    /**
     * 认证用户快照，学院刻意保留旧值以验证重新查库授权
     */
    private static User operator;

    /**
     * 校验专用测试库配置并构造真实Mapper和评分事务链路
     * <p>
     * URL未配置时通过测试假设明确中止此类用例；库名不符或用户未配置时直接失败。
     * 仅装配测试所需对象，不启动完整应用，也不读取业务环境数据源。
     * </p>
     *
     * @throws Exception 数据源配置、Mapper XML加载或事务代理构造失败时抛出
     */
    @BeforeAll
    static void connectExplicitTestDatabase() throws Exception {
        String url = System.getenv("RESULT_PERMISSION_TEST_URL");
        // 不存在独立测试连接时不可使用应用默认连接，更不能把跳过解释为数据库验收通过。
        Assumptions.assumeTrue(url != null && !url.isEmpty(), "未配置隔离MySQL库，SQL/锁/事务验证未完成");
        if (!url.matches("jdbc:mysql://[^/]+/result_permission_test(?:\\?.*)?")) {
            throw new IllegalArgumentException("只能使用专用result_permission_test数据库");
        }
        String username = System.getenv("RESULT_PERMISSION_TEST_USER");
        if (username == null) throw new IllegalArgumentException("必须显式指定隔离库用户");
        String password = System.getenv("RESULT_PERMISSION_TEST_PASSWORD");
        dataSource = new DriverManagerDataSource(url, username, password == null ? "" : password);
        jdbc = new JdbcTemplate(dataSource);
        transactions = new DataSourceTransactionManager(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        // 与项目的下划线字段映射约定一致；本测试显式关闭二级缓存以观察当前数据库状态。
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        GlobalConfig global = new GlobalConfig(); global.setBanner(false);
        GlobalConfig.DbConfig db = new GlobalConfig.DbConfig(); db.setTablePrefix("sys_");
        db.setLogicDeleteValue("0"); db.setLogicNotDeleteValue("1"); global.setDbConfig(db);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource); factory.setConfiguration(configuration); factory.setGlobalConfig(global);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        results = session.getMapper(ResultMapper.class);
        roles = session.getMapper(UserRoleMapper.class);
        ProjectTaskImportMapper tasks = session.getMapper(ProjectTaskImportMapper.class);
        ProjectTaskGuardImpl guard = new ProjectTaskGuardImpl();
        ReflectionTestUtils.setField(guard, "mapper", tasks);
        ResultServiceImpl target = new ResultServiceImpl();
        ReflectionTestUtils.setField(target, "resultMapper", results);
        ReflectionTestUtils.setField(target, "projectTaskImportMapper", tasks);
        ReflectionTestUtils.setField(target, "projectTaskGuard", guard);
        ReflectionTestUtils.setField(target, "resultPermissionPolicy",
                new ResultPermissionPolicy(new RoleAssert(roles, session.getMapper(UserMapper.class))));
        // 直接调用Service对象不会执行@Transactional，必须添加真实事务拦截器验证整批回滚。
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (ResultService) proxy.getProxy();
        operator = new User(); operator.setUserId(7); operator.setUserName("u7");
        operator.setStatus(1); operator.setCollege("stale-cache");
    }

    /**
     * 在已校验的专用数据库内重建六张测试表并恢复角色、学院及结果基线
     * <p>
     * 样本包含停用目标账号、大小写与尾部空格不同的学院、空学院、重复或缺失角色关联，
     * 以及结果任务和项目不一致的记录；用于区分合法历史数据与越界或失效数据。
     * </p>
     */
    @BeforeEach
    void resetOnlyDedicatedTestTables() {
        jdbc.execute("DROP TABLE IF EXISTS sys_result, sys_task, sys_project, sys_user_role, sys_role, sys_user");
        jdbc.execute("CREATE TABLE sys_user (user_id INT PRIMARY KEY, user_name VARCHAR(64), nick_name VARCHAR(64), "
                + "avatar VARCHAR(255), password VARCHAR(255), email VARCHAR(255), phone VARCHAR(32), sex VARCHAR(8), major VARCHAR(255), "
                + "create_time DATETIME, entry_time DATETIME, last_login DATETIME, status INT, college VARCHAR(255)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE sys_role (role_id INT PRIMARY KEY, role_name VARCHAR(255) UNIQUE, role_code VARCHAR(255) UNIQUE, role_status INT, weight INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE sys_user_role (user_id INT, role_id INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE sys_project (id INT PRIMARY KEY, title VARCHAR(255), start_time DATETIME, end_time DATETIME, status INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE sys_task (id VARCHAR(32) PRIMARY KEY, p_id INT, task_name VARCHAR(255), category VARCHAR(255), score INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE sys_result (id INT PRIMARY KEY, p_id INT, task_id VARCHAR(32), u_id INT, score INT, evidence TEXT) ENGINE=InnoDB");
        // 用户11为停用的历史成绩目标，目标状态不能代替本次学院归属判断。
        jdbc.execute("INSERT INTO sys_user(user_id,user_name,status,college) VALUES (7,'u7',1,'A'),(11,'u11',0,'A'),(22,'u22',1,'B'),(33,'u33',1,'a'),(44,'u44',1,'A '),(55,'u55',1,NULL)");
        jdbc.execute("INSERT INTO sys_role VALUES (8,'二级学院院长','dean',1,9),(17,'书记','clerk',1,9),(2,'超级用户','admin',1,10),(1,'普通用户','normal',1,100)");
        jdbc.execute("INSERT INTO sys_user_role VALUES (7,17),(7,8),(7,8),(7,1),(7,999)");
        jdbc.execute("INSERT INTO sys_project(id,title,status) VALUES (1,'项目一',1),(2,'项目二',1)");
        jdbc.execute("INSERT INTO sys_task VALUES ('10',1,'任务一','教学',100),('20',2,'任务二','教学',100)");
        jdbc.execute("INSERT INTO sys_result VALUES (1,1,'10',11,50,'[]'),(2,1,'10',22,60,'[]'),(3,1,'10',33,70,'[]'),"
                + "(4,1,'10',44,75,'[]'),(5,1,'10',55,80,'[]'),(6,2,'20',11,40,'[]'),(7,1,'20',11,99,'[]')");
    }

    /**
     * 验证真实角色SQL去重并排除停用、缺失或空编码角色，角色停用后的请求立即拒绝
     */
    @Test
    void roleQueryDeduplicatesAndExcludesDisabledMissingAndEmptyRoles() {
        assertEquals(3, roles.selectActiveRoleCodes(7).size());
        assertTrue(roles.selectActiveRoleCodes(7).contains("dean"));
        jdbc.update("UPDATE sys_role SET role_status=0 WHERE role_id=8");
        assertFalse(roles.selectActiveRoleCodes(7).contains("dean"));
        assertDenied(403, () -> service.getLists(new SubTaskIdDto(), operator));
        jdbc.update("UPDATE sys_role SET role_code=' ' WHERE role_id=1");
        jdbc.update("UPDATE sys_role SET role_code=NULL WHERE role_id=17");
        assertTrue(roles.selectActiveRoleCodes(7).isEmpty());
        assertDenied(403, () -> service.getList(new SubTaskDto(), operator));
        assertTrue(roles.selectActiveRoleCodes(999).isEmpty());
    }

    /**
     * 验证新旧真实查询的字段筛选、精确学院过滤、任务项目一致性及漏传范围时默认拒绝
     */
    @Test
    void realLegacyAndNewQueriesRespectExactCollegeAndTaskProjectJoin() {
        assertEquals(2, service.getList(new SubTaskDto(), operator).get("total"));
        assertEquals(2, service.getLists(new SubTaskIdDto(), operator).get("total"));
        SubTaskDto names = new SubTaskDto(); names.setTitle("项目一"); names.setTaskName("任务一"); names.setTaskCategory("教学");
        assertEquals(1, service.getList(names, operator).get("total"));
        SubTaskIdDto dto = new SubTaskIdDto(); dto.setProjectId("1"); dto.setTaskName("任务一");
        dto.setTaskCategory("教学"); dto.setLimitScore(51);
        assertEquals(0, service.getLists(dto, operator).get("total"));
        dto.setLimitScore(null); dto.setUserId("22");
        assertEquals(0, service.getLists(dto, operator).get("total"));
        dto.setUserId("11");
        assertEquals(1, service.getLists(dto, operator).get("total"));
        assertTrue(results.getUsersSubTaskScore(null, null, null, null).isEmpty());
        assertTrue(results.getUsersSubTaskScoreById(new SubTaskIdDto(), null).isEmpty());
    }

    /**
     * 验证管理员权重变化不影响全学院查看，修改仍要求有效院长角色和本学院范围
     */
    @Test
    void adminIdentityIgnoresWeightAndViewsAllButEditingStillNeedsDeanCollege() {
        jdbc.update("INSERT INTO sys_user_role VALUES (7,2)");
        for (int weight : Arrays.asList(10, 100)) {
            jdbc.update("UPDATE sys_role SET weight=? WHERE role_id=2", weight);
            assertEquals(6, service.getList(new SubTaskDto(), operator).get("total"));
            assertEquals(6, service.getLists(new SubTaskIdDto(), operator).get("total"));
        }
        assertDenied(403, () -> service.updateSubTaskScore(Collections.singletonList(row("1", "10", "22", 80)), operator));
        jdbc.update("UPDATE sys_role SET role_status=0 WHERE role_id=8");
        assertDenied(403, () -> service.updateSubTaskScore(Collections.singletonList(row("1", "10", "11", 80)), operator));
    }

    /**
     * 验证角色显示名称不能授予身份，停用及大小写或尾空格异常编码不能沿用院长权限
     */
    @Test
    void nameChangesNeverGrantPermissionAndRoleDisableTakesEffectNextRequest() {
        assertEquals(2, service.getLists(new SubTaskIdDto(), operator).get("total"));
        jdbc.update("UPDATE sys_role SET role_name='院长' WHERE role_id=17");
        jdbc.update("UPDATE sys_role SET role_status=0 WHERE role_id=8");
        assertDenied(403, () -> service.getLists(new SubTaskIdDto(), operator));
        jdbc.update("UPDATE sys_role SET role_status=1,role_code='DEAN' WHERE role_id=8");
        assertDenied(403, () -> service.getLists(new SubTaskIdDto(), operator));
        jdbc.update("UPDATE sys_role SET role_code='dean ' WHERE role_id=8");
        assertDenied(403, () -> service.getLists(new SubTaskIdDto(), operator));
        jdbc.update("UPDATE sys_role SET role_code='dean' WHERE role_id=8");
        assertEquals(2, service.getLists(new SubTaskIdDto(), operator).get("total"));
    }

    /**
     * 验证数据库当前禁用状态及学院变更覆盖认证用户旧快照，查询和评分均使用新范围
     */
    @Test
    void currentStatusAndCollegeOverrideCachedAccount() {
        jdbc.update("UPDATE sys_user SET status=0 WHERE user_id=7");
        assertDenied(401, () -> service.getLists(new SubTaskIdDto(), operator));
        jdbc.update("UPDATE sys_user SET status=1,college='B' WHERE user_id=7");
        assertEquals(1, service.getList(new SubTaskDto(), operator).get("total"));
        service.updateSubTaskScore(Collections.singletonList(row("1", "10", "22", 80)), operator);
        assertEquals(80, score(2));
        assertDenied(403, () -> service.updateSubTaskScore(Collections.singletonList(row("1", "10", "11", 80)), operator));
    }

    /**
     * 验证混合学院批次全部分数保持不变，合法多项目评分提交且同分数重试成功
     */
    @Test
    void mixedCollegeBatchLeavesAllScoresUnchangedAndLegalBatchCommits() {
        assertDenied(403, () -> service.updateSubTaskScore(Arrays.asList(row("1", "10", "11", 80), row("1", "10", "22", 90)), operator));
        assertEquals(50, score(1)); assertEquals(60, score(2));
        List<SubTaskIdDto> legal = Arrays.asList(row("2", "20", "11", 90), row("1", "10", "11", 80));
        service.updateSubTaskScore(legal, operator);
        assertEquals(80, score(1)); assertEquals(90, score(6));
        service.updateSubTaskScore(legal, operator); // 同分数合法重复提交不能按影响行数误报失败。
        assertEquals(80, score(1));
        assertThrows(ProjectTaskException.class,
                () -> service.updateSubTaskScore(Collections.singletonList(row("1", "20", "11", 80)), operator));
    }

    /**
     * 验证现有雪花任务ID按字符串传递，不因超出int范围而被误拒绝
     */
    @Test
    void preservesExistingSnowflakeStringTaskIds() {
        String taskId = "504291504043327488";
        jdbc.update("UPDATE sys_task SET id=? WHERE id='10'", taskId);
        jdbc.update("UPDATE sys_result SET task_id=? WHERE task_id='10'", taskId);
        service.updateSubTaskScore(Collections.singletonList(row("1", taskId, "11", 80)), operator);
        assertEquals(80, score(1));
    }

    /**
     * 验证更新SQL独立防御缺失或误传全学院范围，并只更新明确学院内的目标
     */
    @Test
    void updateSqlCannotBypassCollegeWithNullOrAllScope() {
        List<SubTaskIdDto> rows = Arrays.asList(row("1", "10", "11", 80), row("1", "10", "22", 90));
        results.updateBySubTaskName(rows, null);
        results.updateBySubTaskName(rows, ResultAccessScope.all(7));
        assertEquals(50, score(1)); assertEquals(60, score(2));
        results.updateBySubTaskName(rows, ResultAccessScope.college(7, "A"));
        assertEquals(80, score(1)); assertEquals(60, score(2));
    }

    /**
     * 在第二条更新注入数据库异常，验证同一评分事务中此前成功执行的更新也会回滚
     */
    @Test
    void laterSqlFailureRollsBackEarlierUpdateInSameScoringTransaction() {
        jdbc.update("UPDATE sys_user SET college='A' WHERE user_id=22");
        // 两个目标均合法，使失败发生在SQL写入阶段，验证回滚而非仅验证写入前拒绝。
        jdbc.execute("CREATE TRIGGER fail_second_score BEFORE UPDATE ON sys_result FOR EACH ROW "
                + "BEGIN IF NEW.u_id=22 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced test failure'; END IF; END");
        assertThrows(RuntimeException.class, () -> service.updateSubTaskScore(
                Arrays.asList(row("1", "10", "11", 80), row("1", "10", "22", 90)), operator));
        assertEquals(50, score(1)); assertEquals(60, score(2));
    }

    /**
     * 验证并发学院调整等待目标用户锁释放，提交后新的评分请求按调整后的学院拒绝
     *
     * @throws Exception 并发任务执行、限时等待或数据库操作失败时抛出
     */
    @Test
    void targetCollegeUpdateWaitsForScoringTransactionToReleaseUserLock() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch attempted = new CountDownLatch(1);
        final Future<?>[] changing = new Future<?>[1];
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactions);
            transaction.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
            transaction.execute(status -> {
                assertEquals("A", results.lockScoreTargetUser(11).getCollege());
                changing[0] = executor.submit(() -> {
                    // 通知主线程已开始并发操作，后续更新使用另一个线程的独立数据库连接。
                    attempted.countDown();
                    jdbc.update("UPDATE sys_user SET college='B' WHERE user_id=11");
                });
                try { assertTrue(attempted.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                // 事务仍持有用户锁，此时更新应保持等待；提交后再检查并发更新可以完成。
                assertThrows(TimeoutException.class, () -> changing[0].get(300, TimeUnit.MILLISECONDS));
                service.updateSubTaskScore(Collections.singletonList(row("1", "10", "11", 80)), operator);
                assertEquals(80, score(1));
                return null;
            });
            changing[0].get(5, TimeUnit.SECONDS);
            assertEquals("B", jdbc.queryForObject("SELECT college FROM sys_user WHERE user_id=11", String.class));
            assertDenied(403, () -> service.updateSubTaskScore(Collections.singletonList(row("1", "10", "11", 90)), operator));
            assertEquals(80, score(1));
        } finally { executor.shutdownNow(); }
    }

    /**
     * 读取指定测试结果的实际持久化分数
     *
     * @param id 测试结果主键
     * @return 当前数据库分数，用于提交及回滚前后比较
     */
    private static int score(int id) { return jdbc.queryForObject("SELECT score FROM sys_result WHERE id=?", Integer.class, id); }

    /**
     * 断言实际Service调用抛出指定状态码的权限异常
     *
     * @param status 预期的401或403状态码
     * @param action 通过真实数据源执行的Service调用
     */
    private static void assertDenied(int status, Runnable action) {
        assertEquals(status, assertThrows(AuthPermissionException.class, action::run).getStatus());
    }
    /**
     * 构造指定目标及分数的测试评分项
     *
     * @param project 请求项目ID
     * @param task 字符串任务ID，可为既有雪花ID
     * @param user 目标用户ID
     * @param score 待写入分数
     * @return 用于真实评分事务的请求项
     */
    private static SubTaskIdDto row(String project, String task, String user, int score) {
        SubTaskIdDto row = new SubTaskIdDto(); row.setProjectId(project); row.setTaskId(task); row.setUserId(user); row.setScore(score); return row;
    }
}
