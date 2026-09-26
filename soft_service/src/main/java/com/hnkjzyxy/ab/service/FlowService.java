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
 * @author 16702
 */
public interface FlowService extends IService<Flow> {
    Map<String, Object> getFlowList(FlowParam param);

    void createFlow(Flow flow, User user);

    ApiResult getProjectApproveList(ProjectParam param, User user);

    Map<String, Object> getResultOutcomeList(ApproveParam param, User user) throws Exception;

    ApiResult getAppRoveResultDetail(ApproveParam vo);

    void submitApprove(ResultItem param, User user);

    FlowQueryVo findFlowByPId(String pId);

    ApiResult getProjectResultDetail(Integer integer, Integer userId);

    FlowStatusVo getProjectFLow(String pId, Integer userId);

    List<String> getProjectApproveYears(User user);

    List<String> getFlowYears(Integer userId);

    Map<String, Object> getNotSubmittedList(UserParam queryVo, User user);

    void updateProjectFlowStatus(Integer pId, Integer status);
}