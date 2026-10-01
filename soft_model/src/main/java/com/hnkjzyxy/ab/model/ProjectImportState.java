package com.hnkjzyxy.ab.model;

import lombok.Data;

import java.util.Date;

/**
 * 项目任务导入审核与暂存冻结状态
 * <p>
 * 持久化历史来源审核凭据和首次暂存时间，冻结状态不随 Redis 缓存过期而撤销。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
@Data
public class ProjectImportState {
    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 历史来源及旧暂存是否已完成核验（0=待审核，1=已核验）
     */
    private Integer legacyReviewed;

    /**
     * 首次暂存时间，非空时永久冻结任务变更
     */
    private Date firstStagedAt;

    /**
     * 审核报告编号，新建项目使用 created-by-t04 标识
     */
    private String reviewReference;
}
