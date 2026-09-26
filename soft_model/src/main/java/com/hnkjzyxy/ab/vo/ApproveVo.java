package com.hnkjzyxy.ab.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 审批节点信息
 * 流程信息与流程节点关联查询的结果对象，用于审批列表展示
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApproveVo implements Serializable {

    /**
     * 流程ID
     */
    @TableId
    private Integer id;

    /**
     * 所属项目ID
     */
    private Integer pId;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 流程描述
     */
    @TableField("`describe`")
    private String describe;

    /**
     * 接收人用户ID（多个以逗号分隔）
     */
    private String uId;

    /**
     * 审批角色ID（多个以逗号分隔）
     */
    private String roleId;

    /**
     * 审批步骤（第几步）
     */
    private Integer sort;

}
