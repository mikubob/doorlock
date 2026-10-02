package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.HomeParam;
import com.hnkjzyxy.ab.vo.RadarChartVo;
import com.hnkjzyxy.ab.vo.TeacherAndDepartmentVo;

/**
 * 首页统计服务
 * 提供首页雷达图、柱状图等考核统计数据的查询能力
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-17 10:58
 */
public interface HomeService {


    /**
     * 获得所有分类下的用户的平均值
     *
     * @param param 查询条件
     * @param user  当前用户
     * @return 雷达图数据
     */
    RadarChartVo radarChart(HomeParam param, User user);


    /**
     * 根据老师获得分数
     *
     * @param param 查询条件
     * @param user  当前用户
     * @return 雷达图数据
     */
    RadarChartVo getChartByTeacherOrDepartment(HomeParam param, User user);

    /**
     * 查询项目涉及的教师及教研室信息
     *
     * @param projectId 考核项目ID
     * @return 教师及教研室信息
     */
    TeacherAndDepartmentVo getTeacherAndDepartment(Integer projectId);
}
