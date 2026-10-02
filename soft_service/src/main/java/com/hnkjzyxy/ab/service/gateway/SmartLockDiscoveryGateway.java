package com.hnkjzyxy.ab.service.gateway;

import com.hnkjzyxy.ab.dto.DiscoveredLockDevice;
import java.util.List;

/**
 * 局域网门禁设备发现接口，由启动模块的 SDK 适配器实现
 */
public interface SmartLockDiscoveryGateway {
    /**
     * 启动一轮局域网门禁设备搜索
     * <p>
     * 搜索结果由 SDK 异步回调逐步收集，调用返回不代表搜索已完成。
     * 可通过 discoveredDevices 获取本轮当前已发现的设备。
     * </p>
     */
    void startDiscovery();

    /**
     * 获取当前搜索已发现设备的快照
     * <p>
     * 返回当前时刻的结果，不等待本轮搜索完成，也不向调用方暴露 SDK 对象。
     * </p>
     *
     * @return 设备发现结果列表；尚未启动搜索或尚未发现设备时为空列表
     */
    List<DiscoveredLockDevice> discoveredDevices();
}
