package com.hnkjzyxy.ab;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.params.ProjectItemImportParam;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.params.UserEditParam;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.excel.TaskExcelImportService;
import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import com.hnkjzyxy.ab.service.support.SmartLockStateService;
import com.hnkjzyxy.ab.vo.ProjectItemVo;
import com.hnkjzyxy.ab.vo.UserVo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 验证跨模块资源、请求模型、事务及异步状态回写的行为。 */
class StructureRefactorCompatibilityTest {
    /**
     * 验证请求类型兼容旧 JSON 且输出模型不承担输入校验
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void requestModelsAcceptExistingPayloadsAndKeepOutputModelsIndependent() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        UserEditParam edit = mapper.readValue("{\"userName\":\"1001\",\"email\":\"user@example.com\","
                + "\"phone\":\"13800138000\",\"userId\":99,\"weight\":100}", UserEditParam.class);
        assertEquals("1001", edit.getUserName());
        assertEquals("user@example.com", edit.getEmail());
        try (ValidatorFactory validators = Validation.buildDefaultValidatorFactory()) {
            assertTrue(validators.getValidator().validate(edit).isEmpty());
            edit.setEmail("invalid");
            assertFalse(validators.getValidator().validate(edit).isEmpty());
            assertTrue(validators.getValidator().validate(new UserVo()).isEmpty());
        }
        String itemJson = "{\"id\":8,\"projectId\":7,\"parentId\":1,\"category\":\"分类\","
                + "\"pname\":\"任务\",\"standard\":\"标准\",\"grade\":2,\"score\":10,"
                + "\"isFile\":1,\"isExtend\":0,\"remark\":null}";
        ProjectItemSaveParam save = mapper.readValue(itemJson, ProjectItemSaveParam.class);
        ProjectItemVo output = mapper.readValue(itemJson, ProjectItemVo.class);
        assertEquals(mapper.valueToTree(output), mapper.valueToTree(save));
        ProjectItemImportParam selected = mapper.readValue(itemJson, ProjectItemImportParam.class);
        assertEquals(8, selected.getId());
        assertEquals(7, selected.getProjectId());
        assertEquals(2, mapper.valueToTree(selected).size());
    }

    /**
     * 验证 Mapper 模块资源发现及映射语句解析
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void mapperXmlResourcesLoadFromMapperModuleAndResolveStatements() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml");
        assertTrue(resources.length >= 7);
        MybatisConfiguration configuration = new MybatisConfiguration();
        Set<String> filenames = new HashSet<>();
        for (Resource resource : resources) {
            filenames.add(resource.getFilename());
            try (InputStream input = resource.getInputStream()) {
                new XMLMapperBuilder(input, configuration, resource.toString(),
                        configuration.getSqlFragments()).parse();
            }
        }
        assertTrue(filenames.contains("CheckResultMapper.xml"));
        assertTrue(configuration.hasStatement("com.hnkjzyxy.ab.mapper.CheckResultMapper.selectListByClasses"));
        assertTrue(configuration.hasStatement("com.hnkjzyxy.ab.mapper.TaskMapper.selectGroupMetrics"));
        assertTrue(configuration.hasStatement("com.hnkjzyxy.ab.mapper.ProjectMapper.getProjectItemById"));
    }

    /**
     * 验证任务 Excel 导入保留隔离级别及异常回滚规则
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void taskExcelImportStillRollsBackAtTheOriginalTransactionBoundary() throws Exception {
        AnnotationTransactionAttributeSource source = new AnnotationTransactionAttributeSource();
        TransactionAttribute transaction = source.getTransactionAttribute(
                TaskExcelImportService.class.getMethod("readTaskExcel",
                        org.springframework.web.multipart.MultipartFile.class, Integer.class),
                TaskExcelImportService.class);
        assertNotNull(transaction);
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, transaction.getIsolationLevel());
        assertTrue(transaction.rollbackOn(new Exception("解析失败")));
        ProjectTaskGuard guard = mock(ProjectTaskGuard.class);
        when(guard.lock(7)).thenThrow(new IllegalStateException("项目被冻结"));
        TaskExcelImportService target = new TaskExcelImportService();
        ReflectionTestUtils.setField(target, "projectTaskGuard", guard);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(manager, source));
        TaskExcelImportService proxy = (TaskExcelImportService) factory.getProxy();
        assertThrows(IllegalStateException.class, () -> proxy.readTaskExcel(
                new MockMultipartFile("file", "tasks.xlsx", "application/octet-stream", new byte[]{1}), 7));
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
    }

    /**
     * 验证设备查询结果相互隔离且通讯完成后才回写
     */
    @Test
    void doorQueriesKeepTheirOwnResultsAndPersistOnlyAfterCommunicationCompletes() {
        SmartLockGateway gateway = mock(SmartLockGateway.class);
        SmartLockService persistence = mock(SmartLockService.class);
        CompletableFuture<Integer> firstReply = new CompletableFuture<>();
        CompletableFuture<Integer> secondReply = new CompletableFuture<>();
        when(gateway.queryDoorStatus("10.0.0.1", 8000, "first")).thenReturn(firstReply);
        when(gateway.queryDoorStatus("10.0.0.2", 8000, "second")).thenReturn(secondReply);
        SmartLockStateService service = new SmartLockStateService(gateway, persistence);
        LockInfo first = lock(1, "10.0.0.1", "first");
        LockInfo second = lock(2, "10.0.0.2", "second");
        CompletableFuture<LockInfo> firstResult = service.refreshStatus(first);
        CompletableFuture<LockInfo> secondResult = service.refreshStatus(second);
        assertFalse(firstResult.isDone());
        verifyNoInteractions(persistence);
        secondReply.complete(0);
        assertSame(second, secondResult.join());
        assertFalse(firstResult.isDone());
        firstReply.complete(1);
        assertSame(first, firstResult.join());
        assertEquals(1, first.getSwitchStatus());
        assertEquals(0, second.getSwitchStatus());
        verify(persistence).updateSwitchStatus(1, 1);
        verify(persistence).updateSwitchStatus(2, 0);
    }

    /**
     * 验证设备通讯失败时不写入数据库状态
     */
    @Test
    void failedDoorQueriesDoNotWriteDatabaseState() {
        SmartLockGateway gateway = mock(SmartLockGateway.class);
        SmartLockService persistence = mock(SmartLockService.class);
        CompletableFuture<Integer> reply = new CompletableFuture<>();
        when(gateway.queryDoorStatus("10.0.0.1", 8000, "first")).thenReturn(reply);
        CompletableFuture<LockInfo> result = new SmartLockStateService(gateway, persistence)
                .refreshStatus(lock(1, "10.0.0.1", "first"));
        reply.completeExceptionally(new IllegalStateException("超时"));
        assertThrows(CompletionException.class, result::join);
        verifyNoInteractions(persistence);
    }

    /**
     * 创建用于异步状态查询验证的门禁设备对象
     *
     * @param id 包结构调整兼容性测试，验证资源加载、事务及异步状态回写ID
     * @param ip IP地址
     * @param sn 门禁设备SN
     * @return 智能门锁设备信息
     */
    private LockInfo lock(int id, String ip, String sn) {
        LockInfo lock = new LockInfo();
        lock.setLockId(id);
        lock.setIpAddress(ip);
        lock.setSnCode(sn);
        lock.setPortNumber(8000);
        return lock;
    }
}
