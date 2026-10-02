package com.hnkjzyxy.ab.service.gateway;

import java.util.concurrent.CompletableFuture;

/** 门禁通讯边界，由启动模块的设备 SDK 适配器实现。 */
public interface SmartLockGateway {
    void openDoor(String ipAddress, int port, String sn, String channel);
    void closeDoor(String sn, String ipAddress, int port, String channel);
    CompletableFuture<Integer> queryDoorStatus(String ipAddress, int port, String sn);
}
