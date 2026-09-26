package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 流程任务人员信息
 * 项目流程节点上的接收人与审批角色
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowTaskVo implements Serializable {

    /**
     * 项目ID
     */
    private Integer pId;

    /**
     * 接收人用户ID（多个以逗号分隔）
     */
    private String uId;

    /**
     * 审批角色ID（多个以逗号分隔）
     */
    private String roleId;
}
