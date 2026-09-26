package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时开关锁任务
 */
@Data
public class ScheduleTask {

    /**
     * 任务ID
     */
    @TableId(value = "taskId", type = IdType.AUTO)
    private int taskId;

    /**
     * 锁ID
     */
    private int lockId;

    /**
     * 用户ID
     */
    private int userId;

    /**
     * 定时操作（0=关锁，1=开锁）
     */
    private int timedOperation;

    /**
     * 任务详情
     */
    private String taskDetails;

    /**
     * 任务状态（0=未启用，1=已启用，3=已取消）
     */
    private int taskStatus;

    /**
     * 是否循环（0=否，1=是）
     */
    private int isLoop;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 执行小时
     */
    private int hour;

    /**
     * 执行分钟
     */
    private int minute;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 重复执行的星期（0-6 数组）
     */
    private String countDay;

    /**
     * 重复次数
     */
    private int loopCount;
}
