package com.hnkjzyxy.ab.smartlock.command;

import com.hnkjzyxy.ab.smartlock.support.CommandAllocator;

import Door.Access.Command.CommandDetail;
import Door.Access.Command.INCommand;
import Door.Access.Command.INCommandResult;
import Door.Access.Connector.ConnectorEvent;
import Door.Access.Door8800.Command.Door.CloseDoor;
import Door.Access.Door8800.Command.Door.Parameter.RemoteDoor_Parameter;

/**
 * 门禁关门命令封装，提交 SDK 命令并处理通讯回调
 */
public class CloseDoorCommand {
    /**
     * 关门命令通讯参数
     */
    CommandDetail cmdDtl;

    /**
     * 开门指令类
     *
     * @param detail SDK 命令通讯参数
     */
    public CloseDoorCommand(CommandDetail detail) {
        cmdDtl = detail;
        /**
         * 创建命令监听
         */
        cmdDtl.Event = getConnectorEvent();
    }

    /**
     * 开门指令
     *
     * @param Channel 门禁通道编号
     */
    public void execute(String Channel) {


        /**
         * 命令参数对象
         */

        RemoteDoor_Parameter parameter = new RemoteDoor_Parameter(cmdDtl);
        /**
         * 设置门禁控制器1-4门是否执行开门指令，1表示执行
         */
        parameter.Door.SetDoor(Integer.parseInt(Channel), 1);
//        parameter.Door.SetDoor(2, 1);
//        parameter.Door.SetDoor(3, 1);
//        parameter.Door.SetDoor(4, 1);
        /**
         * 创建命令对象
         */
        CloseDoor cmd = new CloseDoor(parameter);
        /**
         * 将需要执行的命令添加到队列，由分配器来执行
         */
        CommandAllocator.addCommand(cmd);
    }

    /**
     * 获取当前关门命令的通讯监听器
     *
     * @return SDK 通讯监听器
     */
    private ConnectorEvent getConnectorEvent() {
        return new ConnectorEvent() {
            /**
             * 命令成功
             * @param cmd
             * @param result
             */
            @Override
            public void CommandCompleteEvent(INCommand cmd, INCommandResult result) {
                CommandDetail cmdDtl = cmd.getCommandParameter().getCommandDetail();
                cmdDtl.Identity.GetIdentity();
                System.out.println(cmdDtl.Identity.GetIdentity() + ":远程关门成功");
            }

            /**
             * 命令超时
             * @param cmd
             */
            @Override
            public void CommandTimeout(INCommand cmd) {
                System.out.println("远程关门命令超时");
            }
        };
    }
}
