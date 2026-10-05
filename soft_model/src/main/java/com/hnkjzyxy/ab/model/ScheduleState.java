package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排程版本及最近完整课表覆盖范围，只有成功事务才能更新。
 */
@Data
@TableName("sys_schedule_state")
public class ScheduleState {
    /**
     * 单例主键。
     */
    @TableId(type = IdType.INPUT)
    private Integer id;
    /**
     * 全局排程版本。
     */
    private Long scheduleVersion;
    /**
     * 最近成功发布时刻。
     */
    private LocalDateTime lastSuccess;
    /**
     * 可信覆盖开始日期。
     */
    private LocalDate coverageStart;
    /**
     * 可信覆盖结束日期。
     */
    private LocalDate coverageEnd;
    /**
     * 发布时采用的作息版本。
     */
    private String policyVersion;
}
