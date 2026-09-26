package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.model.MajorDetails;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.MajorDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 专业方向管理
 * 提供专业方向的增删改查接口
 */
@RestController
@RequestMapping("/majorDetails")
public class MajorDetailsController {
    @Autowired
    private MajorDetailsService majorDetailsService;

    /**
     * 添加专业方向
     *
     * @param majorDetails 专业方向信息
     * @return 操作结果
     */
    @PostMapping
    public ApiResult addMajorDetails(@RequestBody MajorDetails majorDetails) {
        return majorDetailsService.addMajorDetails(majorDetails);
    }

    /**
     * 修改专业方向
     *
     * @param majorDetails 专业方向信息
     * @return 操作结果
     */
    @PutMapping
    public ApiResult updateMajorDetails(@RequestBody MajorDetails majorDetails) {
        return majorDetailsService.updateMajorDetails(majorDetails);
    }

    /**
     * 删除专业方向
     *
     * @param id 专业方向ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult deleteMajorDetails(@PathVariable Long id) {
        return majorDetailsService.deleteMajorDetails(id);
    }

    /**
     * 获取所有专业方向
     *
     * @return 专业方向列表
     */
    @GetMapping
    public ApiResult getMajorDetails() {
        return majorDetailsService.getMajorDetails();
    }
}
