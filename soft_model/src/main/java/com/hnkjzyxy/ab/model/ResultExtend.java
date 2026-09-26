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
 * 任务结果扩展项
 * 任务表 Task 中 isExtend=1 时，才会记录任务结果扩展项；
 * 按用户ID和任务ID取值
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultExtend implements Serializable {

    /**
     * 扩展项主键ID
     */
    @TableId
    private Integer id;

    /**
     * 用户唯一编号
     */
    private Integer uId;

    /**
     * 任务ID
     */
    @NotNull(message = "任务id不能为空！")
    @JsonAlias("taskId")
    private String taskId;

    /**
     * 关联的结果ID
     */
    private Integer resultId;

    /**
     * 扩展项类别：1：国家级、2：省级、3：校级、4：其他
     */
    @JsonAlias("itemType")
    private String itemType;

    /**
     * 扩展项名称
     */
    @JsonAlias("itemName")
    private String itemName;

    /**
     * 发布时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonAlias("createTime")
    private Date createTime;

    /**
     * 扩展项备注
     */
    @JsonAlias("note")
    private String note;

    /**
     * 评分分数
     */
    @JsonAlias("score")
    private int score;

    /**
     * 佐证材料（格式：[材料路径]）
     */
    private String evidence;

    /**
     * 佐证材料路径列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<String> evidenceList;
}
