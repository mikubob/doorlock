package com.hnkjzyxy.ab.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 流程审批状态树
 * 按流程步骤组织的审批状态列表
 */
@Data
public class FlowStatusVo implements Serializable {

    /**
     * 标题（步骤名称）
     */
    private String title;

    /**
     * 该步骤下的审批状态明细
     */
    private List<FlowStatus> children;

}
