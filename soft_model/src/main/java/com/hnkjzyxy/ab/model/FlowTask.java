package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 审批流程节点
 * 描述项目审批流程中每个审批步骤的接收人、审批人及起止时间
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowTask implements Serializable {

    /**
     * 流程节点ID
     */
    @TableId
    private Integer id;

    /**
     * 审批步骤（第几步，从 1 开始）
     */
    private Integer sort;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 接收人用户ID（多个以逗号分隔）
     */
    private String uId;

    /**
     * 接收人用户ID列表（非数据库字段）
     */
    @TableField(exist = false)
    @JsonAlias("uIds")
    private List<Integer> uIds;

    /**
     * 审批角色ID（多个以逗号分隔）
     */
    private String roleId;

    /**
     * 审批角色ID列表（非数据库字段）
     */
    @TableField(exist = false)
    @JsonAlias("roleIds")
    private List<Integer> roleIds;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空！")
    private Date startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空！")
    private Date endTime;

    /**
     * 流程父节点ID
     */
    private Integer parentId;

    /**
     * 所属项目ID（非数据库字段）
     */
    @TableField(exist = false)
    private Integer projectId;

    /**
     * 所属项目名称（非数据库字段）
     */
    @TableField(exist = false)
    private String projectName;

    /**
     * 状态（逻辑删除：0=正常，1=已删除）
     */
    @TableLogic
    private Integer status;

}
