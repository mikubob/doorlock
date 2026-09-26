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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ResultService extends IService<Result> {
    DataVo columnarChart(HomeParam param, User user);

    Map<String, Object> getList(SubTaskDto dto, User user);

    void updateSubTaskScore(List<SubTaskIdDto> dto, User user);

    Map<String, Object> getLists(SubTaskIdDto dto, User user);

    void downloadEvidenceToWord(String evidences, HttpServletResponse response);


    Map<String, Object> getListByCategoryName(Task param);


    ApiResult getUserResultWeekCount(Integer userId);

    LineVo finishProjectScore(Integer year, User user);

    PieVo finishProjectScale(User user, Integer year);

    List<String> getFinishScaleYears(User user);

    List<String> getFinishProjectYears(User user);

}
