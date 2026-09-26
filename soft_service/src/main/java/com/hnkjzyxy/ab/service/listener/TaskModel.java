package com.hnkjzyxy.ab.service.listener;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-05 19:02
 */
public class TaskModel {

    @ExcelProperty(value = "类别", index = 1)
    private String category;
    @ExcelProperty(value = "子任务", index = 2)
    private String taskName;
    @ExcelProperty(value = "计分标准", index = 3)
    private String standard;
    @ExcelProperty(value = "满分", index = 4)
    private Integer score;
    @ExcelProperty(value = "是否需要文件上传", index = 5)
    private Integer isFile;

    @ExcelProperty(value = "是否需要扩展项", index = 6)
    private Integer isExtend;

    @ExcelProperty(value = "备注", index = 7)
    private String remark;


    public TaskModel() {
    }

    public TaskModel(String category, String taskName, String standard, Integer score, Integer isFile, Integer isExtend, String remark) {
        this.category = category;
        this.taskName = taskName;
        this.standard = standard;
        this.score = score;
        this.isFile = isFile;
        this.isExtend = isExtend;
        this.remark = remark;
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

    @Override
    public String toString() {
        return "TaskModel{" +
                "category='" + category + '\'' +
                ", taskName='" + taskName + '\'' +
                ", standard='" + standard + '\'' +
                ", score=" + score +
                ", isFile=" + isFile +
                ", isExtend=" + isExtend +
                ", remark='" + remark + '\'' +
                '}';
    }
}
