package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排程变更审计及同步冲突待处理记录。
 */
@Data
@TableName("sys_schedule_audit")
public class ScheduleAudit {
    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 排程版本。
     */
    private Long scheduleVersion;
    /**
     * 变更类型。
     */
    private String action;
    /**
     * 认证操作者或系统来源。
     */
    private String actor;
    /**
     * 变更及冲突明细。
     */
    private String detail;
    /**
     * 当前冲突处理状态，普通变更为空。
     */
    private String issueStatus;
    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
