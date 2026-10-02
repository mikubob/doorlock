package com.hnkjzyxy.ab.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 门禁设备发现结果，传递设备 SN、IP 地址和端口，屏蔽 SDK 类型
 */
@Getter
@AllArgsConstructor
public class DiscoveredLockDevice {
    /**
     * 门禁设备SN
     */
    private final String sn;
    /**
     * 门禁设备IP地址
     */
    private final String ipAddress;
    /**
     * 门禁设备TCP端口
     */
    private final int port;
}
