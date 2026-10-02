package com.hnkjzyxy.ab.service.support;

import com.hnkjzyxy.ab.model.LockInfo;
import com.hnkjzyxy.ab.service.SmartLockService;
import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

/**
 * 门禁状态更新服务，在查询通讯成功后更新指定锁的状态
 * <p>
 * 通讯异常通过 Future 传递给调用方，不执行状态回写。各次查询独立持有结果。
 * </p>
 */
@Service
public class SmartLockStateService {
    /**
     * 门禁设备通讯接口
     */
    private final SmartLockGateway gateway;
    /**
     * 智能门锁业务服务
     */
    private final SmartLockService smartLockService;

    /**
     * 初始化门禁状态更新服务
     *
     * @param gateway 门禁设备通讯接口
     * @param smartLockService 门禁设备状态持久化服务
     */
    public SmartLockStateService(SmartLockGateway gateway, SmartLockService smartLockService) {
        this.gateway = gateway;
        this.smartLockService = smartLockService;
    }

    /**
     * 查询指定门禁设备状态并在成功后回写
     * <p>
     * 每个设备使用独立的查询结果。仅在通讯正常完成后修改设备对象并调用状态更新服务。
     * </p>
     *
     * @param lockInfo 待查询的设备信息，包含锁ID、IP地址、端口和SN
     * @return 在通讯及状态回写完成后返回原设备对象的 Future；通讯失败时异常完成
     */
    public CompletableFuture<LockInfo> refreshStatus(LockInfo lockInfo) {
        return gateway.queryDoorStatus(lockInfo.getIpAddress(), lockInfo.getPortNumber(), lockInfo.getSnCode())
                .thenApply(status -> {
                    lockInfo.setSwitchStatus(status);
                    smartLockService.updateSwitchStatus(lockInfo.getLockId(), status);
                    return lockInfo;
                });
    }
}
