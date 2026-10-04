package com.hnkjzyxy.ab;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.hnkjzyxy.ab.mapper.CheckResultMapper;
import com.hnkjzyxy.ab.mapper.ConstructMapper;
import com.hnkjzyxy.ab.mapper.FlowMapper;
import com.hnkjzyxy.ab.mapper.ProjectTaskImportMapper;
import com.hnkjzyxy.ab.mapper.ResultMapper;
import com.hnkjzyxy.ab.mapper.SwitchRecordMapper;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.dto.ResultAccessScope;
import com.hnkjzyxy.ab.dto.ProjectAssessScope;
import com.hnkjzyxy.ab.model.Construct;
import com.hnkjzyxy.ab.model.SwitchRecord;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mapper SQL 迁移的兼容性验证
 * <p>
 * 使用真实 MyBatis-Plus 配置加载 XML，验证语句注册、参数绑定、动态 SQL、
 * 默认方法代理、主键回填及行锁缓存配置，无需连接数据库。
 * </p>
 */
class MapperXmlCompatibilityTest {

    private static Configuration configuration;
    private static final List<Class<?>> MAPPERS = new ArrayList<>();

    /**
     * 按项目配置加载所有 Mapper 和 XML 资源
     *
     * @throws Exception 资源加载或映射配置失败时抛出
     */
    @BeforeAll
    static void loadMappings() throws Exception {
        MybatisConfiguration mybatis = new MybatisConfiguration();
        mybatis.setMapUnderscoreToCamelCase(true);
        GlobalConfig global = new GlobalConfig();
        global.setBanner(false);
        GlobalConfig.DbConfig db = new GlobalConfig.DbConfig();
        db.setTablePrefix("sys_");
        db.setIdType(IdType.AUTO);
        db.setLogicDeleteValue("0");
        db.setLogicNotDeleteValue("1");
        global.setDbConfig(db);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(mock(DataSource.class));
        factory.setConfiguration(mybatis);
        factory.setGlobalConfig(global);
        factory.setMapperLocations(resolver.getResources("classpath*:mapper/*.xml"));
        configuration = factory.getObject().getConfiguration();
        for (Resource resource : resolver.getResources("classpath*:com/hnkjzyxy/ab/mapper/*Mapper.class")) {
            String name = resource.getFilename().replace(".class", "");
            Class<?> mapper = Class.forName("com.hnkjzyxy.ab.mapper." + name);
            MAPPERS.add(mapper);
            if (!configuration.hasMapper(mapper)) {
                configuration.addMapper(mapper);
            }
        }
    }

    /**
     * 验证自定义接口均有 XML 实现，且不再声明 SQL 和结果映射注解
     */
    @Test
    void registersEveryCustomStatementWithoutSqlAnnotations() {
        assertFalse(MAPPERS.isEmpty());
        List<String> forbidden = Arrays.asList("Select", "Insert", "Update", "Delete",
                "SelectProvider", "InsertProvider", "UpdateProvider", "DeleteProvider",
                "Options", "Results", "ResultMap");
        for (Class<?> mapper : MAPPERS) {
            for (Method method : mapper.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                for (Annotation annotation : method.getAnnotations()) {
                    assertFalse(forbidden.contains(annotation.annotationType().getSimpleName()), method.toString());
                }
                if (!method.isDefault()) {
                    assertTrue(configuration.hasStatement(mapper.getName() + "." + method.getName()), method.toString());
                }
            }
        }
    }

    /**
     * 使用实际接口参数解析器验证全部 XML 方法可生成并绑定 JDBC 参数
     *
     * @throws Exception 参数构造或 JDBC 绑定失败时抛出
     */
    @Test
    void bindsAllXmlMethodParameters() throws Exception {
        for (Class<?> mapper : MAPPERS) {
            for (Method method : mapper.getDeclaredMethods()) {
                if (method.isDefault() || method.isSynthetic()) {
                    continue;
                }
                Object parameters = new ParamNameResolver(configuration, method).getNamedParams(arguments(method));
                MappedStatement statement = configuration.getMappedStatement(mapper.getName() + "." + method.getName());
                BoundSql bound = statement.getBoundSql(parameters);
                assertFalse(bound.getSql().trim().isEmpty(), method.toString());
                configuration.newParameterHandler(statement, parameters, bound).setParameters(mock(PreparedStatement.class));
            }
        }
    }

    /**
     * 通过真实 Mapper 代理调用 MyBatis-Plus 默认方法并验证其生成 SQL 可绑定
     *
     * @throws Exception 默认方法代理调用失败时抛出
     */
    @Test
    void invokesMybatisPlusDefaultsThroughMapperProxy() throws Exception {
        List<String> executedSql = new ArrayList<>();
        SqlSession session = mock(SqlSession.class, invocation -> {
            if ("getConfiguration".equals(invocation.getMethod().getName())) {
                return configuration;
            }
            Object[] args = invocation.getArguments();
            if (args.length > 0 && args[0] instanceof String && configuration.hasStatement((String) args[0])) {
                MappedStatement statement = configuration.getMappedStatement((String) args[0]);
                Object parameters = args.length > 1 ? args[1] : null;
                BoundSql bound = statement.getBoundSql(parameters);
                executedSql.add(bound.getSql().replaceAll("\\s+", " ").trim());
                configuration.newParameterHandler(statement, parameters, bound).setParameters(mock(PreparedStatement.class));
                if (((String) args[0]).endsWith(".selectCount")) {
                    return 1;
                }
                if ("selectList".equals(invocation.getMethod().getName())) {
                    return Collections.emptyList();
                }
                if ("update".equals(invocation.getMethod().getName()) || "delete".equals(invocation.getMethod().getName())) {
                    return 1;
                }
            }
            return null;
        });
        int invoked = 0;
        for (Class<?> mapper : MAPPERS) {
            Object proxy = configuration.getMapper(mapper, session);
            for (Method method : mapper.getDeclaredMethods()) {
                if (method.isDefault() && !method.isSynthetic()) {
                    executedSql.clear();
                    method.invoke(proxy, arguments(method));
                    assertFalse(executedSql.isEmpty(), method.toString());
                    String generated = executedSql.get(0).replace(" ", "");
                    if ("selectMajorDetails".equals(method.getName()) || "getStudentInfoByStudentId".equals(method.getName())) {
                        assertTrue(generated.contains("del_flag=1"), method.toString());
                    }
                    if ("findFlowByPId".equals(method.getName()) || "getNoticeById".equals(method.getName())
                            || "getNoticeTotal".equals(method.getName())) {
                        assertTrue(generated.contains("status=1"), method.toString());
                    }
                    if ("deleteAll".equals(method.getName())) {
                        assertEquals("DELETEFROMsys_course_schedule", generated);
                    }
                    invoked++;
                }
            }
        }
        assertTrue(invoked > 0);
        verify(session, atLeastOnce()).delete(anyString(), any());
        verify(session, atLeastOnce()).update(anyString(), any());
    }

    /**
     * 验证建设项目参与人写入与用户菜单查询均使用占位符绑定参数
     */
    @Test
    void parameterizesAssociationInsertAndMenuLookup() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("conId", 7);
        parameters.put("uIds", "1,2,3");
        BoundSql insert = statement(ConstructMapper.class, "addConstructByUId").getBoundSql(parameters);
        assertEquals(2, insert.getParameterMappings().size());
        assertFalse(insert.getSql().contains("1,2,3"));
        parameters.put("userId", 9);
        BoundSql menu = statement(UserMapper.class, "getNavMenu").getBoundSql(parameters);
        assertEquals(1, menu.getParameterMappings().size());
        assertTrue(menu.getSql().contains("?"));
    }

    /**
     * 验证集合查询按元素类型映射，并由真实 Mapper 代理转换为去重的 HashSet
     */
    @Test
    void mapsScalarRowsIntoDeclaredHashSets() {
        SqlSession session = mock(SqlSession.class);
        when(session.getConfiguration()).thenReturn(configuration);
        when(session.selectList(anyString(), any())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return name.startsWith(ConstructMapper.class.getName())
                    ? Arrays.asList("2026", "2025", "2026") : Arrays.asList(7, 9, 7);
        });
        ConstructMapper constructs = configuration.getMapper(ConstructMapper.class, session);
        HashSet<String> years = new HashSet<>(Arrays.asList("2026", "2025"));
        assertEquals(years, constructs.getConstructYears(7));
        assertEquals(years, constructs.getConstructYearsByUser(new QueryWrapper<Construct>().eq("u_id", 7)));
        assertEquals(String.class, statement(ConstructMapper.class, "getConstructYears").getResultMaps().get(0).getType());
        assertEquals(String.class, statement(ConstructMapper.class, "getConstructYearsByUser").getResultMaps().get(0).getType());

        ResultMapper results = configuration.getMapper(ResultMapper.class, session);
        HashSet<Integer> userIds = new HashSet<>(Arrays.asList(7, 9));
        assertEquals(userIds, results.getUserIdByRole(1, 7, 2));
        assertEquals(userIds, results.selectUserIdByStep(7, 2));
        assertEquals(Integer.class, statement(ResultMapper.class, "getUserIdByRole").getResultMaps().get(0).getType());
        assertEquals(Integer.class, statement(ResultMapper.class, "selectUserIdByStep").getResultMaps().get(0).getType());

        when(session.selectList(anyString(), any())).thenReturn(Collections.emptyList());
        assertTrue(constructs.getConstructYears(7).isEmpty());
        assertTrue(constructs.getConstructYearsByUser(new QueryWrapper<>()).isEmpty());
        assertTrue(results.getUserIdByRole(1, 7, 2).isEmpty());
        assertTrue(results.selectUserIdByStep(7, 2).isEmpty());
    }

    /**
     * 验证用户为空时年度查询省略 WHERE，指定用户时绑定过滤条件
     */
    @Test
    void handlesOptionalFlowYearFilter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", null);
        String all = sql(FlowMapper.class, "getFlowYears", parameters);
        assertFalse(all.contains(" WHERE "));
        parameters.put("userId", 7);
        String filtered = sql(FlowMapper.class, "getFlowYears", parameters);
        assertTrue(filtered.contains("WHERE user_id = ?"));
    }

    /**
     * 验证学院集合展开、可选条件及排序白名单的动态 SQL 行为
     */
    @Test
    void expandsDynamicCollectionsAndWhitelistsSortDirection() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", "2026-01-01");
        parameters.put("endDate", "2026-12-31");
        parameters.put("colleges", Collections.emptyList());
        assertFalse(sql(CheckResultMapper.class, "getCollegeStatistics", parameters).contains("college IN"));
        parameters.put("colleges", Arrays.asList("软件学院", "艺术学院"));
        BoundSql bound = statement(CheckResultMapper.class, "getCollegeStatistics").getBoundSql(parameters);
        assertEquals(4, bound.getParameterMappings().size());
        assertTrue(bound.hasAdditionalParameter("__frch_college_0"));
        assertTrue(bound.hasAdditionalParameter("__frch_college_1"));
        parameters.put("order", "asc");
        assertTrue(sql(CheckResultMapper.class, "sortedByTime", parameters).endsWith("ORDER BY date ASC"));
        parameters.put("order", "asc; DROP TABLE sys_check_result");
        assertTrue(sql(CheckResultMapper.class, "sortedByTime", parameters).endsWith("ORDER BY date DESC"));
    }

    /**
     * 验证 XML 迁移保留自增主键回填、开关记录映射及项目行锁缓存刷新
     */
    @Test
    void preservesGeneratedKeysResultMappingAndLockCachePolicy() {
        MappedStatement insert = statement(SwitchRecordMapper.class, "insert");
        assertArrayEquals(new String[]{"switchId"}, insert.getKeyProperties());
        assertArrayEquals(new String[]{"switch_id"}, insert.getKeyColumns());
        assertEquals(SwitchRecord.class, statement(SwitchRecordMapper.class, "selectById").getResultMaps().get(0).getType());
        assertEquals(5, statement(SwitchRecordMapper.class, "selectById").getResultMaps().get(0).getResultMappings().size());
        MappedStatement lock = statement(ProjectTaskImportMapper.class, "lockProject");
        assertTrue(lock.isFlushCacheRequired());
        assertTrue(lock.getBoundSql(Collections.singletonMap("id", 7)).getSql().endsWith("FOR UPDATE"));
    }

    /**
     * 验证原有结果统计 XML 的项目参数、实体参数类型和关联字段保持有效
     *
     * @throws Exception JDBC 参数绑定失败时抛出
     */
    @Test
    void bindsResultFiltersAndUsesActualTaskAndUserKeys() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("pId", 7);
        parameters.put("uId", 9);
        parameters.put("year", "2026");
        assertEquals(3, statement(ResultMapper.class, "getPIdByRadar").getBoundSql(parameters).getParameterMappings().size());
        parameters.put("pId", null);
        assertEquals(2, statement(ResultMapper.class, "getPIdByRadar").getBoundSql(parameters).getParameterMappings().size());
        parameters.put("pId", 7);
        parameters.put("category", "教学");
        String statistics = sql(ResultMapper.class, "selectScoreByDepartmentAndCollege", parameters);
        assertTrue(statistics.contains("re.u_id = u.user_id"));
        assertTrue(statistics.contains("where re.p_id = ?"));
        assertTrue(sql(ResultMapper.class, "getUsersSubTaskScore", parameters).contains("a.task_id=c.id"));

        SubTaskIdDto dto = new SubTaskIdDto();
        dto.setProjectId("7");
        dto.setUserId("9");
        dto.setLimitScore(5);
        MappedStatement statement = statement(ResultMapper.class, "getUsersSubTaskScoreById");
        // 使用与真实Mapper签名一致的命名参数；全学院范围不追加学院占位符，保留筛选参数数量断言。
        parameters.put("dto", dto);
        parameters.put("scope", ResultAccessScope.all(7));
        BoundSql bound = statement.getBoundSql(parameters);
        assertEquals(3, bound.getParameterMappings().size());
        configuration.newParameterHandler(statement, parameters, bound).setParameters(mock(PreparedStatement.class));
    }

    /**
     * 读取指定 Mapper 方法的映射语句
     *
     * @param mapper Mapper 接口类型
     * @param name 方法名称
     * @return 已注册的映射语句
     */
    private static MappedStatement statement(Class<?> mapper, String name) {
        return configuration.getMappedStatement(mapper.getName() + "." + name);
    }

    /**
     * 生成便于断言的 SQL，将连续空白折叠为单个空格
     *
     * @param mapper Mapper 接口类型
     * @param name 方法名称
     * @param parameters 方法参数
     * @return 规范化后的 SQL
     */
    private static String sql(Class<?> mapper, String name, Object parameters) {
        return statement(mapper, name).getBoundSql(parameters).getSql().replaceAll("\\s+", " ").trim();
    }

    /**
     * 根据接口方法签名构造代表性参数
     *
     * @param method 待验证的方法
     * @return 与方法参数类型一致的参数数组
     * @throws Exception 参数对象无法构造时抛出
     */
    private static Object[] arguments(Method method) throws Exception {
        Object[] arguments = new Object[method.getParameterCount()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = sample(method.getParameterTypes()[i], method.getGenericParameterTypes()[i]);
        }
        return arguments;
    }

    /**
     * 构造标量、集合、查询条件或实体的测试值
     *
     * @param type 参数的原始类型
     * @param generic 参数的泛型类型
     * @return 可用于 SQL 生成及参数绑定的测试值
     * @throws Exception 实体无法实例化时抛出
     */
    private static Object sample(Class<?> type, Type generic) throws Exception {
        // 范围对象仅允许工厂构造，通用参数绑定检查不能再尝试调用无参构造方法。
        if (type == ResultAccessScope.class) return ResultAccessScope.college(7, "2026");
        if (type == ProjectAssessScope.class) return new ProjectAssessScope(7, ProjectAssessScope.Type.ALL_RECIPIENTS);
        if (type == String.class) return "2026";
        if (type == Integer.class || type == int.class) return 7;
        if (type == Long.class || type == long.class) return 7L;
        if (type == Date.class) return new Date(0);
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 1, 1, 0, 0);
        if (QueryWrapper.class.isAssignableFrom(type)) return new QueryWrapper<>().eq("id", 7);
        if (List.class.isAssignableFrom(type) || HashSet.class.isAssignableFrom(type)) {
            Type itemType = ((ParameterizedType) generic).getActualTypeArguments()[0];
            Object item = sample((Class<?>) itemType, itemType);
            return HashSet.class.isAssignableFrom(type)
                    ? new HashSet<>(Collections.singleton(item)) : Collections.singletonList(item);
        }
        return type.getDeclaredConstructor().newInstance();
    }
}
