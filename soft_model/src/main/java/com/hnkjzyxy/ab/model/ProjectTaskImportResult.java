package com.hnkjzyxy.ab.model;

import lombok.Data;

/**
 * 项目子项合并导入执行结果
 * <p>
 * 提供新增、更新、跳过及保留任务数量，第一阶段固定使用 MERGE 且不删除任务。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
@Data
public class ProjectTaskImportResult {
    /**
     * 本次导入的项目ID
     */
    private Integer projectId;

    /**
     * 导入模式，固定为 MERGE
     */
    private final String mode = "MERGE";

    /**
     * 请求中通过校验的不同来源子项数量
     */
    private int requested;

    /**
     * 本次新增任务数量
     */
    private int created;

    /**
     * 保留原主键并更新的任务数量
     */
    private int updated;

    /**
     * 字段无变化、无需写入的任务数量
     */
    private int skipped;

    /**
     * 删除任务数量，第一阶段固定为0
     */
    private final int deleted = 0;

    /**
     * 保留的无来源映射任务数量，包含 Excel 及尚未确认来源的任务
     */
    private int retainedOtherTasks;
}
