package com.hnkjzyxy.ab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.model.CheckResult;
import com.hnkjzyxy.ab.vo.CheckResultStatisticsVo;
import com.hnkjzyxy.ab.model.Course;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.CheckResultService;
import com.hnkjzyxy.ab.service.CourseService;
import com.hnkjzyxy.ab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 课程管理
 * 提供课程数据的上传与查询，以及学院/班级维度的缺勤率统计分析接口
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/4/24 14:56
 */
@RestController
@RequestMapping("/course")
public class CourseController {
    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;

    /**
     * 课程业务服务
     */
    @Autowired
    private CourseService courseService;
    /**
     * 教学巡查结果业务服务
     */
    @Autowired
    private CheckResultService checkResultService;


    /**
     * 查询课程巡查列表
     *
     * @param resultVo 课程查询条件（可选）
     * @param authentication 当前登录认证信息
     * @return 课程列表
     */
    @PostMapping("/list")
    public ApiResult checkResult(@RequestBody(required = false) Course resultVo, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());

        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserName, user.getUserName());
        User one = userService.getOne(queryWrapper);
        List<Course> vo = courseService.getList(resultVo, one);

        return ApiResult.ok("data", vo);
    }

    /**
     * 上传课程数据
     * 解析 Excel 导入课程，管理员可选择先清空原有课程数据
     *
     * @param file     课程数据 Excel 文件
     * @param isDelete 是否清空原有课程数据（1=清空后重新导入）
     * @param authentication 当前登录认证信息
     * @return 操作结果
     */
    @PostMapping("/upload")
    //@RepeatSubmit
    public ApiResult uploadCourse(@RequestParam("file") MultipartFile file, @RequestParam int isDelete, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());

        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        if (isDelete == 1) {
            //删除所有数据库内容
            courseService.updateAllState();
            LambdaQueryWrapper<Course> courseQueryWrapper = new LambdaQueryWrapper<>();
            courseQueryWrapper.eq(Course::getState, 0);
            courseService.remove(courseQueryWrapper);
        }
        courseService.uploadCourseFile(file, user);


        return ApiResult.ok("上传成功！");
    }


    /**
     * 获取班级列表
     *
     * @param authentication 当前登录认证信息
     * @return 当前用户可见的班级名称列表
     */
    @GetMapping("/classes/list")
    public ApiResult getClassesList(Authentication authentication) {
        String userName = authentication.getName();
        // 直接查询数据库，绕过缓存
        User directUser = userService.getOne(new QueryWrapper<User>().eq("user_name", userName));
        List<String> classesList = courseService.getClassesList(directUser);
        return ApiResult.ok("data", classesList);
    }

    /**
     * 获取教室列表
     *
     * @return 教室名称列表
     */
    @GetMapping("/classroom/list")
    public ApiResult getClassroomList() {
        List<String> classesList = courseService.getclassroomList();
        return ApiResult.ok("data", classesList);
    }

    /**
     * 获取任课教师列表
     *
     * @return 任课教师名称列表
     */
    @GetMapping("/teacher/list")
    public ApiResult getTeacherList() {
        List<String> classesList = courseService.getTeacherList();
        return ApiResult.ok("data", classesList);
    }

    /**
     * 获取辅导员列表
     *
     * @return 辅导员名称列表
     */
    @GetMapping("/counsellor/list")
    public ApiResult getCounsellorList() {
        List<String> classesList = courseService.getCounsellorList();
        return ApiResult.ok("data", classesList);
    }

    /**
     * -----------------------------------------------------------------------------------
     * 图表功能特定接口
     */

    /**
     * 学院课程巡查综合分析
     * 按时间范围统计学院内课程的缺勤率、带食物率及请假人次
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param college   学院名称
     * @param authentication 当前登录认证信息
     * @return 学院课程巡查汇总统计
     */
    @GetMapping("/getcollegecoursecnalysis")
    public ApiResult getCollegeCourseAnalysis(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "college", required = false) String college,
            Authentication authentication
    ) {
        String name = authentication.getName();
        User user = userService.getUserByName(name);
        //应到人数
        int shouldArrival = 0;
        //实到人数
        int arrival = 0;
        //请假人次
        int numberLeaveRequests = 0;
        //带食物率
        int foodCarryingRate = 0;

        //先获取时间范围内的学院课程数据
        for (CheckResult checkResult : checkResultService.findByDateRangeAndCollege(startDate, endDate, college,user)) {
            //统计相应数据库
            //1.应到人数
            shouldArrival += checkResult.getShouldArrival();
            //2.实到人数
            arrival += checkResult.getArrival();
            //3.请假人次
            numberLeaveRequests += checkResult.getPeopleLeave();
            //4.带食物人数
            foodCarryingRate += checkResult.getFoodBringPerson();
        }
        CheckResultStatisticsVo checkResultResponse = new CheckResultStatisticsVo();
        // 缺勤率计算
        if (shouldArrival > 0) {
            double absenteeismRate = (shouldArrival - arrival) * 100.0 / shouldArrival;
            // 保留2位小数
            checkResultResponse.setAbsenteeismRate(String.format("%.2f%%", absenteeismRate));
        } else {
            checkResultResponse.setAbsenteeismRate("0%"); // 避免除零错误
        }
        //请假人数
        checkResultResponse.setNumberLeaveRequests(String.valueOf(numberLeaveRequests));

        // 带食物率计算
        if (arrival > 0) {
            double foodRate = foodCarryingRate * 100.0 / arrival;
            checkResultResponse.setFoodCarryingRate(String.format("%.2f%%", foodRate));
        } else {
            checkResultResponse.setFoodCarryingRate("0%");
        }

        return ApiResult.ok("data", checkResultResponse);
    }

    /**
     * 缺勤率最高的班级排行（TOP 10）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param college   学院名称
     * @param authentication 当前登录认证信息
     * @return 缺勤率最高的 10 个班级
     */
    @GetMapping("/gethighestabsenteeismrate")
    public ApiResult getHighestAbsenteeismRate(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "college", required = false) String college,
            Authentication authentication) {
        String name = authentication.getName();
        User user = userService.getUserByName(authentication.getName());
        List<CheckResult> byDateRangeAndCollege = checkResultService.findByDateRangeAndCollege(startDate, endDate, college,user);

        // 使用Map进行分组统计
        Map<String, ClassStatistics> classStatsMap = new HashMap<>();

        for (CheckResult checkResult : byDateRangeAndCollege) {
            String className = checkResult.getClasses();
            ClassStatistics stats = classStatsMap.getOrDefault(className, new ClassStatistics());
            stats.addShouldArrival(checkResult.getShouldArrival());
            stats.addArrival(checkResult.getArrival());
            classStatsMap.put(className, stats);
        }

        // 计算缺勤率并收集结果
        List<Map<String, String>> lookClasses = classStatsMap.entrySet().stream()
                .map(entry -> {
                    String className = entry.getKey();
                    ClassStatistics stats = entry.getValue();
                    Map<String, String> map = new HashMap<>();
                    map.put("className", className);

                    if (stats.shouldArrival > 0) {
                        double absenteeismRate = (stats.shouldArrival - stats.arrival) * 100.0 / stats.shouldArrival;
                        map.put("absenteeismRate", String.format("%.2f%%", absenteeismRate));
                    } else {
                        map.put("absenteeismRate", "0%");
                    }
                    return map;
                })
                .collect(Collectors.toList());

        // 按缺勤率降序排序并取前10名
        List<Map<String, String>> top10 = lookClasses.stream()
                .sorted((a, b) -> {
                    double rateA = Double.parseDouble(a.get("absenteeismRate").replace("%", ""));
                    double rateB = Double.parseDouble(b.get("absenteeismRate").replace("%", ""));
                    return Double.compare(rateB, rateA); // 降序排序
                })
                .limit(10)
                .collect(Collectors.toList());

        return ApiResult.ok("data", top10);
    }

    /**
     * 按月统计各班级缺勤率
     *
     * 返回数据结构示例：
     * <pre>
     *  [{
     *    "absenceRate": 0.00,        // 缺勤率：0.00% 表示该月没有缺勤
     *    "month": 11,                // 月份
     *    "totalArrival": 102,        // 总实到人数
     *    "totalAbsence": 0,          // 总缺勤人数
     *    "className": "软件游戏3232班", // 班级名称
     *    "totalShouldArrival": 102   // 总应到人数
     *  }]
     *  </pre>
     *
     * @param year    年份
     * @param college 学院名称
     * @param authentication 当前登录认证信息
     * @return 按班级分组的月度缺勤明细
     */
    @GetMapping("/getmonthlyabsenteeismrate")
    public ApiResult getMonthlyAbsenteeismRate(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "college", required = false) String college,
            Authentication authentication) {
        String name = authentication.getName();
        User user = userService.getUserByName(authentication.getName());
        List<Map<String, Object>> monthlyAbsenceDetailByCollege =
                checkResultService.getMonthlyAbsenceDetailByCollege(year, college,user);

        // 使用Stream按班级分类数据
        Map<String, List<Map<String, Object>>> classifiedData =
                monthlyAbsenceDetailByCollege.stream()
                        .collect(Collectors.groupingBy(
                                item -> (String) item.get("className"),
                                Collectors.mapping(item -> {
                                    Map<String, Object> data = new HashMap<>(item);
                                    data.remove("className"); // 移除重复的班级名称
                                    return data;
                                }, Collectors.toList())
                        ));

        return ApiResult.ok("data", classifiedData);
    }

    /**
     * 按月、学院分析教师授课班级缺勤率排行
     * 用于学院间对比分析
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param college   学院名称
     * @param authentication 当前登录认证信息
     * @return 缺勤率最高的 10 个班级
     */
    @GetMapping("/getabsenceclassesbycollege")
    ApiResult getAbsenceClassesByCollege(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "college", required = false) String college,
            Authentication authentication) {
        String name = authentication.getName();
        User user = userService.getUserByName(authentication.getName());
        List<Map<String, Object>> absenceClassesByCollegeAndDateRange = checkResultService.getAbsenceClassesByCollegeAndDateRange(startDate, endDate, college,user);
        return ApiResult.ok("data", absenceClassesByCollegeAndDateRange);


    }
    /**
     * 获取学院列表
     * 管理员返回全部学院，其他用户仅返回本人所属学院
     *
     * @param authentication 当前登录认证信息
     * @return 学院名称列表
     */
    @GetMapping("/getallcolleges")
    ApiResult getAllColleges(Authentication authentication) {
        String name = authentication.getName();
        User userByName = userService.getUserByName(authentication.getName());
        String college = userByName.getCollege();
        String nickName = userByName.getNickName();
        if (nickName.equals("管理员")) {
            return ApiResult.ok("data", checkResultService.getAllColleges());
        }
        ArrayList<String> objects = new ArrayList<>();
        objects.add(college);
        return ApiResult.ok("data", objects);

    }

    /**
     * 按时间范围和学院批量查询巡查统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param colleges  学院名称列表
     * @return 学院巡查统计数据
     */
    @GetMapping("/getCollegeStatistics")
    ApiResult getCollegeStatistics(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("colleges") List<String> colleges) {
        return ApiResult.ok("data", checkResultService.getCollegeStatistics(startDate, endDate, colleges));
    }

    /**
     * 班级数据统计辅助类
     */
    private static class ClassStatistics {
        /**
         * 课程统计应到人数累计值
         */
        private int shouldArrival = 0;
        /**
         * 课程统计实到人数累计值
         */
        private int arrival = 0;

        /**
         * 累加应到人数
         *
         * @param count 需要累加的数量
         */
        public void addShouldArrival(int count) {
            this.shouldArrival += count;
        }

        /**
         * 累加实到人数
         *
         * @param count 需要累加的数量
         */
        public void addArrival(int count) {
            this.arrival += count;
        }
    }
    /**
     * 获取全部学院列表
     *
     * @return 学院名称列表
     */
    @GetMapping("/getCollege")
    public ApiResult getColege(){
        List<String> colege = courseService.getColege();
        return ApiResult.ok("data",colege);
    }
    /**
     * 根据学院获取班级列表
     *
     * @param college 学院名称
     * @return 该学院下的班级名称列表
     */
    @GetMapping("/getClass")
    public ApiResult getClass(String college){
        List<String> classes = courseService.getClassByCollege(college);
        return ApiResult.ok("data",classes);
    }
}
