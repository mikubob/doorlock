package com.hnkjzyxy.ab.controller;

import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ExamRecordService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.vo.ExamRecordQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考试操作记录；沿用开锁记录的登录用户读取权限。
 */
@RestController
public class ExamRecordController {
    /**
     * 考试操作查询服务。
     */
    private final ExamRecordService records;
    /**
     * 登录用户校验服务，与开锁记录一致。
     */
    private final UserService users;

    /**
     * 创建操作记录控制器。
     * @param records 操作记录
     * @param users 用户访问
     */
    public ExamRecordController(ExamRecordService records, UserService users) {
        this.records = records; this.users = users;
    }

    /**
     * 查询考试操作历史；仅提供读取，不允许修改历史。
     * @param query 查询条件
     * @param authentication 当前登录身份
     * @return 分页记录
     */
    @GetMapping("/smart/examRecord")
    @PreAuthorize("isAuthenticated()")
    public ApiResult list(ExamRecordQuery query, Authentication authentication) {
        if (users.getUserByName(authentication.getName()) == null) throw new IllegalArgumentException("用户不能为空！");
        return ApiResult.ok("data", records.list(query));
    }
}
