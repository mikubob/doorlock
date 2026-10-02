package com.hnkjzyxy.ab.utils;

import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 * 跨平台文件路径分隔符转换工具
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 13:53
 */
public class FilePathUtils {

    /**
     * 当前操作系统的文件分隔符（Windows 为 \，Unix/Mac 为 /）
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
     * 本地调试入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        //这里是win的调用，所以打印出来的就是\  windows中的文件的路径是反斜杠
        //而mac和unix中的文件的路径是正的斜杠，也就是注释键
        System.out.println(FILE_SEPARATOR);

        //一个反斜杠 表示这个是转义字符的意思，两个表示这个是win中路径的地址
        String path = "D:\\uploadFile\\test01\\29324";
        System.out.println(path.replace("\\", ",").replace("/", ","));

        System.out.println("------------test----------");
        String arrString = "[\"/pdf/file/202005/504366351138160640.pdf\",\"/pdf/file/202005/504366572270256128.pdf\",\"/pdf/file/202005/504366741296513024.pdf\"]";

        List<String> strings = JSON.parseArray(arrString, String.class);//通过这个方法就可以把一整串的数组字符串变成真正的数组
        System.out.println(arrString.length());//这里统计的是字符的个数
        System.out.println(strings);
    }

    /**
     * 将路径分隔符转换为当前操作系统的形式
     *
     * @param path 本地文件路径
     * @return 查询得到的文本信息
     */
    public static String getRealFilePath(String path) {

        //两个replace就表示，无论是那个系统，都会把文件的路径地址变成当前的系统的文件的分割的方式。
        //返回这个文件真实的地址
        return path.replace("/", FILE_SEPARATOR).replace("\\", FILE_SEPARATOR);
    }


}
