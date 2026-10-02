package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.TeachAnalysistHomeParam;
import com.hnkjzyxy.ab.params.TeachHomeParam;
import com.hnkjzyxy.ab.vo.RadarChartVo;

import java.util.List;
import java.util.Map;

/**
 * 教师档案统计Service接口
 */
public interface TeacherPortfolioService {
    /**
     * 按学院和当前用户范围生成教师档案雷达图
     *
     * @param param 教师档案统计操作或查询参数
     * @param user 当前用户
     * @param college 学院名称
     * @return 雷达图统计数据
     */
    public RadarChartVo getradarChart(TeachHomeParam param, User user,String college);
    /**
     * 按教师查询条件生成档案统计数据
     *
     * @param param 教师档案统计操作或查询参数
     * @param user 当前用户
     * @return 雷达图统计数据
     */
    public RadarChartVo getChartByTeacher(TeachHomeParam param, User user);
    /**
     * 按教师查询条件生成档案统计数据
     *
     * @param param 教师档案统计操作或查询参数
     * @param user 当前用户
     * @return 查询结果列表
     */
    public  List<Map<String, Map<String, Integer>>>    getChartByTeacher(TeachAnalysistHomeParam param, User user);
}
