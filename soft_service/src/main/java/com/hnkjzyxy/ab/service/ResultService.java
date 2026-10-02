package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.dto.SubTaskDto;
import com.hnkjzyxy.ab.dto.SubTaskIdDto;
import com.hnkjzyxy.ab.model.Result;
import com.hnkjzyxy.ab.model.Task;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.HomeParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.vo.DataVo;
import com.hnkjzyxy.ab.vo.LineVo;
import com.hnkjzyxy.ab.vo.PieVo;

import java.util.List;
import java.util.Map;

/**
 * 考核结果Service接口
 */
public interface ResultService extends IService<Result> {
    /**
     * 按当前用户的角色范围生成项目评分柱状图
     *
     * @param param 考核结果操作或查询参数
     * @param user 当前用户
     * @return 柱状图统计数据
     */
    DataVo columnarChart(HomeParam param, User user);

    /**
     * 查询子任务考核结果及分页信息
     *
     * @param dto 考核结果操作或查询参数
     * @param user 当前用户
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getList(SubTaskDto dto, User user);

    /**
     * 校验结果归属和评分范围后更新子任务分数
     *
     * @param dto 考核结果操作或查询参数
     * @param user 当前用户
     */
    void updateSubTaskScore(List<SubTaskIdDto> dto, User user);

    /**
     * 查询指定子任务结果及关联明细
     *
     * @param dto 考核结果操作或查询参数
     * @param user 当前用户
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getLists(SubTaskIdDto dto, User user);


    /**
     * 查询项目指定分类下的任务名称
     *
     * @param param 考核结果操作或查询参数
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getListByCategoryName(Task param);


    /**
     * 汇总用户本周每天完成的结果数量
     *
     * @param userId 用户ID
     * @return 统一接口响应
     */
    ApiResult getUserResultWeekCount(Integer userId);

    /**
     * 生成指定年度的项目完成评分折线图
     *
     * @param year 统计年度
     * @param user 当前用户
     * @return 折线图统计数据
     */
    LineVo finishProjectScore(Integer year, User user);

    /**
     * 生成指定年度的项目完成占比饼图
     *
     * @param user 当前用户
     * @param year 统计年度
     * @return 饼图统计数据
     */
    PieVo finishProjectScale(User user, Integer year);

    /**
     * 查询完成占比统计可选年度
     *
     * @param user 当前用户
     * @return 查询结果列表
     */
    List<String> getFinishScaleYears(User user);

    /**
     * 查询项目完成评分统计可选年度
     *
     * @param user 当前用户
     * @return 查询结果列表
     */
    List<String> getFinishProjectYears(User user);

}
