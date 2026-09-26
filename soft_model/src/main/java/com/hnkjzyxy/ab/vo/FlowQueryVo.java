package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程节点人员树（父项）
 * 用于流程审批列表中按步骤展示接收人与审批人
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowQueryVo {

    /**
     * 标题（流程步骤名称）
     */
    private String title;

    /**
     * 子节点（接收人 / 审批人分组）
     */
    private List<FlowQueryTaskVo> children;

}
