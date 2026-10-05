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
    @TableId(value = "task_id", type = IdType.AUTO)
    private int taskId;

    /**
     * 数据库期望配置版本，回执扣次数不改变此版本。
     */
    private Long rowVersion = 0L;
    /**
     * Quartz 对账状态 pending、synced 或 failed。
     */
    private String quartzSyncStatus;
    /**
     * 最近对账原因，不包含设备密钥。
     */
    private String quartzSyncMessage;
    /**
     * 最近成功对账的学校时间。
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime quartzSyncedAt;

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
     * 任务状态（0=未启用，1=已启用，2=已完成，3=已取消）
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
     * 独立设备通道，不从备注推断。
     */
    private String doorChannel;

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
