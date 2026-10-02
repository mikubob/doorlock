package com.hnkjzyxy.ab.smartlock.support;

import com.hnkjzyxy.ab.smartlock.listener.DefaultConnectorListener;

import Door.Access.Command.CommandDetail;
import Door.Access.Command.INCommand;
import Door.Access.Connector.ConnectorAllocator;
import Door.Access.Connector.ConnectorDetail;
import Door.Access.Connector.E_ControllerType;
import Door.Access.Connector.INConnector;
import Door.Access.Connector.TCPClient.TCPClientDetail;
import Door.Access.Door8800.Door8800Identity;

public class CommandAllocator {

    /**
     * 连接分配器
     */
    public static ConnectorAllocator allocator = ConnectorAllocator.GetAllocator();

    static {
        allocator.AddListener(new DefaultConnectorListener());/** 添加全局事件监听*/
    }

    /**
     * 添加需要执行的命令
     *
     * @param cmd
     */
    public static void addCommand(INCommand cmd) {
        allocator.AddCommand(cmd);
    }

    /**
     * 获取连接通道信息
     *
     * @param detail
     * @return
     */
    public static INConnector getConnector(ConnectorDetail detail) {
        return allocator.GetConnector(detail);
    }


    /**
     * 获取设备通讯详情
     *
     * @return 通讯详情
     */
    public static CommandDetail getTcpCommandDetail(String sn, String ip, int port) {
        Door8800Identity idt = new Door8800Identity(
                sn,  /**设备SN 16位由英文数字和横杠*/
                "FFFFFFFF",/**设备通讯密码 八位数字组成*/
                E_ControllerType.Door8900); /**设备类型*/
        CommandDetail commandDetail = new CommandDetail();
        /**
         * 通讯对象
         */
        commandDetail.Connector = new TCPClientDetail(
                ip,/**设备IP地址，默认是192.168.1.150*/
                port);/**设备tcp端口号，默认是8000*/
        commandDetail.Identity = idt;
        commandDetail.Timeout = 5000;/**命令超时时间*/
        commandDetail.RestartCount = 2;/**命令失败后重新尝试发送的次数*/
        return commandDetail;
    }

    public static CommandDetail getTcpCommandDetail() {
        Door8800Identity idt = new Door8800Identity(
                "FC-8940H43030001",  /**设备SN 16位由英文数字和横杠*/
                "FFFFFFFF",/**设备通讯密码 八位数字组成*/
                E_ControllerType.Door8900); /**设备类型*/
        CommandDetail commandDetail = new CommandDetail();
        /**
         * 通讯对象
         */
        commandDetail.Connector = new TCPClientDetail(
                "192.168.1.158",/**设备IP地址，默认是192.168.1.150*/
                8000);/**设备tcp端口号，默认是8000*/
        commandDetail.Identity = idt;
        commandDetail.Timeout = 5000;/**命令超时时间*/
        commandDetail.RestartCount = 2;/**命令失败后重新尝试发送的次数*/
        return commandDetail;
    }


}

