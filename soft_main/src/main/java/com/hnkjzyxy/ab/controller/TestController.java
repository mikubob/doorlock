package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.utils.IpUtil;
import com.hnkjzyxy.ab.utils.ReadExcelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 测试接口
 * 用于接口连通性、文件解析及客户端网络信息自检，非业务接口
 */
@RestController
public class TestController {
    @Autowired
    private UserService userService;
    @Autowired
    private ReadExcelUtils readExcelUtils;

    public static void main(String[] args) {
//        List<Integer> list = JSONArray.parseArray("[1,2,3,4,5]", Integer.class);
//        String string = list.toString();
//        System.out.println(string);

    }

    /**
     * 接口连通性测试
     * 返回固定文本及请求来源 IP，用于自检服务是否可用
     *
     * @return 测试文本及客户端 IP
     */
    @GetMapping("/test")
    public String test1(HttpServletRequest request) {
        String ipAddr = IpUtil.getIpAddr(request);
        return "test1测试" + ipAddr;
    }

    /**
     * 查询全部用户列表
     *
     * @param param 查询参数
     * @return 用户列表
     */
    @GetMapping("/userList")
    //@Cacheable(value = {"userList"},key = "#authentication.getName()",sync = true)
    public ApiResult getUserList(ProjectParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        param.setUserId(user.getUserId());
        ApiResult apiResult = new ApiResult();
        return apiResult.put("data", userService.list());
    }

    /**
     * 解析 Excel 文件
     * 上传 Excel 并返回逐行解析后的键值数据
     *
     * @param file Excel 文件
     * @return 解析后的表格数据
     */
    @GetMapping("/excel")
    public ApiResult getExcel(MultipartFile file) {
        List<Map<String, String>> maps = readExcelUtils.generalExcel(file);
        return ApiResult.ok("data", maps);
    }

    /**
     * 文件上传测试
     *
     * @param file 上传的文件
     * @return 操作结果
     */
    @GetMapping("/file")
    public ApiResult getFile(MultipartFile file) {


        return ApiResult.ok("data");
    }

    /**
     * 获取请求来源设备的 MAC 地址
     *
     * @return 客户端 MAC 地址
     */
    @GetMapping("/test/mac")
    public ApiResult testMac(HttpServletRequest request) throws Exception {
        String ipAddr = IpUtil.getIpAddr(request);
        System.out.println(ipAddr);
        String macAddress = IpUtil.getMacAddrByIp(ipAddr);
        return ApiResult.ok("data", macAddress);
    }
}
