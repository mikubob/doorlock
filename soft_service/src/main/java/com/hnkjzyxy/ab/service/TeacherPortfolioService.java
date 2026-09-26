package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.TeachAnalysistHomeParam;
import com.hnkjzyxy.ab.params.TeachHomeParam;
import com.hnkjzyxy.ab.vo.RadarChartVo;

import java.util.List;
import java.util.Map;

public interface TeacherPortfolioService {
    public RadarChartVo getradarChart(TeachHomeParam param, User user,String college);
    public RadarChartVo getChartByTeacher(TeachHomeParam param, User user);
    public  List<Map<String, Map<String, Integer>>>    getChartByTeacher(TeachAnalysistHomeParam param, User user);
}
