package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 建设项目
 * 记录建设项目的任务分解与分配关系
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/5/8 15:34
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Construct {

    /**
     * 建设项目ID
     */
    @TableId
    private Integer id;

    /**
     * 所属项目ID
     */
    private Integer pId;

    /**
     * 负责人用户ID
     */
    private Integer uId;

    /**
     * 任务标题
     */
    private String titleName;

    /**
     * 是否为任务节点（0=否，1=是）
     */
    private Integer isTask;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 建设目标
     */
    private String target;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 状态
     */
    @TableLogic
    private Integer status;

    /**
     * 子任务列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Construct> children;

    /**
     * 成果列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<ConstructResult> outcomesList;

    /**
     * 分配用户ID列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Integer> uIds;

}
