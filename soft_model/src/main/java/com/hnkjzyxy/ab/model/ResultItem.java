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
 * 项目结果审批项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultItem implements Serializable {

    /**
     * 审批项ID
     */
    @TableId
    private Integer id;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    @JsonAlias("pId")
    private Integer pId;

    /**
     * 用户ID
     */
    @JsonAlias("uId")
    private Integer uId;

    /**
     * 审批分数
     */
    @JsonAlias("score")
    private String score;

    /**
     * 审批步骤
     */
    @JsonAlias("step")
    private Integer step;

    /**
     * 审批时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 审批人ID
     */
    @JsonAlias("userId")
    private Integer userId;

    /**
     * 状态
     */
    @JsonAlias("status")
    @TableLogic
    private Integer status;

    /**
     * 审批人昵称（非数据库字段）
     */
    @TableField(exist = false)
    private String AppRoveName;

    /**
     * 审批意见
     */
    private String opinion;

    /**
     * 是否打回（0=否，1=是）
     */
    @NotNull(message = "是否打回不能为空！")
    private Integer isFlag;

    /**
     * 结果列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Result> results;

}
