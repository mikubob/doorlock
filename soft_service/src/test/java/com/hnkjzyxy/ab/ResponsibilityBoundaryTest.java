package com.hnkjzyxy.ab;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnkjzyxy.ab.dto.DiscoveredLockDevice;
import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectResultSubmitParam;
import com.hnkjzyxy.ab.service.ProjectTaskGuard;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.gateway.SmartLockDiscoveryGateway;
import com.hnkjzyxy.ab.service.impl.ProjectServiceImpl;
import com.hnkjzyxy.ab.service.support.SmartLockDiscoveryService;
import com.hnkjzyxy.ab.vo.ResultVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 职责拆分回归测试，验证请求缓存兼容及门禁设备登记规则
 */
class ResponsibilityBoundaryTest {
    /**
     * 验证提交参数与返回对象的 JSON 兼容性及输入校验边界
     *
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Test
    void resultRequestAndResponseKeepJsonFieldsButOnlyRequestRequiresProjectId() throws Exception {
        String payload = "{\"uid\":9,\"projectId\":7,\"evidence\":\"材料\",\"step\":1,"
                + "\"score\":\"10\",\"opinion\":\"意见\",\"isFlag\":0,\"userName\":\"1001\","
                + "\"nickName\":\"教师\",\"results\":[]}";
        ObjectMapper mapper = new ObjectMapper();
        ProjectResultSubmitParam request = mapper.readValue(payload, ProjectResultSubmitParam.class);
        ResultVo response = mapper.readValue(payload, ResultVo.class);
        assertEquals(mapper.valueToTree(response), mapper.valueToTree(request));
        assertEquals(9, request.getUId());
        ResultVo cached = JSON.parseObject(JSON.toJSONString(request), ResultVo.class);
        assertEquals(mapper.valueToTree(response), mapper.valueToTree(cached));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
            request.setProjectId(null);
            assertEquals("projectId", factory.getValidator().validate(request).iterator().next()
                    .getPropertyPath().toString());
            assertTrue(factory.getValidator().validate(new ResultVo()).isEmpty());
        }
    }

    /**
     * 验证旧暂存缓存可读取且新参数保留原缓存键和有效期
     */
    @Test
    void stagingStillReadsOldVoJsonAndWritesNewRequestWithSameKeyAndTtl() {
        RedisTemplate redis = mock(RedisTemplate.class);
        ValueOperations values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ProjectTaskGuard guard = mock(ProjectTaskGuard.class);
        ProjectServiceImpl service = new ProjectServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "projectTaskGuard", guard);
        User user = new User();
        user.setUserId(9);
        user.setUserName("1001");
        ProjectResultSubmitParam request = new ProjectResultSubmitParam();
        request.setProjectId(7);
        request.setResults(Collections.emptyList());
        service.projectStaging(request, user);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(guard).recordStaging(7, user, request.getResults());
        verify(values).set(eq("1001-7"), json.capture(), eq(15L), eq(TimeUnit.DAYS));
        when(redis.hasKey("1001-7")).thenReturn(true);
        when(values.get("1001-7")).thenReturn(json.getValue());
        assertEquals(7, service.getProjectStaging(user, 7).getProjectId());
        ResultVo oldCached = new ResultVo(9, 7, "材料", 1, "10", "意见", 0,
                "1001", "教师", Collections.emptyList());
        when(values.get("1001-7")).thenReturn(JSON.toJSONString(oldCached));
        ResultVo restored = service.getProjectStaging(user, 7);
        assertEquals(9, restored.getUId());
        assertEquals("材料", restored.getEvidence());
    }

    /**
     * 验证已存在SN被跳过且每个新设备仅登记一次
     */
    @Test
    void registrationSkipsKnownSnAndInsertsEachNewDeviceOnlyOnce() {
        SmartLockDiscoveryGateway gateway = mock(SmartLockDiscoveryGateway.class);
        SmartLockService persistence = mock(SmartLockService.class);
        LockInfo known = new LockInfo();
        known.setSnCode("known");
        when(persistence.getAll()).thenReturn(Collections.singletonList(known));
        when(gateway.discoveredDevices()).thenReturn(Arrays.asList(
                new DiscoveredLockDevice("known", "10.0.0.1", 8101),
                new DiscoveredLockDevice("new-a", "10.0.0.2", 8102),
                new DiscoveredLockDevice("new-a", "10.0.0.2", 8102),
                new DiscoveredLockDevice("new-b", "10.0.0.3", 8103)));
        when(persistence.add(any())).thenReturn(true);
        SmartLockDiscoveryService service = new SmartLockDiscoveryService(gateway, persistence);
        service.startDiscovery();
        verify(gateway).startDiscovery();
        assertTrue(service.registerDiscoveredDevices());
        ArgumentCaptor<LockInfo> saved = ArgumentCaptor.forClass(LockInfo.class);
        verify(persistence, times(2)).add(saved.capture());
        List<LockInfo> locks = saved.getAllValues();
        assertEquals("new-a", locks.get(0).getSnCode());
        assertEquals("10.0.0.2", locks.get(0).getIpAddress());
        assertEquals(8102, locks.get(0).getPortNumber());
        assertEquals("new-b", locks.get(1).getSnCode());
    }

    /**
     * 验证没有发现设备时不访问数据库
     */
    @Test
    void emptyDiscoveryDoesNotReadOrWriteTheDatabase() {
        SmartLockDiscoveryGateway gateway = mock(SmartLockDiscoveryGateway.class);
        SmartLockService persistence = mock(SmartLockService.class);
        when(gateway.discoveredDevices()).thenReturn(Collections.emptyList());
        assertFalse(new SmartLockDiscoveryService(gateway, persistence).registerDiscoveredDevices());
        verifyNoInteractions(persistence);
    }

    /**
     * 验证登记失败不会对重复快照条目再次执行新增
     */
    @Test
    void failedRegistrationReturnsFailureAndDoesNotRetryDuplicateSnapshotEntries() {
        SmartLockDiscoveryGateway gateway = mock(SmartLockDiscoveryGateway.class);
        SmartLockService persistence = mock(SmartLockService.class);
        when(persistence.getAll()).thenReturn(Collections.emptyList());
        DiscoveredLockDevice device = new DiscoveredLockDevice("new", "10.0.0.2", 8102);
        when(gateway.discoveredDevices()).thenReturn(Arrays.asList(device, device));
        when(persistence.add(any())).thenReturn(false);
        assertFalse(new SmartLockDiscoveryService(gateway, persistence).registerDiscoveredDevices());
        verify(persistence).add(any());
    }
}
