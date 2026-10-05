package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.export.ExcelResponseExporter;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.client.OaApiClient;
import com.hnkjzyxy.ab.exception.ImportRejectedException;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.vo.CheckResultByTeacherDataVo;
import com.hnkjzyxy.ab.vo.CheckResultDataVo;
import com.hnkjzyxy.ab.vo.CheckResultImportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 教学巡查管理
 * 提供教学巡查记录的录入、查询、统计分析与导出接口
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/4/23 20:19
 */
@Slf4j
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;

    /**
     * 教学巡查结果业务服务
     */
    @Autowired
    private CheckResultService checkResultService;

    /**
     * OA 接口客户端
     */
    @Autowired
    private OaApiClient oaApiClient;


    /**
     * 查询教学巡查记录列表
     *
     * @param resultVo 巡查记录查询条件（可选）
     * @param authentication 当前登录认证信息
     * @return 当前用户所属学院的巡查记录列表
     */
    @PostMapping("/list")
    public ApiResult checkResult(@RequestBody(required = false) CheckResult resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());

        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }


        List<CheckResult> vo = checkResultService.getList(resultVo, user);
        List<CheckResult> voback=new ArrayList<>();
        for(CheckResult item:vo){
            if (Objects.equals(item.getCollege(), user.getCollege())){
                voback.add(item);
            }
        }

        return ApiResult.ok("data", voback);
    }

    /**
     * 新增或修改巡查记录
     *
     * @param resultVo 巡查记录信息
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping
    //@RepeatSubmit
    public ApiResult checkResultAddOrEdit(@Valid @RequestBody CheckResult resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        /**
         * 历史接口不支持课程时段，请假人数保留人工确认或原快照，不用今天的数据覆盖。
         */
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));
        if (!admin) {
            if (user == null || user.getCollege() == null) throw new com.hnkjzyxy.ab.exception.AuthPermissionException(403, "用户学院范围未确认");
            if (resultVo.getId() != null) {
                CheckResult previous = checkResultService.getById(resultVo.getId());
                if (previous == null || !java.util.Objects.equals(user.getCollege(), previous.getCollege())) throw new com.hnkjzyxy.ab.exception.AuthPermissionException(403, "不能修改其他学院巡查");
            }
            resultVo.setCollege(user.getCollege());
        }
        if (resultVo.getId() != null && resultVo.getPeopleLeave() == null) {
            CheckResult previous = checkResultService.getById(resultVo.getId());
            if (previous != null) resultVo.setPeopleLeave(previous.getPeopleLeave());
        }
        checkResultService.addOrEdit(resultVo);

        return ApiResult.ok();
    }


    /**
     * 删除巡查记录
     *
     * @param id 巡查记录ID
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/{id}")
    public ApiResult checkResultDeleteById(@PathVariable("id") Integer id, Authentication authentication) {

        if (ObjectUtil.isNull(id)) {
            throw new RuntimeException("id不能为空！");
        }


        checkResultService.removeById(id);
        return ApiResult.ok();
    }


    /**
     * 巡查数据统计
     * 按辅导员所带班级维度统计巡查数据，管理员可查看全部学院
     *
     * @param resultVo 统计查询条件
     * @param authentication 当前登录认证信息
     * @return 巡查数据统计结果
     */
    @GetMapping("/dataStatistics")
    public ApiResult dataStatistics(CheckResult resultVo, Authentication authentication) {
        String userName = authentication.getName();
        // 直接查询数据库，绕过缓存
        User directUser = userService.getOne(new QueryWrapper<User>().eq("user_name", userName));
        List<CheckResultDataVo> map = new ArrayList<>();
        User user = userService.getUserByName(userName);
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        if (directUser != null && directUser.getNickName().equals("管理员")) {
            return ApiResult.ok("data", checkResultService.dataStatistics(resultVo));
        }

        String college = null;
        if (directUser != null) {
            college = directUser.getCollege();
            //通过时间来查询数据

            for (CheckResultDataVo dataStatistic : checkResultService.dataStatistics(resultVo)) {
                // 使用空安全比较，避免NullPointerException
                if (Objects.equals(dataStatistic.getCollege(), college)) {
                    map.add(dataStatistic);
                }
            }
            return ApiResult.ok("data", map);
        }
        return ApiResult.error("用户无学院信息");
    }


    /**
     * 查询单个班级在指定时间段内的巡查数据
     *
     * @param resultVo 查询条件（班级、时间范围）
     * @param authentication 当前登录认证信息
     * @return 班级巡查数据明细
     */
    @GetMapping("/dataStatistics/classes")
    public ApiResult dataStatisticsByClasses(CheckResult resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        String userName = user.getUserName();
        User one = userService.getOne(new QueryWrapper<User>().eq("user_name", userName));
        if ("管理员".equals(one.getNickName())) {
            one.setCollege(null);
            List<CheckResult> map = checkResultService.dataStatisticsByClasses(resultVo, one);
            return ApiResult.ok("data", map);
        }
        List<CheckResult> map = checkResultService.dataStatisticsByClasses(resultVo, one);
        return ApiResult.ok("data", map);
    }


    /**
     * 导出巡查数据为 Excel
     *
     * @param resultVo 待导出的巡查数据
     * @param response HTTP 响应流，直接输出 Excel 文件
     */
    @PostMapping("/export")
    public void exportAssess(@RequestBody List<CheckResultDataVo> resultVo, HttpServletResponse response) {

        System.out.println(resultVo);
        if (ObjectUtil.isEmpty(resultVo)) {
            throw new RuntimeException("导出结果不能为空！");
        }
        ExcelResponseExporter.exportSchedule(resultVo, response);
    }


    /**
     * 上传巡查数据
     * 解析 Excel 批量导入教学巡查记录，整次导入在单个事务内完成
     *
     * @param file           巡查数据 Excel 文件
     * @param authentication 当前登录用户
     * @return 导入成功时返回回执（总行数、成功数、失败数与逐行错误清单）；
     * 整次导入被拒绝时返回 code=400 与拒绝原因，不写入任何数据
     */
    @PostMapping("/upload")
    //@RepeatSubmit
    public ApiResult uploadCourse(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());

        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }

        CheckResultImportResult result;
        try {
            result = checkResultService.uploadCheckResult(file, user);
        } catch (ImportRejectedException e) {
            // 整次导入被拒绝（数据量超限 / 有错即放弃 / 表头严格校验未通过）：明确告知失败，而非「成功 0 条」
            log.warn("[巡查导入] 拒绝导入，上传人={}，原因={}", user.getUserName(), e.getReason());
            return ApiResult.error("导入被拒绝：" + e.getReason());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return ApiResult.ok("msg", result.getMessage()).put("data", result);
    }


    /**
     * 教学巡查分析（按科任老师分组）
     *
     * @param resultVo 统计查询条件
     * @param authentication 当前登录认证信息
     * @return 按科任老师分组的巡查分析数据
     */
    @GetMapping("/dataStatisticsByTeacher")
    public ApiResult teachCheckAnalysis(CheckResult resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        String userName = authentication.getName();
        User directUser = userService.getOne(new QueryWrapper<User>().eq("user_name", userName));
        //判断权限
        if (directUser != null && directUser.getNickName().equals("管理员")) {
            return ApiResult.ok("data", checkResultService.dataStatistics(resultVo));
        }
        if (directUser != null) {

            List<CheckResultByTeacherDataVo> list = new ArrayList<>();
            for (CheckResultByTeacherDataVo teachCheckAnalysis : checkResultService.teachCheckAnalysis(resultVo)) {
                String college = teachCheckAnalysis.getCollege();
                // 使用空安全比较，避免NullPointerException
                if (Objects.equals(directUser.getCollege(), college)) {
                    list.add(teachCheckAnalysis);
                }
            }

            return ApiResult.ok("data", list);
        }

        return ApiResult.error("用户无学院信息");
    }


    /**
     * 按教师维度统计巡查数据
     *
     * @param resultVo 统计查询条件
     * @param authentication 当前登录认证信息
     * @return 教师维度巡查统计数据
     */
    @GetMapping("/dataStatistics/teacher")
    public ApiResult dataStatisticsByTeacher(CheckResult resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<CheckResult> map = checkResultService.dataStatisticsByTeacher(resultVo);
        return ApiResult.ok("data", map);
    }

    /**
     * 导出教师维度巡查数据为 Excel
     *
     * @param resultVo 待导出的巡查数据
     * @param response HTTP 响应流，直接输出 Excel 文件
     */
    @PostMapping("/exportByTeacher")
    public void exportAssessByTeacher(@RequestBody List<CheckResultByTeacherDataVo> resultVo, HttpServletResponse response) {

        if (ObjectUtil.isEmpty(resultVo)) {
            throw new RuntimeException("导出结果不能为空！");
        }
        ExcelResponseExporter.exportScheduleByTeacher(resultVo, response);
    }

    /**
     * 调试接口：查看当前用户信息
     * 临时用于排查用户学院、缓存等数据，正式环境建议移除
     *
     * @param authentication 当前登录认证信息
     * @return 调试结果提示（明细输出到控制台）
     */
    @GetMapping("/debug/userInfo")
    public ApiResult debugUserInfo(Authentication authentication) {
        String userName = authentication.getName();
        System.out.println("=== 用户信息调试 ===");
        System.out.println("Authentication用户名: " + userName);

        // 1. 直接查询数据库
        User directUser = userService.getOne(new QueryWrapper<User>().eq("user_name", userName));
        System.out.println("直接数据库查询结果:");
        System.out.println("  User ID: " + (directUser != null ? directUser.getUserId() : "null"));
        System.out.println("  User Name: " + (directUser != null ? directUser.getUserName() : "null"));
        System.out.println("  College: " + (directUser != null ? directUser.getCollege() : "null"));

        // 2. 通过缓存查询
        User cachedUser = userService.getUserByName(userName);
        System.out.println("缓存查询结果:");
        System.out.println("  User ID: " + (cachedUser != null ? cachedUser.getUserId() : "null"));
        System.out.println("  User Name: " + (cachedUser != null ? cachedUser.getUserName() : "null"));
        System.out.println("  College: " + (cachedUser != null ? cachedUser.getCollege() : "null"));

        // 3. 比较两个对象是否相同
        if (directUser != null && cachedUser != null) {
            System.out.println("对象比较:");
            System.out.println("  是否同一个对象: " + (directUser == cachedUser));
            System.out.println("  学院是否相同: " + Objects.equals(directUser.getCollege(), cachedUser.getCollege()));
        }

        System.out.println("=== 调试结束 ===");

        return ApiResult.ok("调试信息已输出到控制台");
    }

    /**
     * 按到岗时间排序巡查记录
     *
     * @param order 排序方式（ascending=升序，descending=降序）
     * @return 排序后的巡查记录列表
     */
    @GetMapping("timeSorting")
    public ApiResult timeSorting(@RequestParam String order/*,@RequestParam String commuteTime*/) {
        List<CheckResult> checkResult=checkResultService.sortedByTime(order);
        return ApiResult.ok("data",checkResult);
    }
    /**
     * 按到岗率与日期排序巡查记录
     *
     * @param arrivalRate 到岗率排序方式（ascending=升序，descending=降序）
     * @param date        日期排序方式（ascending=升序，descending=降序）
     * @return 排序后的巡查记录列表
     */
    @GetMapping("commuteSorting")
    public ApiResult commuteSorting(@RequestParam String arrivalRate,@RequestParam String date) {
        if ("ascending".equals(arrivalRate)){
            arrivalRate = arrivalRate.substring(0, 3);
        }else if ("descending".equals(arrivalRate)){
            arrivalRate = arrivalRate.substring(0, 4);
        }
        if ("ascending".equals(date)){
            date = date.substring(0, 3);
        }else if ("descending".equals(date)){
            date = date.substring(0, 4);
        }
        List<CheckResult> checkResult=checkResultService.sortedByCommuteTime(arrivalRate,date);
        return ApiResult.ok("data",checkResult);
    }




}
