package com.hnkjzyxy.ab.service.gateway;

import java.util.concurrent.CompletableFuture;

/**
 * 门禁设备通讯接口，由启动模块的 SDK 适配器实现
 */
public interface SmartLockGateway {
    /**
     * 发送设备命令并等待厂家回执；完成表示设备已接受命令，物理门状态须另行查询。
     * @param ip 设备 IP
     * @param port 设备端口
     * @param sn 真实 SN
     * @param channel 真实通道
     * @param open 是否开门
     * @return 每个命令独立的回执
     */
    CompletableFuture<Void> sendDoorCommand(String ip, int port, String sn, String channel, boolean open);
    /**
     * 向指定门禁设备发送开门命令
     * <p>
     * 命令由 SDK 异步执行，方法返回仅表示完成命令提交，不保证设备已完成操作。
     * </p>
     *
     * @param ipAddress 门禁设备IP地址
     * @param port 门禁设备TCP端口
     * @param sn 门禁设备SN
     * @param channel 门禁通道编号
     */
    void openDoor(String ipAddress, int port, String sn, String channel);
    /**
     * 向指定门禁设备发送关门命令
     * <p>
     * 命令由 SDK 异步执行，方法返回仅表示完成命令提交，不保证设备已完成操作。
     * </p>
     *
     * @param sn 门禁设备SN
     * @param ipAddress 门禁设备IP地址
     * @param port 门禁设备TCP端口
     * @param channel 门禁通道编号
     */
    void closeDoor(String sn, String ipAddress, int port, String channel);
    /**
     * 异步查询门禁设备的第一门状态
     *
     * @param ipAddress 门禁设备IP地址
     * @param port 门禁设备TCP端口
     * @param sn 门禁设备SN
     * @return 跟随 SDK 回调完成的状态 Future，保留设备协议状态值；通讯失败时异常完成
     */
    CompletableFuture<Integer> queryDoorStatus(String ipAddress, int port, String sn);
}
