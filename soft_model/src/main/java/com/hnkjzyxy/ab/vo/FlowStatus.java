package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 流程审批状态
 * 用于流程进度时间轴展示，包含各步骤的审批人、审批时间与审批意见
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowStatus {

    /**
     * 标题（步骤名称）
     */
    private String title;

    /**
     * 审批人
     */
    private String content;

    /**
     * 审批时间
     */
    private Date timestamp;

    /**
     * 是否完成审批（0=未完成，1=已完成）
     */
    private Integer flag;

    /**
     * 审批意见
     */
    private String opinion;

    /**
     * 审批得分
     */
    private String score;

    /**
     * 历史审批记录
     */
    private List<FlowStatus> history;
}
