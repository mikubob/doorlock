package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 项目结果
 * 记录用户针对某个项目任务提交的成果与得分
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result implements Serializable {

    /**
     * 结果ID
     */
    @TableId
    private Integer id;

    /**
     * 用户唯一编号
     */
    private Integer uId;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空！")
    @JsonAlias("pId")
    private Integer pId;

    /**
     * 任务ID
     */
    @NotNull(message = "任务id不能为空！")
    @JsonAlias("taskId")
    private String taskId;

    /**
     * 评分分数
     */
    private int score;

    /**
     * 佐证材料（格式：[材料路径]）
     */
    private String evidence;

    /**
     * 佐证材料列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<String> evidenceList;

    /**
     * 佐证材料数量
     */
    private Integer evidenceCount;

    /**
     * 步骤
     */
    private Integer step;

    /**
     * 提交时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否完成答题（0=否，1=是）
     */
    private Integer isFinish;

    /**
     * 当前用户审批结果（非数据库字段）
     */
    @TableField(exist = false)
    private ResultItem resultItem;

    /**
     * 所有审批成绩（非数据库字段）
     */
    @TableField(exist = false)
    private List<ResultItem> resultItems;

    /**
     * 用户昵称（非数据库字段）
     */
    @TableField(exist = false)
    private String nickName;

    /**
     * 用户名（工号，非数据库字段）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 专业（非数据库字段）
     */
    @TableField(exist = false)
    private String major;

    /**
     * 佐证材料扩展记录（非数据库字段）
     */
    @TableField(exist = false)
    private List<ResultExtend> resultExtends;

    /**
     * 任务列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Task> taskList;

    /**
     * 任务（非数据库字段）
     */
    @TableField(exist = false)
    private Task task;
}
