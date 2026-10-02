package com.hnkjzyxy.ab.controller;

import cn.hutool.core.util.StrUtil;
import com.hnkjzyxy.ab.model.Construct;
import com.hnkjzyxy.ab.model.ConstructResult;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ConFileParam;
import com.hnkjzyxy.ab.params.ConstructQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ConstructResultService;
import com.hnkjzyxy.ab.service.ConstructService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.utils.UploadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import static com.hnkjzyxy.ab.controller.ProjectController.getApiResult;

/**
 * 建设项目管理
 * 提供建设项目的创建、分配、成果提交与下载等接口
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/5/8 15:36
 */
@RestController
public class ConstructController {

    /**
     * 建设项目业务服务
     */
    @Autowired
    private ConstructService constructService;
    /**
     * 建设项目提交结果业务服务
     */
    @Autowired
    private ConstructResultService constructResultService;
    /**
     * 上传、下载及材料压缩工具
     */
    @Resource
    private UploadUtils uploadUtils;
    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;


    /**
     * 新增建设项目
     *
     * @param construct 建设项目信息
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/add/construct")
    public ApiResult addConstruct(@Validated @RequestBody Construct construct, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        construct.setUId(user.getUserId());
        constructService.addConstruct(construct);
        return ApiResult.ok("创建成功！");
    }

    /**
     * 根据项目ID查询建设项目详情
     *
     * @param id 建设项目ID
     * @return 建设项目详情
     */
    @GetMapping("/getConstruct/{id}")
    public ApiResult getConstructById(@PathVariable("id") String id) {
        if (StrUtil.isBlank(id)) {
            throw new RuntimeException("建设项目id不能为空！");
        }
        Construct construct = constructService.getConstructById(id);
        return ApiResult.ok("data", construct);
    }

    /**
     * 分配建设项目
     *
     * @param param 建设项目分配参数
     * @return 操作结果
     */
    @PostMapping("/assignment/construct")
    public ApiResult assignmentConstruct(ConstructQueryParam param) {
        constructService.assignmentConstruct(param);
        return ApiResult.ok("分配成功！");
    }

    /**
     * 获取建设项目年份列表
     *
     * @param type 查询类型（0=我创建的，1=我接收的，其他=全部）
     * @param authentication 当前登录认证信息
     * @return 建设项目年份列表
     */
    @GetMapping("/construct/years/{type}")
    public ApiResult getConstructYears(@PathVariable("type") String type, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        List<String> list = constructService.getConstructYears(user.getUserId());
        List<String> userList = constructService.getConstructYearsByUser(user.getUserId());
        return getApiResult(type, list, userList);
    }

    /**
     * 查询我创建的建设项目列表
     *
     * @param param 建设项目分页查询条件
     * @return 建设项目列表及分页数据
     */
    @GetMapping("/construct/list")
    public ApiResult getConstructList(ConstructQueryParam param) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByName(authentication.getName());
        param.setUserId(user.getUserId());
        Map<String, Object> map = constructService.getConstructList(param);
        return ApiResult.ok("data", map);
    }

    /**
     * 查询我接收的建设项目列表
     *
     * @param param 建设项目分页查询条件
     * @param authentication 当前登录认证信息
     * @return 建设项目列表及分页数据
     */
    @GetMapping("/construct/list/user")
    public ApiResult getConstructListByUser(ConstructQueryParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        param.setUserId(user.getUserId());
        Map<String, Object> map = constructService.getConstructListByUserId(param);
        return ApiResult.ok("data", map);
    }

    /**
     * 上传项目成果文件
     *
     * @param file   成果文件
     * @param conId  建设项目ID
     * @param taskId 任务ID
     * @return 成果文件访问地址
     */
    @PostMapping("/uploadConFile")
    public ApiResult uploadConFile(@RequestParam("file") MultipartFile file, @RequestParam("conId") String conId, @RequestParam("taskId") String taskId) {
        if (StrUtil.isBlank(conId) || StrUtil.isBlank(taskId)) {
            throw new RuntimeException("项目id和任务不能为空！");
        }
        String conUrl = conId + File.separator + taskId;
        Optional<String> s = uploadUtils.uploadConFile(file, conUrl);
        return ApiResult.ok("data", s.isPresent() ? s.get() : "");
    }

    /**
     * 提交建设项目成果
     *
     * @param result 成果提交信息
     * @return 提交后的成果信息
     */
    @PostMapping("/subConstruct/result")
    public ApiResult subConstructResult(@Validated @RequestBody ConstructResult result) {
        ConstructResult constructResult = constructService.subConstructResult(result);
        return ApiResult.ok("data", constructResult);
    }

    /**
     * 删除项目成果
     *
     * @param id 成果ID
     * @return 操作结果
     */
    @PostMapping("/delAchievement/{id}")
    //@CacheEvict(value = {HnkjxyConstants.CONSTRUCT_DETAIL},allEntries = true)
    public ApiResult delAchieveById(@PathVariable("id") String id) {
        constructResultService.removeConstructResult(id);
        return ApiResult.ok();
    }

    /**
     * 下载项目成果
     * 按建设项目与任务打包下载成果文件
     *
     * @param param    成果文件查询条件（conId、taskId）
     * @param response HTTP 响应流，直接输出压缩包
     */
    @GetMapping("/downloadZipPdf")
    public void downloadPDF(@Validated ConFileParam param, HttpServletResponse response) {
        String conUrl = param.getConId();
        if (StrUtil.isNotBlank(param.getTaskId())) {
            conUrl = conUrl + File.separator + param.getTaskId();
        }
        uploadUtils.downloadPDFs(conUrl, response);
    }


}