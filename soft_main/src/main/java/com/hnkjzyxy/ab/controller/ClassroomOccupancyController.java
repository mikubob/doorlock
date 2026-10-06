package com.hnkjzyxy.ab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.mapper.ScheduleAuditMapper;
import com.hnkjzyxy.ab.model.ScheduleAudit;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.ClassroomOccupancyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 教室排程快照只读接口。
 */
@RestController
@RequestMapping("/classroomOccupancy")
public class ClassroomOccupancyController {
    /**
     * 占用查询服务。
     */
    private final ClassroomOccupancyService service;
    /**
     * 当前冲突及变更历史。
     */
    private final ScheduleAuditMapper auditMapper;

    /**
     * 创建查询控制器。
     * @param service 占用查询服务
     * @param auditMapper 审计访问
     */
    public ClassroomOccupancyController(ClassroomOccupancyService service, ScheduleAuditMapper auditMapper) {
        this.service = service; this.auditMapper = auditMapper;
    }

    /**
     * 查看当前待处理问题；修正独立课程/考试后自动复核关闭旧版本问题。
     * @return 最近二百条当前问题
     */
    @GetMapping("/issues")
    @PreAuthorize("hasRole('admin')")
    public ApiResult issues() {
        return ApiResult.ok("data", auditMapper.selectList(new LambdaQueryWrapper<ScheduleAudit>()
                .eq(ScheduleAudit::getIssueStatus, "OPEN")
                .orderByDesc(ScheduleAudit::getId).last("LIMIT 200")));
    }

    /**
     * 获取授权管理端快照；设备专用凭证及范围配置完成前使用已认证管理端。
     * @param classroomId 可选内部教室主键
     * @param boardSn 可选班牌设备身份
     * @return 一致快照
     */
    @GetMapping("/snapshot")
    @PreAuthorize("hasRole('admin')")
    public ApiResult snapshot(@RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String boardSn) {
        return ApiResult.ok("data", service.snapshot(classroomId, boardSn));
    }

    /**
     * 读取排考目标日期范围的完整安排，查询不触发 OA 同步。
     *
     * @param classroomId 内部教室主键
     * @param from 开始学校日期
     * @param to 结束学校日期
     * @return 已知安排和可信度问题
     */
    @GetMapping("/schedule")
    @PreAuthorize("hasRole('admin')")
    public ApiResult schedule(@RequestParam Long classroomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResult.ok("data", service.schedule(classroomId, from, to));
    }
}
