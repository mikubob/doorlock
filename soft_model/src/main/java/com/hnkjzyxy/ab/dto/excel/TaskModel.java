package com.hnkjzyxy.ab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 项目任务 Excel 导入行模型
 * 保留模板列名、列索引及单元格原始值，供导入监听器解析
 *
 * @version 1.0
 * @email 1670203784@qq.com
 * @author Spell a
 * @date 2024-01-05 19:02
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskModel {

    /**
     * 类别
     */
    @ExcelProperty(value = "类别", index = 1)
    private String category;

    /**
     * 子任务
     */
    @ExcelProperty(value = "子任务", index = 2)
    private String taskName;

    /**
     * 计分标准
     */
    @ExcelProperty(value = "计分标准", index = 3)
    private String standard;

    /**
     * 满分
     */
    @ExcelProperty(value = "满分", index = 4)
    private Integer score;

    /**
     * 是否需要文件上传
     */
    @ExcelProperty(value = "是否需要文件上传", index = 5)
    private Integer isFile;

    /**
     * 是否需要扩展项
     */
    @ExcelProperty(value = "是否需要扩展项", index = 6)
    private Integer isExtend;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注", index = 7)
    private String remark;

    /**
     * 返回导入行的原有日志描述
     *
     * @return 行字段描述
     */
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
