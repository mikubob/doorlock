package com.hnkjzyxy.ab.smartlock.client;

import Door.Access.Command.CommandDetail;
import Door.Access.Command.CommandParameter;
import Door.Access.Command.INCommand;
import Door.Access.Command.INCommandResult;
import Door.Access.Connector.ConnectorAllocator;
import Door.Access.Connector.ConnectorDetail;
import Door.Access.Connector.ConnectorEvent;
import Door.Access.Connector.E_ControllerType;
import Door.Access.Connector.TCPClient.TCPClientDetail;
import Door.Access.Door8800.Command.Data.DoorPortDetail;
import Door.Access.Door8800.Command.Door.HoldDoor;
import Door.Access.Door8800.Command.Door.Parameter.RemoteDoor_Parameter;
import Door.Access.Door8800.Command.System.ReadWorkStatus;
import Door.Access.Door8800.Command.System.Result.ReadWorkStatus_Result;
import Door.Access.Door8800.Door8800Identity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import com.hnkjzyxy.ab.service.gateway.SmartLockGateway;
import com.hnkjzyxy.ab.smartlock.listener.DefaultConnectorListener;
import com.hnkjzyxy.ab.smartlock.support.CommandAllocator;
import com.hnkjzyxy.ab.smartlock.command.CloseDoorCommand;

/**
 * 门禁 SDK 适配器，只负责设备通讯，不读写数据库。
 */
@Component
public class SmartLockClient implements SmartLockGateway {

    /**
     * 通过 SDK 提交开门命令
     * <p>
     * 命令结果由设备通讯回调处理，本方法不等待设备完成操作。
     * </p>
     *
     * @param ipAddress 门禁设备IP地址
     * @param port 门禁设备TCP端口
     * @param snStr 门禁设备SN
     * @param Channel 门禁通道编号
     */
    @Override
    public void openDoor(String ip, int port, String sn, String channel) { sendDoorCommand(ip, port, sn, channel, true); }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> sendDoorCommand(String ip, int port, String sn, String channel, boolean open) {
        if (channel == null || !channel.matches("[1-4]")) throw new IllegalArgumentException("通道必须为1至4");
        CompletableFuture<Void> receipt = new CompletableFuture<>();
        CommandDetail detail = CommandAllocator.getTcpCommandDetail(sn, ip, port);
        if (detail == null) throw new IllegalArgumentException("设备通讯参数无效");
        detail.Event = new ConnectorEvent() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void CommandCompleteEvent(INCommand command, INCommandResult result) { receipt.complete(null); }
            /**
             * {@inheritDoc}
             */
            @Override
            public void CommandTimeout(INCommand command) { receipt.completeExceptionally(new java.util.concurrent.TimeoutException("设备命令超时")); }
            /**
             * {@inheritDoc}
             */
            @Override
            public void ConnectorErrorEvent(ConnectorDetail connector) { receipt.completeExceptionally(new IllegalStateException("设备连接失败")); }
            /**
             * {@inheritDoc}
             */
            @Override
            public void ConnectorErrorEvent(INCommand command, boolean stopped) { receipt.completeExceptionally(new IllegalStateException("设备连接失败")); }
            /**
             * {@inheritDoc}
             */
            @Override
            public void PasswordErrorEvent(INCommand command) { receipt.completeExceptionally(new IllegalStateException("设备通讯认证失败")); }
        };
        RemoteDoor_Parameter parameter = new RemoteDoor_Parameter(detail);
        parameter.Door.SetDoor(Integer.parseInt(channel), 1);
        if (open) CommandAllocator.addCommand(new HoldDoor(parameter));
        else CommandAllocator.addCommand(new Door.Access.Door8800.Command.Door.CloseDoor(parameter));
        return receipt;
    }

    /**
     * 通过 SDK 异步查询第一门状态
     *
     * @param ipAddress 门禁设备IP地址
     * @param port 门禁设备TCP端口
     * @param snStr 门禁设备SN
     * @return SDK 回调完成后的门状态值；超时、连接或密码错误时异常完成
     */
    @Override
    public CompletableFuture<Integer> queryDoorStatus(String ipAddress, int port, String snStr) {
        ConnectorAllocator allocator = ConnectorAllocator.GetAllocator();
        CompletableFuture<Integer> futurePrice = new CompletableFuture<>();
        allocator.AddListener(new DefaultConnectorListener());

        CommandDetail detail = new CommandDetail();
        detail.Identity = new Door8800Identity(
                snStr,
                "FFFFFFFF",
                E_ControllerType.Door8900);

        TCPClientDetail tcp = new TCPClientDetail(ipAddress, port);
        tcp.Timeout = 5000;
        tcp.RestartCount = 1;
        detail.Connector = tcp;
        detail.RestartCount = 3;
        detail.Timeout = 3000;

        CommandParameter parameter = new CommandParameter(detail);
        ReadWorkStatus cmd = new ReadWorkStatus(parameter);

        detail.Event = new ConnectorEvent() {
            @Override
            public void CommandCompleteEvent(INCommand cmd, INCommandResult result) {
                if (result instanceof ReadWorkStatus_Result) {
                    ReadWorkStatus_Result statusResult = (ReadWorkStatus_Result) result;
                    // 增加空指针检查
                    if (statusResult.LockState != null) {
                        DoorPortDetail dp = statusResult.LockState;
                        futurePrice.complete(dp.GetDoor(1));
                    } else {
                        futurePrice.completeExceptionally(new Exception("门状态信息为空"));
                        return;
                    }
                }
                System.out.println("CommandCompleteEvent命令执行成功！");
                if (!futurePrice.isDone()) {
                    futurePrice.completeExceptionally(new IllegalStateException("设备返回的门状态结果无效"));
                }
            }

            @Override
            public void CommandTimeout(INCommand cmd) {
                System.out.println("CommandTimeout命令执行超时！");
                futurePrice.completeExceptionally(new Exception("命令执行超时"));
            }

            @Override
            public void ConnectorErrorEvent(ConnectorDetail detail) {
                System.out.println("ConnectorErrorEvent连接设备出错！");
                futurePrice.completeExceptionally(new Exception("连接设备出错"));
            }

            @Override
            public void ConnectorErrorEvent(INCommand cmd, boolean isStop) {
                System.out.println("ConnectorErrorEvent连接设备出错！");
                futurePrice.completeExceptionally(new Exception("连接设备出错"));
            }

            @Override
            public void PasswordErrorEvent(INCommand cmd) {
                System.out.println("PasswordErrorEvent设备通讯密码错误！");
                futurePrice.completeExceptionally(new Exception("设备通讯密码错误"));
            }
        };
        allocator.AddCommand(cmd);
        return futurePrice;
    }

    /**
     * 通过 SDK 提交关门命令
     * <p>
     * 命令结果由设备通讯回调处理，本方法不等待设备完成操作。
     * </p>
     *
     * @param sn 门禁设备SN
     * @param ip 门禁设备IP地址
     * @param port 门禁设备TCP端口
     * @param Channel 门禁通道编号
     */
    @Override
    public void closeDoor(String sn, String ip, int port, String channel) { sendDoorCommand(ip, port, sn, channel, false); }
}
