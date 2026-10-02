package com.hnkjzyxy.ab.service.support;

import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

/** 查询设备状态并在通讯完成后持久化；每次查询独立持有结果。 */
@Service
public class SmartLockStateService {
    private final SmartLockGateway gateway;
    private final SmartLockService smartLockService;

    public SmartLockStateService(SmartLockGateway gateway, SmartLockService smartLockService) {
        this.gateway = gateway;
        this.smartLockService = smartLockService;
    }

    public CompletableFuture<LockInfo> refreshStatus(LockInfo lockInfo) {
        return gateway.queryDoorStatus(lockInfo.getIpAddress(), lockInfo.getPortNumber(), lockInfo.getSnCode())
                .thenApply(status -> {
                    lockInfo.setSwitchStatus(status);
                    smartLockService.updateSwitchStatus(lockInfo.getLockId(), status);
                    return lockInfo;
                });
    }
}
