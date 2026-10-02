package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Flow;
import com.hnkjzyxy.ab.model.ResultItem;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ApproveParam;
import com.hnkjzyxy.ab.params.FlowParam;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.UserParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.vo.FlowQueryVo;
import com.hnkjzyxy.ab.vo.FlowStatusVo;

import java.util.List;
import java.util.Map;

/**
 * 审批流程Service接口
 *
 * @author 16702
 */
public interface FlowService extends IService<Flow> {
    /**
     * 分页查询审批流程
     *
     * @param param 审批流程操作或查询参数
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getFlowList(FlowParam param);

    /**
     * 创建审批流程及其节点
     *
     * @param flow 审批流程信息
     * @param user 当前用户
     */
    void createFlow(Flow flow, User user);

    /**
     * 查询当前用户可审批的项目
     *
     * @param param 审批流程操作或查询参数
     * @param user 当前用户
     * @return 统一接口响应
     */
    ApiResult getProjectApproveList(ProjectParam param, User user);

    /**
     * 查询审批结果列表及分页信息
     *
     * @param param 审批流程操作或查询参数
     * @param user 当前用户
     * @return 查询数据及相关统计信息
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    Map<String, Object> getResultOutcomeList(ApproveParam param, User user) throws Exception;

    /**
     * 查询审批结果明细
     *
     * @param vo 审批流程操作或查询参数
     * @return 统一接口响应
     */
    ApiResult getAppRoveResultDetail(ApproveParam vo);

    /**
     * 提交审批意见及评分
     *
     * @param param 审批明细数据
     * @param user 当前用户
     */
    void submitApprove(ResultItem param, User user);

    /**
     * 按项目ID查询审批流程
     *
     * @param pId 考核项目ID
     * @return 审批流程展示信息
     */
    FlowQueryVo findFlowByPId(String pId);

    /**
     * 查询指定用户的项目结果明细
     *
     * @param integer 项目ID
     * @param userId 用户ID
     * @return 统一接口响应
     */
    ApiResult getProjectResultDetail(Integer integer, Integer userId);

    /**
     * 查询项目审批流程及当前用户的进度
     *
     * @param pId 考核项目ID
     * @param userId 用户ID
     * @return 项目流程状态信息
     */
    FlowStatusVo getProjectFLow(String pId, Integer userId);

    /**
     * 查询用户可审批项目的年度
     *
     * @param user 当前用户
     * @return 查询结果列表
     */
    List<String> getProjectApproveYears(User user);

    /**
     * 查询用户相关审批流程的年度
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    List<String> getFlowYears(Integer userId);

    /**
     * 分页查询项目中尚未提交结果的用户
     *
     * @param queryVo 审批流程操作或查询参数
     * @param user 当前用户
     * @return 查询数据及相关统计信息
     */
    Map<String, Object> getNotSubmittedList(UserParam queryVo, User user);

    /**
     * 更新项目审批流程状态
     *
     * @param pId 考核项目ID
     * @param status 状态值
     */
    void updateProjectFlowStatus(Integer pId, Integer status);
}