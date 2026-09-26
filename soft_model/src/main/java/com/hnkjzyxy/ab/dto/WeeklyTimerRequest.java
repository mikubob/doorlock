package com.hnkjzyxy.ab.dto;


import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 每周定时开锁请求参数
 * 用于配置智能锁的每周定时开关任务
 */
@Data
public class WeeklyTimerRequest {

    /**
     * 锁ID
     */
    @NotNull(message = "锁ID不能为空")
    private Integer lockId;

    /**
     * 小时（0-23）
     */
    @NotNull(message = "小时不能为空")
    private Integer hour;

    /**
     * 分钟（0-59）
     */
    @NotNull(message = "分钟不能为空")
    private Integer minute;

    /**
     * 星期数组（1-7 分别代表周一到周日）
     */
    @NotEmpty(message = "至少选择一天")
    private int[] daysOfWeek;
}
