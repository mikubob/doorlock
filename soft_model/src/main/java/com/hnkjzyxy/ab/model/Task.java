package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 项目任务
 * 记录项目下的任务项、评分标准及分值
 */
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
     * 获取来源子项ID
     *
     * @return 来源子项ID，无来源映射时返回 null
     */
    public Integer getSourceProjectItemId() {
        return sourceProjectItemId;
    }

    /**
     * 设置经过核验的来源子项ID
     *
     * @param sourceProjectItemId 来源子项ID
     */
    public void setSourceProjectItemId(Integer sourceProjectItemId) {
        this.sourceProjectItemId = sourceProjectItemId;
    }

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

    public Task() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPId() {
        return pId;
    }

    public void setPId(Integer pId) {
        this.pId = pId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getIsFile() {
        return isFile;
    }

    public void setIsFile(Integer isFile) {
        this.isFile = isFile;
    }

    public Integer getIsExtend() {
        return isExtend;
    }

    public void setIsExtend(Integer isExtend) {
        this.isExtend = isExtend;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
