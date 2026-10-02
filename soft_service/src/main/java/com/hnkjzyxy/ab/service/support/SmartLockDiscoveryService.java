package com.hnkjzyxy.ab.service.support;

import com.hnkjzyxy.ab.dto.DiscoveredLockDevice;
import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.gateway.SmartLockDiscoveryGateway;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 门禁设备发现业务服务，启动搜索并按 SN 登记新设备
 */
@Service
public class SmartLockDiscoveryService {
    /**
     * 门禁设备发现接口
     */
    private final SmartLockDiscoveryGateway gateway;
    /**
     * 智能门锁业务服务
     */
    private final SmartLockService smartLockService;

    /**
     * 初始化门禁设备发现业务服务
     *
     * @param gateway 局域网门禁设备发现接口
     * @param smartLockService 门禁设备查询及持久化服务
     */
    public SmartLockDiscoveryService(SmartLockDiscoveryGateway gateway, SmartLockService smartLockService) {
        this.gateway = gateway;
        this.smartLockService = smartLockService;
    }

    /**
     * 通过设备发现接口启动局域网搜索
     * <p>
     * 本方法仅启动通讯搜索，不登记设备；登记由 registerDiscoveredDevices 执行。
     * </p>
     */
    public void startDiscovery() {
        gateway.startDiscovery();
    }

    /**
     * 将已发现的新门禁设备登记到数据库
     * <p>
     * 按数据库已有 SN 及本轮快照中的 SN 去重，每个新 SN 本轮只调用一次新增操作。
     * 未开始搜索或发现结果为空时直接返回 false；本方法不等待搜索完成。
     * </p>
     *
     * @return 至少一个新设备成功登记时返回 true，否则返回 false
     */
    public boolean registerDiscoveredDevices() {
        List<DiscoveredLockDevice> devices = gateway.discoveredDevices();
        if (devices.isEmpty()) {
            return false;
        }
        Set<String> knownSerials = new HashSet<>();
        for (LockInfo lock : smartLockService.getAll()) {
            if (lock != null && lock.getSnCode() != null) {
                knownSerials.add(lock.getSnCode());
            }
        }
        boolean added = false;
        for (DiscoveredLockDevice device : devices) {
            if (!knownSerials.add(device.getSn())) {
                continue;
            }
            LockInfo lock = new LockInfo();
            lock.setIpAddress(device.getIpAddress());
            lock.setPortNumber(device.getPort());
            lock.setSnCode(device.getSn());
            if (smartLockService.add(lock)) {
                added = true;
            }
        }
        return added;
    }
}
