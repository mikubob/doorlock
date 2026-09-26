package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Construct;
import com.hnkjzyxy.ab.model.ConstructResult;
import com.hnkjzyxy.ab.params.ConstructQueryParam;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/5/8 15:38
 */
public interface ConstructService extends IService<Construct> {
    void addConstruct(Construct construct);

    Construct getConstructById(String id);

    Map<String, Object> getConstructList(ConstructQueryParam param);

    Map<String, Object> getConstructListByUserId(ConstructQueryParam param);

    void assignmentConstruct(ConstructQueryParam param);

    List<String> getConstructYears(Integer userId);

    List<String> getConstructYearsByUser(Integer userId);

    ConstructResult subConstructResult(ConstructResult result);
}
