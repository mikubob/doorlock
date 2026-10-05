package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备命令及厂家回执，不能用提交成功推断物理门状态。
 */
@Data
@TableName("sys_lock_command")
public class LockCommand {
    /**
     * 命令 ID。
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 调用幂等身份。
     */
    private String requestId;
    /**
     * 真实设备主键。
     */
    private Integer lockId;
    /**
     * 提交时的教室绑定快照，不随设备之后的绑定变更改写。
     */
    private Long classroomId;
    /**
     * 可选来源任务。
     */
    private Integer taskId;
    /**
     * 发送时绑定快照。
     */
    private String deviceSn;
    /**
     * 设备通道。
     */
    private String doorChannel;
    /**
     * 开或关。
     */
    private Integer operation;
    /**
     * submitted / acknowledged / failed / unknown。
     */
    private String status;
    /**
     * 认证用户。
     */
    private String actor;
    /**
     * 故障原因。
     */
    private String errorMessage;
    /**
     * 提交时刻。
     */
    private LocalDateTime createdTime;
    /**
     * 回执时刻。
     */
    private LocalDateTime completedTime;
}
