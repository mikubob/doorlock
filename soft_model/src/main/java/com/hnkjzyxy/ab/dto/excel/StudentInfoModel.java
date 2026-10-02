package com.hnkjzyxy.ab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生信息导入模型
 * 对应学生信息 Excel 导入模板的行数据
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StudentInfoModel {

    /**
     * 学号
     */
    @ExcelProperty(value = "学生学号", index = 1)
    private Long studentId;

    /**
     * 姓名
     */
    @ExcelProperty(value = "学生姓名", index = 2)
    private String name;

    /**
     * 班级
     */
    @ExcelProperty(value = "班级", index = 3)
    private String className;

    /**
     * 网页设计与制作成绩
     */
    @ExcelProperty(value = "网页设计与制作成绩", index = 4)
    private Integer webScore;

    /**
     * 面向对象程序设计（Java）成绩
     */
    @ExcelProperty(value = "面向对象程序设计（Java）成绩", index = 5)
    private Integer javaScore;

    /**
     * 程序设计基础成绩
     */
    @ExcelProperty(value = "程序设计基础成绩", index = 6)
    private Integer programScore;

    /**
     * 数据库应用技术成绩
     */
    @ExcelProperty(value = "据库应用技术成绩", index = 7)
    private Integer databaseScore;
}
