package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Construct;
import com.hnkjzyxy.ab.model.ConstructResult;
import com.hnkjzyxy.ab.params.ConstructQueryParam;

import java.util.List;
import java.util.Map;

/**
 * 建设项目Service接口
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/8 15:38
 */
public interface ConstructService extends IService<Construct> {
    /**
     * 新增或更新建设项目及其子项目和参与人
     *
     * @param construct 建设项目信息
     */
    void addConstruct(Construct construct);

    /**
     * 查询建设项目及其子项目和参与人信息
     *
     * @param id 建设项目ID
     * @return 建设项目信息
     */
    Construct getConstructById(String id);

    /**
     * 分页查询建设项目
     *
     * @param param 建设项目操作或查询参数
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getConstructList(ConstructQueryParam param);

    /**
     * 分页查询用户参与的建设项目
     *
     * @param param 建设项目操作或查询参数
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getConstructListByUserId(ConstructQueryParam param);

    /**
     * 分配建设项目参与人
     *
     * @param param 建设项目操作或查询参数
     */
    void assignmentConstruct(ConstructQueryParam param);

    /**
     * 查询用户创建的建设项目年度
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    List<String> getConstructYears(Integer userId);

    /**
     * 查询用户参与的建设项目年度
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    List<String> getConstructYearsByUser(Integer userId);

    /**
     * 提交建设项目完成结果
     *
     * @param result 建设项目提交结果信息
     * @return 建设项目提交结果信息
     */
    ConstructResult subConstructResult(ConstructResult result);
}
