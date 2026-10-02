package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 项目任务
 * 记录项目下的任务项、评分标准及分值
 */
@Getter
@Setter
@NoArgsConstructor
public class Task implements Serializable {

    /**
     * 任务唯一ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 项目ID
     */
    private Integer pId;

    /**
     * 来源子项ID，Excel 和未审核的历史任务为空
     */
    private Integer sourceProjectItemId;

    /**
     * 任务类别（分类）
     */
    private String category;

    /**
     * 任务标题
     */
    private String taskName;

    /**
     * 评分标准内容
     */
    private String standard;

    /**
     * 最高分值
     */
    private Integer score;

    /**
     * 是否需要上传佐证材料（0=否，1=是）
     */
    private Integer isFile;

    /**
     * 是否需要扩展项（0=否，1=是）
     */
    private Integer isExtend;

    /**
     * 备注
     */
    private String remark;

    /**
     * 结果（非数据库字段）
     */
    @TableField(exist = false)
    private Result result;

    /**
     * 构造不指定来源子项的任务，保留原有调用方式
     *
     * @param id 任务唯一ID
     * @param pId 项目ID
     * @param category 任务类别（分类）
     * @param taskName 任务标题
     * @param standard 评分标准内容
     * @param score 最高分值
     * @param isFile 是否需要上传佐证材料（0=否，1=是）
     * @param isExtend 是否需要扩展项（0=否，1=是）
     * @param remark 备注
     * @param result 结果（非数据库字段）
     */
    public Task(String id, Integer pId, String category, String taskName, String standard, Integer score, Integer isFile, Integer isExtend, String remark, Result result) {
        this.id = id;
        this.pId = pId;
        this.category = category;
        this.taskName = taskName;
        this.standard = standard;
        this.score = score;
        this.isFile = isFile;
        this.isExtend = isExtend;
        this.remark = remark;
        this.result = result;
    }
}
