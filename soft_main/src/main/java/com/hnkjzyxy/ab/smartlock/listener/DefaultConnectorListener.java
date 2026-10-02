package com.hnkjzyxy.ab.smartlock.listener;

import Door.Access.Command.INCommand;
import Door.Access.Command.INCommandResult;
import Door.Access.Connector.ConnectorDetail;
import Door.Access.Connector.INConnectorEvent;
import Door.Access.Data.INData;

/**
 * 门禁 SDK 默认空监听器，各回调均不执行业务处理
 */
public class DefaultConnectorListener implements INConnectorEvent {
    /**
     * 命令完成回调，默认不执行处理
     *
     * @param inCommand SDK 命令
     * @param inCommandResult SDK 命令执行结果
     */
    public void CommandCompleteEvent(INCommand inCommand, INCommandResult inCommandResult) {
    }

    /**
     * 命令执行进度回调，默认不执行处理
     *
     * @param inCommand SDK 命令
     */
    public void CommandProcessEvent(INCommand inCommand) {
    }

    /**
     * 连接错误回调，默认不执行处理
     *
     * @param inCommand SDK 命令
     * @param b 是否停止命令
     */
    public void ConnectorErrorEvent(INCommand inCommand, boolean b) {
    }

    /**
     * 连接错误回调，默认不执行处理
     *
     * @param connectorDetail SDK 连接信息
     */
    public void ConnectorErrorEvent(ConnectorDetail connectorDetail) {
    }

    /**
     * 命令超时回调，默认不执行处理
     *
     * @param inCommand SDK 命令
     */
    public void CommandTimeout(INCommand inCommand) {
    }

    /**
     * 设备密码错误回调，默认不执行处理
     *
     * @param inCommand SDK 命令
     */
    public void PasswordErrorEvent(INCommand inCommand) {
    }

    /**
     * 命令校验和错误回调，默认不执行处理
     *
     * @param inCommand SDK 命令
     */
    public void ChecksumErrorEvent(INCommand inCommand) {
    }

    /**
     * 设备监控数据回调，默认不执行处理
     *
     * @param connectorDetail SDK 连接信息
     * @param inData 设备监控数据
     */
    public void WatchEvent(ConnectorDetail connectorDetail, INData inData) {
    }

    /**
     * 客户端上线回调，默认不执行处理
     *
     * @param connectorDetail SDK 连接信息
     */
    public void ClientOnline(ConnectorDetail connectorDetail) {
    }

    /**
     * 客户端下线回调，默认不执行处理
     *
     * @param connectorDetail SDK 连接信息
     */
    public void ClientOffline(ConnectorDetail connectorDetail) {
    }
}