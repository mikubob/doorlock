package com.hnkjzyxy.ab.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 课表同步执行结果
 * <p>
 * 用于向调用方（定时任务 / 手动刷新接口 / 启动执行器）回报本次同步的详细情况，
 * 便于日志排查与前端提示。
 * </p>
 *
 * @version 1.0
 * @date 2026-09-29
 */
@Data
public class CourseScheduleSyncResult implements Serializable {

    /**
     * 序列化版本标识
     */
    private static final long serialVersionUID = 1L;

    /**
     * 是否真正执行了同步流程（开关关闭、未抢到锁时为 false）
     */
    private boolean executed;

    /**
     * 是否同步成功
     */
    private boolean success;

    /**
     * 本次从 OA 拉取到的原始记录数
     */
    private int totalRows;

    /**
     * 通过字段校验、待写入的记录数
     */
    private int validRows;

    /**
     * 实际写入数据库的条数
     */
    private int savedRows;

    /**
     * 替换前表内记录数
     */
    private int previousRows;

    /**
     * 本次耗时（毫秒）
     */
    private long costMillis;

    /**
     * 结论说明
     */
    private String message;

    /**
     * 构造一个「未执行」的结果
     *
     * @param message 跳过原因
     * @return 同步结果
     */
    public static CourseScheduleSyncResult skipped(String message) {
        CourseScheduleSyncResult result = new CourseScheduleSyncResult();
        result.setExecuted(false);
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
