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
    /**
     * 新增专业录取名额信息
     *
     * @param majorDetails 专业录取名额信息
     * @return 统一接口响应
     */
    ApiResult addMajorDetails(MajorDetails majorDetails);

    /**
     * 更新专业录取名额信息
     *
     * @param majorDetails 专业录取名额信息
     * @return 统一接口响应
     */
    ApiResult updateMajorDetails(MajorDetails majorDetails);

    /**
     * 删除专业录取名额信息
     *
     * @param id 专业录取名额ID
     * @return 统一接口响应
     */
    ApiResult deleteMajorDetails(Long id);

    /**
     * 查询专业录取名额信息
     *
     * @return 统一接口响应
     */
    ApiResult getMajorDetails();
}

