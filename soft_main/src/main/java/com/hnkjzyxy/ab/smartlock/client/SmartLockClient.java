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

/** 门禁 SDK 适配器，只负责设备通讯，不读写数据库。 */
@Component
public class SmartLockClient implements SmartLockGateway {

    @Override
    public void openDoor(String ipAddress, int port, String snStr,String Channel) {
        ConnectorAllocator allocator = ConnectorAllocator.GetAllocator();
        CompletableFuture<Boolean> futurePrice = new CompletableFuture<>();
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

        RemoteDoor_Parameter parameter1 = new RemoteDoor_Parameter(detail);
        parameter1.Door.SetDoor(Integer.parseInt(Channel), 1);
//        parameter1.Door.SetDoor(Integer.parseInt(Channel), 1);
//       parameter1.Door.SetDoor(2, 1);

        HoldDoor cmd = new HoldDoor(parameter1);

        detail.Event = new ConnectorEvent() {
            @Override
            public void CommandCompleteEvent(INCommand cmd, INCommandResult result) {
                System.out.println("CommandCompleteEvent命令执行成功！");
                futurePrice.complete(true);
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
    }

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

    @Override
    public void closeDoor(String sn, String ip, int port,String Channel) {
        CommandDetail detail = CommandAllocator.getTcpCommandDetail(sn, ip, port);
        // 增加空检查
        if (detail == null) {
            throw new IllegalArgumentException("无法获取有效的CommandDetail");
        }
        CloseDoorCommand cmd = new CloseDoorCommand(detail);
        cmd.execute(Channel);
    }
}
