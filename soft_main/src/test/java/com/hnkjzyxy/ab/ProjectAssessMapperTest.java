package com.hnkjzyxy.ab;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.hnkjzyxy.ab.dto.ProjectAssessRow;
import com.hnkjzyxy.ab.dto.ProjectAssessScope;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.exception.ProjectTaskException;
import com.hnkjzyxy.ab.mapper.ProjectMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.FlowTask;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.impl.ProjectServiceImpl;
import com.hnkjzyxy.ab.service.utils.ProjectAssessScopeResolver;
import com.hnkjzyxy.ab.vo.ProjectVo;
import com.sun.management.ThreadMXBean;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalAnswers;
import org.mockito.stubbing.Answer;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * 项目考核列表真实XML、JSON关系、筛选及事务快照验证
 * <p>
 * 只允许显式配置PROJECT_ASSESS_TEST_URL指向project_assess_test专用库，
 * 使用PROJECT_ASSESS_TEST_USER及PROJECT_ASSESS_TEST_PASSWORD连接。
 * 每个用例只重建该库的七张测试表，DDL来自softmanage.sql，绝不使用应用数据源。
 * 缺少URL时明确跳过，不将跳过解释为SQL验收完成。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
class ProjectAssessMapperTest {
    /**
     * 本接口涉及的测试表
     */
    private static final List<String> TABLES = Arrays.asList("sys_project", "sys_flow", "sys_flow_task",
            "sys_result", "sys_user", "sys_role", "sys_user_role");
    /**
     * 专用测试库连接
     */
    private static DriverManagerDataSource dataSource;
    /**
     * 测试数据及断言接口
     */
    private static JdbcTemplate jdbc;
    /**
     * 与MyBatis共用的数据源事务管理器
     */
    private static DataSourceTransactionManager transactions;
    /**
     * 加载真实XML及旧分页插件的项目Mapper
     */
    private static ProjectMapper projects;
    /**
     * 真实最高有效角色Mapper
     */
    private static UserRoleMapper roles;
    /**
     * 旧版逐人总分、状态查询，仅用于同环境性能对照
     */
    private static ResultMapper legacyResults;
    /**
     * 旧版逐人资料查询，仅用于同环境性能对照
     */
    private static UserMapper legacyUsers;
    /**
     * 真实SQL及往返次数观察器
     */
    private static final SqlObserver observer = new SqlObserver();
    /**
     * 真实XML配置，用于生成带绑定参数的执行计划
     */
    private static Configuration mappings;
    /**
     * 仓库根目录
     */
    private static Path root;
    /**
     * 实际结构文件文本
     */
    private static String structure;
    /**
     * 具有真实事务代理的考核列表服务
     */
    private ProjectService service;

    /**
     * 校验专用库并加载XML，保留现有分页插件以验证不会重复分页
     *
     * @throws Exception 配置或资源加载失败时抛出
     */
    @BeforeAll
    static void connectOnlyDedicatedDatabase() throws Exception {
        String url = System.getenv("PROJECT_ASSESS_TEST_URL");
        Assumptions.assumeTrue(url != null && !url.isEmpty(), "未配置隔离MySQL，T10 SQL及事务验证未执行");
        if (!url.matches("jdbc:mysql://[^/]+/project_assess_test(?:\\?.*)?")) {
            throw new IllegalArgumentException("只允许使用project_assess_test专用数据库");
        }
        String username = System.getenv("PROJECT_ASSESS_TEST_USER");
        if (username == null) throw new IllegalArgumentException("必须显式指定专用测试库用户");
        String password = System.getenv("PROJECT_ASSESS_TEST_PASSWORD");
        dataSource = new DriverManagerDataSource(url, username, password == null ? "" : password);
        jdbc = new JdbcTemplate(dataSource);
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8.0."));
        transactions = new DataSourceTransactionManager(dataSource);
        MybatisConfiguration config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
        config.setCacheEnabled(false);
        GlobalConfig global = new GlobalConfig(); global.setBanner(false);
        GlobalConfig.DbConfig db = new GlobalConfig.DbConfig(); db.setTablePrefix("sys_"); global.setDbConfig(db);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource); factory.setConfiguration(config); factory.setGlobalConfig(global);
        factory.setPlugins(new PaginationInterceptor(), observer);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        SqlSessionFactory sqlFactory = factory.getObject();
        mappings = sqlFactory.getConfiguration();
        SqlSessionTemplate session = new SqlSessionTemplate(sqlFactory);
        projects = session.getMapper(ProjectMapper.class); roles = session.getMapper(UserRoleMapper.class);
        legacyResults = session.getMapper(ResultMapper.class); legacyUsers = session.getMapper(UserMapper.class);
        root = Paths.get("").toAbsolutePath();
        if (!Files.exists(root.resolve("soft_main/src/sql/softmanage.sql"))) root = root.getParent();
        structure = new String(Files.readAllBytes(root.resolve("soft_main/src/sql/softmanage.sql")), StandardCharsets.UTF_8);
    }

    /**
     * 按实际结构重建专用测试表并构造跨项目、重复角色及混合状态样本
     */
    @BeforeEach
    void resetFixture() {
        for (String table : TABLES) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
            Matcher ddl = Pattern.compile("CREATE TABLE `" + table + "`[\\s\\S]*?;(?=\\r?\\n)").matcher(structure);
            assertTrue(ddl.find(), table); jdbc.execute(ddl.group());
        }
        jdbc.execute("INSERT INTO sys_user(user_id,nick_name,user_name,status) VALUES "
                + "(1,'leader','u1',1),(2,'dean','u2',1),(3,'director','u3',1),(4,'teacher','u4',1),(5,'admin','u5',1),"
                + "(10,'Ab%_中','u10',1),(11,'ab_中','u11',1),(12,NULL,'u12',1)");
        jdbc.execute("INSERT INTO sys_role(role_id,role_name,role_code,role_status,weight) VALUES "
                + "(1,'普通','normal',1,1),(2,'领导','leader',1,20),(3,'院长','dean',1,9),(4,'主任','director',1,5),"
                + "(5,'管理员','admin',1,10),(10,'旧教研室','oldDept',0,2),(11,'其他教研室','dept',1,2),(12,'共享职务','shared',1,2),"
                + "(20,'接收名单','recipients',1,2)");
        jdbc.execute("INSERT INTO sys_user_role(user_id,role_id) VALUES "
                + "(1,2),(2,3),(2,12),(3,4),(3,10),(4,1),(5,5),(10,10),(10,10),(10,20),(10,20),(10,12),(11,11),(999,20)");
        jdbc.execute("INSERT INTO sys_project(id,title,create_name,start_time,end_time,status) VALUES "
                + "(1,'项目一','fixture','2026-01-01','2026-12-31',1),"
                + "(2,'项目二','fixture','2026-02-01','2026-11-30',1),"
                + "(3,'已删除','fixture','2026-01-01','2026-12-31',3),"
                + "(4,'空状态','fixture','2026-01-01','2026-12-31',NULL),"
                + "(5,'本人历史','fixture','2026-01-01','2026-12-31',1)");
        jdbc.execute("INSERT INTO sys_flow(id,p_id,user_id,status) VALUES (1,1,1,1),(2,2,1,1),(3,3,1,1),(4,4,1,1),(6,5,1,0)");
        jdbc.execute("INSERT INTO sys_flow_task(id,parent_id,type,u_id,role_id,status) VALUES "
                + "(1,1,'CC','[10,11]','[20]',1),(2,1,'CC','[10]','[20]',1),"
                + "(3,2,'CC','[12]','[]',1),(4,3,'CC','[10]','[]',1),(5,4,'CC','[10]','[]',1),"
                + "(6,6,'CC','[10]','[]',1),(7,2,'APPROVAL','[10]','[]',1),(8,2,'CC','[10]','[]',0),"
                + "(9,999,'CC','[10]','[]',1)");
        jdbc.execute("INSERT INTO sys_result(id,p_id,u_id,task_id,score,is_finish) VALUES "
                + "(1,1,10,'a',5,4),(2,1,10,'b',7,1),(3,1,11,'a',12,1),"
                + "(4,2,12,'a',NULL,1),(5,5,4,'a',9,1),(6,1,4,'a',11,1),(7,3,4,'a',999,1),(8,4,4,'a',999,1)");
        service = transactionalService(projects);
    }

    /**
     * 为实际服务入口创建注解事务代理
     *
     * @param mapper 本次使用的真实或观测Mapper
     * @return 使用同一数据库连接及REPEATABLE_READ的服务
     */
    private static ProjectService transactionalService(ProjectMapper mapper) {
        ProjectServiceImpl target = new ProjectServiceImpl();
        ReflectionTestUtils.setField(target, "projectMapper", mapper);
        ReflectionTestUtils.setField(target, "projectAssessScopeResolver", new ProjectAssessScopeResolver(roles));
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (ProjectService) proxy.getProxy();
    }

    /**
     * 构造认证链路的用户快照，不读取请求userId
     *
     * @param id 操作人ID
     * @return 认证用户
     */
    private static User operator(int id) {
        User user = new User(); user.setUserId(id); return user;
    }

    /**
     * 从顶层分页响应取得原有VO列表
     *
     * @param result 分页响应
     * @return 当前页列表
     */
    @SuppressWarnings("unchecked")
    private static List<ProjectVo> rows(ApiResult result) {
        return (List<ProjectVo>) result.get("list");
    }

    /**
     * 多角色、多节点和直接接收只展示一行，不放大分数或跨项目串人
     */
    @Test
    void deduplicatesRecipientsAndPreservesAggregateAndFirstStatus() {
        ApiResult result = service.getProjectAssessList(new ProjectQueryParam(), operator(1));
        assertEquals(3L, result.get("total")); assertEquals(3, rows(result).size());
        assertEquals(Arrays.asList(10,11,12), rows(result).stream().map(ProjectVo::getUserId).collect(Collectors.toList()));
        assertEquals(Arrays.asList(12,12,0), rows(result).stream().map(ProjectVo::getScore).collect(Collectors.toList()));
        assertEquals(Arrays.asList(0,1,1), rows(result).stream().map(ProjectVo::getStatus).collect(Collectors.toList()));
        assertEquals(Arrays.asList(1,1,2), rows(result).stream().map(ProjectVo::getProjectId).collect(Collectors.toList()));
        jdbc.update("DELETE FROM sys_result WHERE u_id=12");
        ProjectVo empty = rows(service.getProjectAssessList(new ProjectQueryParam(), operator(1))).get(2);
        assertEquals(0, empty.getStatus().intValue()); assertEquals(0, empty.getScore().intValue());
    }

    /**
     * 院长共享角色、主任共享权重2角色及真实管理员权重10均保持旧范围
     */
    @Test
    void keepsExistingWeightAndAuxiliaryRoleSemantics() {
        for (int id : Arrays.asList(2,3)) {
            ApiResult result = service.getProjectAssessList(new ProjectQueryParam(), operator(id));
            assertEquals(1L, result.get("total")); assertEquals(10, rows(result).get(0).getUserId().intValue());
        }
        assertEquals(0L, service.getProjectAssessList(new ProjectQueryParam(), operator(5)).get("total"));
        assertThrows(AuthPermissionException.class, () -> service.getProjectAssessList(new ProjectQueryParam(), operator(12)));
        assertEquals(0L, projects.countAssessList(new ProjectQueryParam(), null));
        assertEquals(0L, projects.countAssessList(new ProjectQueryParam(), new ProjectAssessScope(1, null)));
    }

    /**
     * 本人项目独立于当前CC名单，补齐项目、角色及删除状态筛选
     */
    @Test
    void selfBranchUsesResultsAndAllFiltersWithoutCcMembership() {
        ProjectQueryParam param = new ProjectQueryParam(); param.setUserId(10);
        assertEquals(Arrays.asList(1,5), rows(service.getProjectAssessList(param, operator(4))).stream()
                .map(ProjectVo::getProjectId).collect(Collectors.toList()));
        param.setProjectId(5); param.setRoleId(1); param.setNickName("teach"); param.setStatus(1);
        assertEquals(1L, service.getProjectAssessList(param, operator(4)).get("total"));
        param.setRoleId(10);
        assertEquals(0L, service.getProjectAssessList(param, operator(4)).get("total"));
    }

    /**
     * 字节敏感的字面昵称匹配、组合状态过滤及原有时间方向在分页前生效
     */
    @Test
    void appliesLiteralNicknameCombinedStatusAndCoveringTime() {
        ProjectQueryParam param = new ProjectQueryParam();
        for (String nick : Arrays.asList("%", "%_", "Ab", "中")) {
            param.setNickName(nick);
            long expected = "中".equals(nick) ? 2L : 1L;
            assertEquals(expected, service.getProjectAssessList(param, operator(1)).get("total"));
        }
        param.setNickName("ab"); assertEquals(11, rows(service.getProjectAssessList(param, operator(1))).get(0).getUserId().intValue());
        param.setNickName(""); assertEquals(2L, service.getProjectAssessList(param, operator(1)).get("total"));
        param.setNickName("' OR 1=1 --"); assertEquals(0L, service.getProjectAssessList(param, operator(1)).get("total"));
        param.setNickName(null); param.setProjectId(1); param.setRoleId(10); param.setStatus(0);
        param.setStartTime(Timestamp.valueOf("2026-03-01 00:00:00")); param.setEndTime(Timestamp.valueOf("2026-10-01 00:00:00"));
        assertEquals(1L, service.getProjectAssessList(param, operator(1)).get("total"));
        param.setStartTime(Timestamp.valueOf("2025-12-31 23:59:59"));
        assertEquals(0L, service.getProjectAssessList(param, operator(1)).get("total"));
    }

    /**
     * 静态同分结果跨页保持固定顺序，同一记录不因页大小变化而改变rank
     */
    @Test
    void stableTiesAcrossPagesAndPageSizes() {
        ProjectQueryParam param = new ProjectQueryParam(); param.setLimit(1);
        List<Integer> ids = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            param.setPage(page); ApiResult result = service.getProjectAssessList(param, operator(1));
            assertEquals(3L, result.get("totalPage")); assertEquals(page, rows(result).get(0).getRank().intValue());
            ids.add(rows(result).get(0).getUserId());
        }
        assertEquals(Arrays.asList(10,11,12), ids);
        param.setPage(1); param.setLimit(2);
        assertEquals(2, rows(service.getProjectAssessList(param, operator(1))).get(1).getRank().intValue());
        param.setPage(3);
        ApiResult empty = service.getProjectAssessList(param, operator(1));
        assertEquals(3L, empty.get("total")); assertTrue(rows(empty).isEmpty());
    }

    /**
     * 名单错误返回可定位节点，不被隐式替换为空页
     *
     * @param invalid 非法名单
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-json", "{}", "null", "[null]", "[true]", "[1.5]", "[{}]", "[[]]", "[2147483648]", "[\"x\"]", "[\"2147483648\"]"})
    void rejectsMalformedRecipientDocuments(String invalid) {
        jdbc.update("UPDATE sys_flow_task SET u_id=? WHERE id=1", invalid);
        ProjectTaskException failure = assertThrows(ProjectTaskException.class,
                () -> service.getProjectAssessList(new ProjectQueryParam(), operator(1)));
        assertEquals(500, failure.getStatus()); assertTrue(failure.getMessage().contains("ID：1"));
    }

    /**
     * 正常空数组、整数字符串及缺失用户关系按明确契约处理
     */
    @Test
    void acceptsIntegerStringsAndEmptyArraysWithoutInventingUsers() {
        jdbc.update("UPDATE sys_flow_task SET u_id='[\"10\",11,999]' WHERE id=1");
        assertNull(projects.findInvalidAssessRecipient(new ProjectQueryParam()));
        assertEquals(3L, service.getProjectAssessList(new ProjectQueryParam(), operator(1)).get("total"));
        jdbc.update("UPDATE sys_flow_task SET u_id='[]',role_id='[]' WHERE id IN (1,2)");
        assertEquals(1L, service.getProjectAssessList(new ProjectQueryParam(), operator(1)).get("total"));
    }

    /**
     * 已删除项目、失效流程、非CC节点及筛选外的非法名单不参与本次候选或审计
     */
    @Test
    void excludesInvalidDocumentsOutsideSelectedActiveCc() {
        jdbc.update("UPDATE sys_flow_task SET u_id='not-json' WHERE id IN (3,4,5,6,7,8,9)");
        ProjectQueryParam param = new ProjectQueryParam(); param.setProjectId(1);
        assertEquals(2L, service.getProjectAssessList(param, operator(1)).get("total"));
        param.setProjectId(null);
        assertTrue(assertThrows(ProjectTaskException.class, () -> service.getProjectAssessList(param, operator(1)))
                .getMessage().contains("ID：3"));
    }

    /**
     * 数据库SUM超出Integer后以Long接收，由服务受控拒绝
     */
    @Test
    void checksWideDatabaseSum() {
        jdbc.update("UPDATE sys_result SET score=2147483647 WHERE u_id=10");
        ProjectAssessScope scope = new ProjectAssessScope(1, ProjectAssessScope.Type.ALL_RECIPIENTS);
        assertEquals(4294967294L, projects.selectAssessListPage(new ProjectQueryParam(), scope, 0, 10).get(0).getTotalScore().longValue());
        assertEquals(500, assertThrows(ProjectTaskException.class,
                () -> service.getProjectAssessList(new ProjectQueryParam(), operator(1))).getStatus());
    }

    /**
     * COUNT后另一连接写入时，真实代理事务内数据页仍读取同一可重复读快照
     */
    @Test
    void countAndPageUseRepeatableReadSnapshot() {
        ProjectMapper observed = mock(ProjectMapper.class, delegatesTo(projects));
        doAnswer(invocation -> {
            long total = projects.countAssessList(invocation.getArgument(0), invocation.getArgument(1));
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,
                    TransactionSynchronizationManager.getCurrentTransactionIsolationLevel().intValue());
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM sys_flow_task WHERE id=3");
                statement.executeUpdate("UPDATE sys_result SET score=100 WHERE id=1");
            }
            return total;
        }).when(observed).countAssessList(any(), any());
        ApiResult result = transactionalService(observed).getProjectAssessList(new ProjectQueryParam(), operator(1));
        assertEquals(3L, result.get("total")); assertEquals(3, rows(result).size());
        assertEquals(12, rows(result).get(0).getScore().intValue());
        assertEquals(2L, service.getProjectAssessList(new ProjectQueryParam(), operator(1)).get("total"));
    }

    /**
     * 使用Mockito的委托答案，保留真实XML调用
     *
     * @param target 真实Mapper
     * @return 委托调用答案
     */
    private static Answer<Object> delegatesTo(Object target) {
        return AdditionalAnswers.delegatesTo(target);
    }

    /**
     * 最小化导入用户提供的快照，验证候选、状态、总分及真实账户分页基线
     * <p>
     * 不导入密码、邮箱、电话、佐证等无关字段，按SQL字面量解析后使用参数绑定批量写入。
     * </p>
     *
     * @throws Exception 快照读取或解析失败时抛出
     */
    @Test
    void verifiesSanitizedSnapshotAgainstIndependentLegacyOracle() throws Exception {
        importSanitizedSnapshot();
        ProjectQueryParam param = new ProjectQueryParam();
        ProjectAssessScope all = new ProjectAssessScope(1, ProjectAssessScope.Type.ALL_RECIPIENTS);
        assertNull(projects.findInvalidAssessRecipient(param));
        assertEquals(362L, projects.countAssessList(param, all));
        List<ProjectAssessRow> candidates = new ArrayList<>();
        for (int offset = 0; offset < 362; offset += 100) {
            candidates.addAll(projects.selectAssessListPage(param, all, offset, 100));
        }
        assertEquals(362, candidates.size());
        assertEquals(183L, candidates.stream().filter(r -> r.getStatus() == 1).count());
        assertEquals(179L, candidates.stream().filter(r -> r.getStatus() == 0).count());
        assertEquals(207L, candidates.stream().mapToLong(ProjectAssessRow::getTotalScore).max().getAsLong());
        for (int project : Arrays.asList(46,47,48,49)) {
            param.setProjectId(project);
            assertEquals(project < 48 ? 122L : 59L, projects.countAssessList(param, all));
        }
        param.setProjectId(null);
        for (int id : Arrays.asList(132,812)) {
            Map<String, Long> expected = legacySnapshotOracle(id);
            assertEquals(id == 132 ? 250 : 36, expected.size());
            Map<String, Long> actual = new HashMap<>();
            List<ProjectVo> ordered = new ArrayList<>();
            for (int page = 1; page <= (expected.size() + 9) / 10; page++) {
                param.setPage(page); ApiResult result = service.getProjectAssessList(param, operator(id));
                assertEquals((long) expected.size(), result.get("total"));
                assertEquals((long) (expected.size() + 9) / 10, result.get("totalPage"));
                for (ProjectVo row : rows(result)) {
                    assertEquals(ordered.size() + 1, row.getRank().intValue());
                    assertNull(actual.put(row.getProjectId() + ":" + row.getUserId(), row.getScore().longValue()));
                    ordered.add(row);
                }
            }
            assertEquals(expected, actual);
            Comparator<ProjectVo> comparator = Comparator.comparing(ProjectVo::getScore).reversed()
                    .thenComparing(ProjectVo::getProjectId).thenComparing(ProjectVo::getUserId);
            for (int i = 1; i < ordered.size(); i++) assertTrue(comparator.compare(ordered.get(i - 1), ordered.get(i)) <= 0);
        }
        for (int admin : Arrays.asList(4,1307)) {
            assertEquals(0L, service.getProjectAssessList(new ProjectQueryParam(), operator(admin)).get("total"));
        }
    }

    /**
     * 只按白名单列导入七张表，保留接收、角色、分数和状态关系
     *
     * @throws Exception 文件读取、SQL字面量解析或列数核对失败时抛出
     */
    private static void importSanitizedSnapshot() throws Exception {
        Path snapshot = root.resolve("soft_main/src/sql/all.sql");
        Assumptions.assumeTrue(Files.exists(snapshot), "缺少用户提供的all.sql，快照对照未执行");
        Map<String, String> selected = new LinkedHashMap<>();
        selected.put("sys_project", "id,title,create_name,start_time,end_time,status");
        selected.put("sys_flow", "id,p_id,user_id,status");
        selected.put("sys_flow_task", "id,parent_id,type,u_id,role_id,status");
        selected.put("sys_result", "id,u_id,p_id,task_id,score,is_finish");
        selected.put("sys_user", "user_id,nick_name,user_name,major,college,status");
        selected.put("sys_role", "role_id,role_name,role_code,role_status,weight");
        selected.put("sys_user_role", "id,user_id,role_id");
        List<String> lines = Files.readAllLines(snapshot, StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entry : selected.entrySet()) {
            String table = entry.getKey();
            Matcher ddl = Pattern.compile("CREATE TABLE `" + table + "`[\\s\\S]*?;(?=\\r?\\n)").matcher(structure);
            assertTrue(ddl.find());
            Matcher column = Pattern.compile("(?m)^\\s+`([^`]+)`").matcher(ddl.group());
            List<String> columns = new ArrayList<>(); while (column.find()) columns.add(column.group(1));
            List<String> wanted = Arrays.asList(entry.getValue().split(","));
            List<Object[]> batch = new ArrayList<>();
            String prefix = "INSERT INTO `" + table + "` VALUES (";
            for (String line : lines) {
                if (!line.startsWith(prefix)) continue;
                assertTrue(line.endsWith(");"), "只接受每行一条的快照INSERT");
                List<String> values = parseSqlLiterals(line.substring(prefix.length(), line.length() - 2));
                assertEquals(columns.size(), values.size(), table);
                Object[] row = new Object[wanted.size()];
                for (int i = 0; i < row.length; i++) {
                    int position = columns.indexOf(wanted.get(i)); assertTrue(position >= 0);
                    row[i] = values.get(position);
                }
                batch.add(row);
            }
            assertFalse(batch.isEmpty(), table);
            jdbc.execute("DELETE FROM " + table);
            String placeholders = String.join(",", Collections.nCopies(wanted.size(), "?"));
            jdbc.batchUpdate("INSERT INTO " + table + "(" + entry.getValue() + ") VALUES (" + placeholders + ")", batch);
        }
    }

    /**
     * 解析MySQL导出的一行值列表，保留逗号、引号及反斜杠转义的字面含义
     *
     * @param text 括号内的SQL字面量列表
     * @return 可供绑定的字符串或null列表
     */
    private static List<String> parseSqlLiterals(String text) {
        List<String> values = new ArrayList<>();
        int position = 0;
        while (position < text.length()) {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
            if (text.charAt(position) == '\'') {
                position++; StringBuilder value = new StringBuilder(); boolean closed = false;
                while (position < text.length()) {
                    char ch = text.charAt(position++);
                    if (ch == '\\') {
                        char escaped = text.charAt(position++);
                        switch (escaped) {
                            case 'n': value.append('\n'); break;
                            case 'r': value.append('\r'); break;
                            case 't': value.append('\t'); break;
                            case '0': value.append('\0'); break;
                            case 'b': value.append('\b'); break;
                            case 'Z': value.append((char) 26); break;
                            default: value.append(escaped);
                        }
                    } else if (ch == '\'') {
                        if (position < text.length() && text.charAt(position) == '\'') {
                            value.append('\''); position++;
                        } else { closed = true; break; }
                    } else value.append(ch);
                }
                assertTrue(closed); values.add(value.toString());
            } else {
                int begin = position;
                while (position < text.length() && text.charAt(position) != ',') position++;
                String value = text.substring(begin, position).trim();
                values.add("NULL".equalsIgnoreCase(value) ? null : value);
            }
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
            if (position < text.length()) { assertEquals(',', text.charAt(position)); position++; }
        }
        return values;
    }

    /**
     * 独立模拟旧院长分支，使用内存关系展开及逐明细求和对照新SQL
     *
     * @param operatorId 快照中真实院长用户ID
     * @return 按项目、用户标识索引的总分
     */
    private static Map<String, Long> legacySnapshotOracle(int operatorId) {
        Map<Integer, Integer> weights = new HashMap<>();
        jdbc.query("SELECT role_id,weight FROM sys_role", r -> { weights.put(r.getInt(1), r.getInt(2)); });
        Map<Integer, Set<Integer>> userRoles = new HashMap<>();
        Map<Integer, Set<Integer>> roleUsers = new HashMap<>();
        jdbc.query("SELECT ur.user_id,ur.role_id FROM sys_user_role ur JOIN sys_user u ON u.user_id=ur.user_id", r -> {
            int user = r.getInt(1), role = r.getInt(2);
            userRoles.computeIfAbsent(user, key -> new HashSet<>()).add(role);
            roleUsers.computeIfAbsent(role, key -> new HashSet<>()).add(user);
        });
        Set<Integer> shared = userRoles.get(operatorId).stream().filter(id -> weights.containsKey(id) && weights.get(id) != 1)
                .collect(Collectors.toSet());
        Map<String, Long> scores = new HashMap<>();
        jdbc.query("SELECT p_id,u_id,score FROM sys_result", r -> {
            String key = r.getInt(1) + ":" + r.getInt(2); scores.merge(key, r.getLong(3), Long::sum);
        });
        Map<String, Long> expected = new HashMap<>();
        jdbc.query("SELECT p.id,ft.u_id,ft.role_id FROM sys_project p JOIN sys_flow f ON f.p_id=p.id "
                + "JOIN sys_flow_task ft ON ft.parent_id=f.id WHERE p.status<>3 AND f.status=1 AND ft.status=1 AND ft.type='CC'", r -> {
            Set<Integer> recipients = new HashSet<>(JSON.parseArray(r.getString(2), Integer.class));
            for (Integer role : JSON.parseArray(r.getString(3), Integer.class)) {
                recipients.addAll(roleUsers.getOrDefault(role, Collections.emptySet()));
            }
            for (int recipient : recipients) {
                if (userRoles.getOrDefault(recipient, Collections.emptySet()).stream().anyMatch(shared::contains)) {
                    String key = r.getInt(1) + ":" + recipient; expected.put(key, scores.getOrDefault(key, 0L));
                }
            }
        });
        return expected;
    }

    /**
     * 生成扩大样本并记录页查询延迟、分配量、往返及执行计划
     * <p>
     * 通过PROJECT_ASSESS_PERF=true显式执行，避免常规CI承担较长性能采样。
     * 单线程本地数据只用于工程对照，不代替预发并发及数据库CPU验收。
     * </p>
     *
     * @throws Exception 数据生成、执行计划或结果写入失败时抛出
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "PROJECT_ASSESS_PERF", matches = "true")
    void measuresScaledPagesAndLegacyAllocation() throws Exception {
        ((Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME))
                .setLevel(Level.WARN);
        List<String> report = new ArrayList<>();
        report.add("users,results,indexed,new_p50_ms,new_p95_ms,new_p99_ms,new_alloc_bytes,new_sql,legacy_ms,legacy_alloc_bytes,legacy_sql");
        ThreadMXBean memory = (ThreadMXBean)
                ManagementFactory.getThreadMXBean();
        memory.setThreadAllocatedMemoryEnabled(true);
        for (int size : Arrays.asList(1000,10000,50000)) {
            if (size > 1000) executeIndexScript("t10-project-assess-indexes-rollback.sql");
            generateScaleFixture(size);
            ProjectQueryParam param = new ProjectQueryParam();
            writeExplain("t10-explain-" + size + "-unindexed.json", param);
            long beforeIndex = System.nanoTime();
            assertEquals((long) size * 2, service.getProjectAssessList(param, operator(1)).get("total"));
            System.out.println("T10 scale=" + size + " unindexed_ms=" + (System.nanoTime() - beforeIndex) / 1_000_000.0);
            executeIndexScript("t10-project-assess-indexes.sql");
            writeExplain("t10-explain-" + size + "-indexed.json", param);
            for (int warmup = 0; warmup < 3; warmup++) service.getProjectAssessList(param, operator(1));
            double[] samples = new double[20]; long allocated = 0;
            for (int run = 0; run < samples.length; run++) {
                observer.count = 0;
                long allocationStart = memory.getThreadAllocatedBytes(Thread.currentThread().getId());
                long start = System.nanoTime();
                ApiResult result = service.getProjectAssessList(param, operator(1));
                samples[run] = (System.nanoTime() - start) / 1_000_000.0;
                allocated += memory.getThreadAllocatedBytes(Thread.currentThread().getId()) - allocationStart;
                assertEquals(10, rows(result).size()); assertEquals(4L, observer.count);
                assertTrue(observer.lastPageSql.contains("LIMIT ?, ?"));
            }
            Arrays.sort(samples);
            double legacyMs = -1; long legacyAllocation = -1, legacySql = -1;
            if (size <= 10000) {
                observer.count = 0;
                long allocationStart = memory.getThreadAllocatedBytes(Thread.currentThread().getId());
                long start = System.nanoTime();
                List<ProjectVo> old = legacyLeaderPage();
                legacyMs = (System.nanoTime() - start) / 1_000_000.0;
                legacyAllocation = memory.getThreadAllocatedBytes(Thread.currentThread().getId()) - allocationStart;
                legacySql = observer.count;
                assertEquals(10, old.size());
                assertEquals(rows(service.getProjectAssessList(param, operator(1))), old);
            }
            for (int limit : Arrays.asList(10,100)) {
                param.setLimit(limit);
                for (int page : Arrays.asList(1, size / limit, size * 2 / limit, size * 2 / limit + 1)) {
                    param.setPage(page); observer.count = 0;
                    ApiResult result = service.getProjectAssessList(param, operator(1));
                    assertEquals((long) size * 2, result.get("total"));
                    assertTrue(rows(result).size() <= limit);
                    assertEquals(rows(result).isEmpty() ? 3L : 4L, observer.count);
                    if (!rows(result).isEmpty()) {
                        assertEquals((page - 1) * limit + 1, rows(result).get(0).getRank().intValue());
                    }
                }
            }
            String row = String.format(Locale.ROOT, "%d,%d,true,%.3f,%.3f,%.3f,%d,4,%.3f,%d,%d",
                    size, size * 8, samples[9], samples[18], samples[19], allocated / samples.length,
                    legacyMs, legacyAllocation, legacySql);
            report.add(row); System.out.println("T10 PERF " + row);
            Files.write(root.resolve("target/t10-performance.csv"), report, StandardCharsets.UTF_8);
        }
        executeIndexScript("t10-project-assess-indexes-rollback.sql");
    }

    /**
     * 在已经核验库名的隔离连接上执行实际交付的索引及回退脚本
     *
     * @param file 仓库SQL目录下的固定脚本名
     * @throws Exception 脚本读取或执行失败时抛出
     */
    private static void executeIndexScript(String file) throws Exception {
        String sql = new String(Files.readAllBytes(root.resolve("soft_main/src/sql/" + file)), StandardCharsets.UTF_8);
        for (String statement : sql.split(";")) {
            if (!statement.trim().isEmpty()) jdbc.execute(statement);
        }
    }

    /**
     * 生成两个项目、每人每项目四条结果的合成样本，仅写专用测试库
     *
     * @param size 合成教师数量
     */
    private static void generateScaleFixture(int size) {
        for (String table : TABLES) jdbc.execute("DELETE FROM " + table);
        jdbc.execute("INSERT INTO sys_role(role_id,role_name,role_code,role_status,weight) VALUES (2,'leader','leader',1,20),(20,'recipients','recipients',1,2)");
        jdbc.execute("INSERT INTO sys_user(user_id,nick_name,user_name,status) VALUES (1,'leader','leader',1)");
        jdbc.execute("INSERT INTO sys_user_role(user_id,role_id) VALUES (1,2)");
        jdbc.execute("INSERT INTO sys_project(id,title,create_name,status) VALUES (100,'scale1','fixture',1),(101,'scale2','fixture',1)");
        jdbc.execute("INSERT INTO sys_flow(id,p_id,user_id,status) VALUES (100,100,1,1),(101,101,1,1)");
        jdbc.execute("INSERT INTO sys_flow_task(id,parent_id,type,u_id,role_id,status) VALUES (100,100,'CC','[]','[20]',1),(101,101,'CC','[]','[20]',1)");
        for (int batchStart = 0; batchStart < size; batchStart += 500) {
            List<Object[]> users = new ArrayList<>(), links = new ArrayList<>(), results = new ArrayList<>();
            for (int i = batchStart; i < Math.min(size, batchStart + 500); i++) {
                int id = 1000 + i;
                users.add(new Object[]{id,"teacher" + id,"u" + id,1}); links.add(new Object[]{id,20});
                for (int project : Arrays.asList(100,101)) {
                    for (int task = 0; task < 4; task++) {
                        results.add(new Object[]{i * 8 + (project - 100) * 4 + task + 1, project, id,
                                "task" + task, i % 7 + task, 1});
                    }
                }
            }
            jdbc.batchUpdate("INSERT INTO sys_user(user_id,nick_name,user_name,status) VALUES (?,?,?,?)", users);
            jdbc.batchUpdate("INSERT INTO sys_user_role(user_id,role_id) VALUES (?,?)", links);
            jdbc.batchUpdate("INSERT INTO sys_result(id,p_id,u_id,task_id,score,is_finish) VALUES (?,?,?,?,?,?)", results);
        }
        jdbc.execute("ANALYZE TABLE sys_result,sys_user_role,sys_user");
    }

    /**
     * 对照旧领导分支的全量VO、逐人查库及排序分页；测试数据不包含过滤或重复节点
     *
     * @return 全量组装后截取的首页
     */
    private static List<ProjectVo> legacyLeaderPage() {
        TransactionTemplate transaction =
                new TransactionTemplate(transactions);
        transaction.setReadOnly(true); transaction.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
        return transaction.execute(status -> {
            roles.getRoleWeight(1);
            List<ProjectVo> list = new ArrayList<>();
            for (FlowTask flow : projects.getProjectAssessFlow(null,null,null)) {
                Set<Integer> ids = new HashSet<>(JSON.parseArray(flow.getUId(), Integer.class));
                for (int role : JSON.parseArray(flow.getRoleId(), Integer.class)) ids.addAll(roles.selectUserIdByRoleId(role));
                for (int id : ids) {
                    ProjectVo vo = new ProjectVo(); vo.setProjectId(flow.getProjectId());
                    Project project = projects.getProjectTime(flow.getProjectId());
                    vo.setStartTime(project.getStartTime()); vo.setEndTime(project.getEndTime());
                    vo.setProjectName(projects.getProjectName(flow.getProjectId()));
                    User user = legacyUsers.selectById(id);
                    Integer finish = legacyResults.findResultStatus(id, flow.getProjectId());
                    vo.setStatus(finish != null && finish == 1 ? 1 : 0);
                    vo.setUserId(id); vo.setMajor(user.getMajor()); vo.setNickName(user.getNickName()); vo.setUserName(user.getUserName());
                    Integer score = legacyResults.findTotalScore(id, flow.getProjectId()); vo.setScore(score == null ? 0 : score);
                    list.add(vo);
                }
            }
            list.sort(Comparator.comparing(ProjectVo::getScore).reversed().thenComparing(ProjectVo::getProjectId).thenComparing(ProjectVo::getUserId));
            for (int i = 0; i < list.size(); i++) list.get(i).setRank(i + 1);
            return new ArrayList<>(list.subList(0,10));
        });
    }

    /**
     * 对实际绑定的页查询生成JSON执行计划，避免把测试SQL改写成另一份查询
     *
     * @param file target目录下的结果文件名
     * @param param 当前页查询条件
     * @throws Exception SQL生成、执行计划或文件写入失败时抛出
     */
    private static void writeExplain(String file, ProjectQueryParam param) throws Exception {
        Map<String,Object> arguments = new HashMap<>(); arguments.put("param",param);
        arguments.put("scope",new ProjectAssessScope(1,ProjectAssessScope.Type.ALL_RECIPIENTS));
        arguments.put("offset",0L); arguments.put("limit",param.getLimit());
        MappedStatement statement = mappings.getMappedStatement(ProjectMapper.class.getName() + ".selectAssessListPage");
        BoundSql bound = statement.getBoundSql(arguments);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement explain = connection.prepareStatement("EXPLAIN FORMAT=JSON " + bound.getSql())) {
            new DefaultParameterHandler(statement,arguments,bound).setParameters(explain);
            try (ResultSet result = explain.executeQuery()) {
                assertTrue(result.next());
                Files.write(root.resolve("target/" + file), result.getString(1).getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 观察真实MyBatis语句准备次数及数据页SQL，不替换执行行为
     */
    @Intercepts(@Signature(type = StatementHandler.class,
            method = "prepare", args = {Connection.class, Integer.class}))
    public static class SqlObserver implements Interceptor {
        /**
         * 本次采样的实际语句准备次数
         */
        private long count;
        /**
         * 最近一次真实数据页SQL
         */
        private String lastPageSql;

        /**
         * 记录当前SQL后继续执行
         *
         * @param invocation 真实语句准备调用
         * @return 原调用结果
         * @throws Throwable 原SQL执行异常
         */
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            count++;
            String sql = ((StatementHandler) invocation.getTarget()).getBoundSql().getSql();
            if (sql.contains("SELECT paged.*")) lastPageSql = sql;
            return invocation.proceed();
        }

        /**
         * 为语句处理器添加观察代理
         *
         * @param target 语句处理器
         * @return 观察代理
         */
        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target,this);
        }

        /**
         * 接收插件配置，此观察器不需要属性
         *
         * @param properties 插件属性
         */
        @Override
        public void setProperties(Properties properties) {
        }
    }
}
