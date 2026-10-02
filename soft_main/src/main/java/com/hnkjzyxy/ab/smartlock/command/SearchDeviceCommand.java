package com.hnkjzyxy.ab.smartlock.command;

import Door.Access.Command.CommandDetail;
import Door.Access.Command.INCommand;
import Door.Access.Command.INCommandResult;
import Door.Access.Connector.ConnectorAllocator;
import Door.Access.Connector.ConnectorDetail;
import Door.Access.Connector.E_ControllerType;
import Door.Access.Connector.INConnectorEvent;
import Door.Access.Connector.UDP.UDPDetail;
import Door.Access.Data.INData;
import Door.Access.Door8800.Command.Door8800Command;
import Door.Access.Door8800.Command.System.Parameter.SearchEquptOnNetNum_Parameter;
import Door.Access.Door8800.Command.System.Result.SearchEquptOnNetNum_Result;
import Door.Access.Door8800.Command.System.SearchEquptOnNetNum;
import Door.Access.Door8800.Door8800Identity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;

/***
 * 查询门禁锁的信息
 * @author kmljs
 *
 */
public class SearchDeviceCommand implements INConnectorEvent {

    /**
     * 连接分配器
     */
    ConnectorAllocator allocator;
    /**
     * 搜索端口
     */
    int searchPort = 8101;
    /**
     * 本机IP地址（Linux 系统中本机IP为空即可）
     */
    String localIp = "127.0.0.1";
    /**
     * upd 服务绑定端口
     */
    int localPort = 9000;
    /**
     * 随机数据
     */
    Random rnd = new Random();
    /**
     * 网络标记最大值
     */
    int max = 65535;
    /**
     * 网络标记最小值
     */
    int min = 10000;
    /**
     * 网络标记
     */
    int SearchNetFlag;
    /**
     * 搜索次数
     */
    int SearchTimes = 3;

    /**
     * 本轮已发现的设备集合，通过同步快照向调用方提供结果
     */
    HashSet<SearchEquptOnNetNum_Result.SearchResult> devices = new HashSet<>();
    /**
     * 搜索到的设备列表
     */
    HashSet<String> deviceList = new HashSet<>();

    /**
     * 当前实例提交的搜索命令，用于隔离其他搜索实例的回调
     */
    private volatile SearchEquptOnNetNum currentCommand;

    /**
     * 初始化设备搜索命令并注册 SDK 通讯监听器
     */
    public SearchDeviceCommand() {
        allocator = ConnectorAllocator.GetAllocator();
        allocator.AddListener(this);/**添加事件监听*/
    }

    /**
     * 启动搜索
     */
    public void start() {
        String IP;
        /**
         * UDP通讯对象
         */
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
            System.out.println("本机IP地址：" + IP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        UDPDetail udp = new UDPDetail("255.255.255.255", searchPort, IP, localPort);
        CommandDetail dt = new CommandDetail();
        dt.Connector = udp;
        dt.Identity = new Door8800Identity("0000000000000000", Door8800Command.NULLPassword, E_ControllerType.Door8900);
        dt.RestartCount = 0;
        dt.Timeout = 6000;//每隔5秒发送一次，所以这里设定5秒超时

        /**
         * 网络标记就是一个随机数
         * */
        SearchNetFlag = rnd.nextInt(max) % (max - min + 1) + min;

        /**
         * 搜索命令参数对象
         */
        SearchEquptOnNetNum_Parameter par = new SearchEquptOnNetNum_Parameter(dt, SearchNetFlag);
        /**
         * 搜索命令对象
         */
        SearchEquptOnNetNum cmd = new SearchEquptOnNetNum(par);
        currentCommand = cmd;

        /**
         * 添加到命令队列中执行
         */
        allocator.AddCommand(cmd);
        /**
         * 搜索次数减一
         */
        SearchTimes--;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void CommandCompleteEvent(INCommand inCommand, INCommandResult inCommandResult) {
        System.out.println("搜索完成");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void CommandProcessEvent(INCommand inCommand) {
        System.out.println("正在搜索中。。。。。。");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ConnectorErrorEvent(INCommand inCommand, boolean b) {
        System.out.println("连接出错");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ConnectorErrorEvent(ConnectorDetail connectorDetail) {
        System.out.println("连接出错");
    }

    /**
     * 处理当前搜索命令的超时结果并按剩余次数继续搜索
     * <p>
     * 门禁搜索在超时回调中返回已收集设备；按 SN 去重并同步更新本轮设备集合。
     * </p>
     *
     * @param inCommand 发生超时的 SDK 命令，仅处理本实例当前搜索命令
     */
    @Override
    public void CommandTimeout(INCommand inCommand) {
        if (inCommand == currentCommand) {
            SearchEquptOnNetNum searchCmd = (SearchEquptOnNetNum) inCommand;
            SearchEquptOnNetNum_Result result = (SearchEquptOnNetNum_Result) searchCmd.getCommandResult();
            System.out.print(result.SearchTotal);
            synchronized (devices) {
                for (int i = 0; i < result.SearchTotal; i++) {
                    SearchEquptOnNetNum_Result.SearchResult device = result.ResultList.get(i);
                    if (deviceList.add(device.SN)) {
                        devices.add(device);
                        System.out.println("设备信息：SN=" + device.SN + ",IP=" + device.TCP.GetIP() + ",TCPPort=" + device.TCP.GetTCPPort());
                    }
                }
            }
            if (SearchTimes == 0) {
                System.out.println("搜索完成,共搜索到" + deviceList.size() + "个设备");
                return;
            }
            start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void PasswordErrorEvent(INCommand inCommand) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ChecksumErrorEvent(INCommand inCommand) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void WatchEvent(ConnectorDetail connectorDetail, INData inData) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ClientOnline(ConnectorDetail connectorDetail) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ClientOffline(ConnectorDetail connectorDetail) {

    }

    /**
     * 获取本轮搜索已发现设备的独立快照
     *
     * @return 查询结果列表
     */
    public HashSet<SearchEquptOnNetNum_Result.SearchResult> getDevices() {
        synchronized (devices) {
            return new HashSet<>(devices);
        }
    }
}
