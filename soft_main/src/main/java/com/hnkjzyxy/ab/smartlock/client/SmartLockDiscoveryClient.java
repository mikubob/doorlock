package com.hnkjzyxy.ab.smartlock.client;

import com.hnkjzyxy.ab.dto.DiscoveredLockDevice;
import com.hnkjzyxy.ab.service.gateway.SmartLockDiscoveryGateway;
import com.hnkjzyxy.ab.smartlock.command.SearchDeviceCommand;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 门禁 SDK 设备发现适配器，维护当前搜索并返回设备快照
 */
@Component
public class SmartLockDiscoveryClient implements SmartLockDiscoveryGateway {
    /**
     * 最近一次成功启动的设备搜索命令
     */
    private volatile SearchDeviceCommand currentSearch;

    /**
     * {@inheritDoc}
     */
    @Override
    public void startDiscovery() {
        SearchDeviceCommand search = new SearchDeviceCommand();
        search.start();
        currentSearch = search;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DiscoveredLockDevice> discoveredDevices() {
        SearchDeviceCommand search = currentSearch;
        if (search == null) {
            return Collections.emptyList();
        }
        return search.getDevices().stream()
                .map(device -> new DiscoveredLockDevice(device.SN, device.TCP.GetIP(), device.TCP.GetTCPPort()))
                .collect(Collectors.toList());
    }
}
