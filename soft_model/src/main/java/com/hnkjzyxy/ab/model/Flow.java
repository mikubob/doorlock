package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 流程信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Flow implements Serializable {

    /**
     * 流程ID
     */
    @TableId
    private Integer id;

    /**
     * 所属用户ID
     */
    private Integer userId;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    @JsonAlias("pId")
    private Integer pId;

    /**
     * 流程描述
     */
    @TableField("`describe`")
    private String describe;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 状态
     */
    @TableLogic
    private Integer status;

    /**
     * 流程任务列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<FlowTask> flowTask;

}
