package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程节点人员树（子项）
 * 用于流程审批列表中「接收人 / 审批人」分组的人员列表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowQueryTaskVo {

    /**
     * 分组标题（接收人 / 审批人）
     */
    private String title;

    /**
     * 该分组下的人员名称列表
     */
    private List<String> list;

}
