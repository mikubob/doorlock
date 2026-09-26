package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.MajorDetails;
import com.hnkjzyxy.ab.result.ApiResult;

/**
 * (MajorDetails)表服务接口
 *
 * @author Binc
 * @since 2025-04-14 13:31:29
 */
public interface MajorDetailsService extends IService<MajorDetails> {
    ApiResult addMajorDetails(MajorDetails majorDetails);

    ApiResult updateMajorDetails(MajorDetails majorDetails);

    ApiResult deleteMajorDetails(Long id);

    ApiResult getMajorDetails();
}

