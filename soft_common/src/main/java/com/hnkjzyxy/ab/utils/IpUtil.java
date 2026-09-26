package com.hnkjzyxy.ab.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/4/16 14:29
 */
public class IpUtil {
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
//            System.out.println("Proxy-Client-IP:" + ip);
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
//            System.out.println("WL-Proxy-Client-IP:" + ip);
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
//            System.out.println("HTTP_CLIENT_IP:" + ip);
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
//            System.out.println("HTTP_X_FORWARDED_FOR:" + ip);
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
//            System.out.println("getRemoteAddr:" + ip);
        }

        String comma = ",";
        String localhost = "127.0.0.1";
        //对于多个地址的情况是可能存在多个代理，一般第一个为真是地址
        if (ip.contains(comma)) {
            ip = ip.split(",")[0];
        }

        if (localhost.equalsIgnoreCase(ip) || "localhost".equalsIgnoreCase(ip) || "0:0:0:0:0:0:0:1".equalsIgnoreCase(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException var5) {
                var5.printStackTrace();
            }
        }

        return ip;
    }


    public static String getMacAddrByIp(String ip) {
        StringBuilder sb = new StringBuilder();
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getByName(ip));
            byte[] mac = network.getHardwareAddress();

            System.out.print("MAC address : ");
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
