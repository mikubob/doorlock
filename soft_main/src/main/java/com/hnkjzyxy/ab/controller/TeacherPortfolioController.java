package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.TeachAnalysistHomeParam;
import com.hnkjzyxy.ab.params.TeachHomeParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ProjectService;
import com.hnkjzyxy.ab.service.TeacherPortfolioService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.vo.RadarChartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 教学画像统计
 * 提供教学成果雷达图、按教师/学院维度的统计与项目查询接口
 */
@RestController
@RequestMapping("/teach/count")
public class TeacherPortfolioController {
    @Autowired
    UserService userService;
    @Autowired
    ProjectService projectService;
    @Autowired
    TeacherPortfolioService teacherPortfolioService;
    /**
     * 获取教学成果雷达图
     *
     * @param param 教学统计查询条件（含学院）
     * @return 雷达图数据
     */
    @PostMapping("/teachAll")
    public ApiResult getRadarChart(@RequestBody TeachHomeParam  param, Authentication authentication){
        User user=userService.getUserByName(authentication.getName());
        if(Objects.isNull(user)){
            throw new RuntimeException("用户不能为空！");
        }
        RadarChartVo radarChartVo = teacherPortfolioService.getradarChart(param, user, param.getCollege());
        return ApiResult.ok("data",radarChartVo);

    }


    /**
     * 按老师统计雷达图
     *
     * @param param 教学统计查询条件
     * @return 雷达图数据
     */
    @PostMapping("/radar/chart/teacher")
    public ApiResult getRadarByTeacher(@RequestBody TeachHomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        RadarChartVo vo = teacherPortfolioService.getChartByTeacher(param, user);
        return ApiResult.ok("data", vo);
    }

    /**
     * 根据年份和学院查询项目列表
     *
     * @param year    年份
     * @param college 学院名称
     * @return 项目列表
     */
    @GetMapping("/year/project")
    public ApiResult getYearProjectList(@RequestParam("year") String year,@RequestParam("college") String  college, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<Project> projects = projectService.getProjectAndCollegeByYear(year,college ,user);
        return ApiResult.ok("data", projects);
    }

    /**
     * 按教师维度分析雷达图数据
     *
     * @param param 教学统计查询条件
     * @return 以教师分组的雷达图数据
     */
    @PostMapping("/radar/chart/analysisteacher")
    public ApiResult analysisteacher(@RequestBody TeachAnalysistHomeParam param, Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<Map<String, Map<String, Integer>>>  chartByTeacher = teacherPortfolioService.getChartByTeacher(param, user);
        return ApiResult.ok("data", chartByTeacher);
    }
    /**
     * 根据学院查询教师雷达图数据
     *
     * @param college 学院名称
     * @return 该学院的教师数据
     */
    @GetMapping("/radar/chart/Teacherbycollege")
    public ApiResult getRadarChart(@RequestParam("college") String college, Authentication authentication) {
        return ApiResult.ok("data",userService.getUserByCollege(college) );
    }
}
