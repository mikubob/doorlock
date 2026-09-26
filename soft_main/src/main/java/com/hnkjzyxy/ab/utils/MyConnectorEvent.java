package com.hnkjzyxy.ab.utils;

import Door.Access.Command.INCommand;
import Door.Access.Command.INCommandResult;
import Door.Access.Connector.ConnectorDetail;
import Door.Access.Connector.INConnectorEvent;
import Door.Access.Data.INData;

public class MyConnectorEvent implements INConnectorEvent {
    public void CommandCompleteEvent(INCommand inCommand, INCommandResult inCommandResult) {
    }

    public void CommandProcessEvent(INCommand inCommand) {
    }

    public void ConnectorErrorEvent(INCommand inCommand, boolean b) {
    }

    public void ConnectorErrorEvent(ConnectorDetail connectorDetail) {
    }

    public void CommandTimeout(INCommand inCommand) {
    }

    public void PasswordErrorEvent(INCommand inCommand) {
    }

    public void ChecksumErrorEvent(INCommand inCommand) {
    }

    public void WatchEvent(ConnectorDetail connectorDetail, INData inData) {
    }

    public void ClientOnline(ConnectorDetail connectorDetail) {
    }

    public void ClientOffline(ConnectorDetail connectorDetail) {
    }
}