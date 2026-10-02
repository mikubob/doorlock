package com.hnkjzyxy.ab.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 学生信息导出数据
 * 用于学生成绩与录取结果的 Excel 导出
 */
@AllArgsConstructor
@NoArgsConstructor
@HeadRowHeight(30)  //表头行高
@ContentRowHeight(15)  //内容行高
@ColumnWidth(18)  //列宽
@ContentFontStyle(fontHeightInPoints = (short) 12)
@Getter
@Setter
public class StudentInfoVo {

    /**
     * 姓名
     */
    @ExcelProperty(value = "姓名", index = 0)
    private String name;

    /**
     * 学号
     */
    @ExcelProperty(value = "学号", index = 1)
    private Long studentId;

    /**
     * 班级
     */
    @ExcelProperty(value = "班级", index = 2)
    private String className;

    /**
     * 网页设计与制作成绩
     */
    @ExcelProperty(value = " 网页设计与制作成绩", index = 3)
    private Integer webScore;

    /**
     * 面向对象程序设计（Java）成绩
     */
    @ExcelProperty(value = "面向对象程序设计（Java）成绩", index = 4)
    private Integer javaScore;

    /**
     * 程序设计基础成绩
     */
    @ExcelProperty(value = "程序设计基础成绩", index = 5)
    private Integer programScore;

    /**
     * 数据库应用技术成绩
     */
    @ExcelProperty(value = "数据库应用技术成绩", index = 6)
    private Integer databaseScore;

    /**
     * 以 web 成绩为主的总分
     */
    @ExcelProperty(value = " 以web成绩为主", index = 7)
    private Integer webTotal;

    /**
     * 以 Java 成绩为主的总分
     */
    @ExcelProperty(value = "以Java成绩为主", index = 8)
    private Integer javaTotal;

    /**
     * 以 program 成绩为主的总分
     */
    @ExcelProperty(value = "以program成绩为主", index = 9)
    private Integer programTotal;

    /**
     * 以 database 成绩为主的总分
     */
    @ExcelProperty(value = "以database成绩为主", index = 10)
    private Integer databaseTotal;

    /**
     * 志愿填报一
     */
    @ExcelProperty(value = "志愿填报一", index = 11)
    private String applicationOne;

    /**
     * 志愿填报二
     */
    @ExcelProperty(value = "志愿填报二", index = 12)
    private String applicationTwo;

    /**
     * 录取的志愿
     */
    @ExcelProperty(value = "录取的志愿", index = 13)
    private String result;

    /**
     * 录取结果（0=未录取，1=已录取）
     */
    @ExcelProperty(value = "录取结果", index = 14)
    @Setter(AccessLevel.NONE)
    private String status;

    /**
     * 修改时间
     */
    @ExcelProperty(value = "修改时间", index = 15)
    private String updateTime;

    /**
     * 填报结束时间
     */
    @ExcelProperty(value = "结束时间", index = 16)
    private String endTime;

    /**
     * 将录取状态编号转换为导出展示文字
     *
     * @param status 录取状态（0=未录取，1=已录取）
     */
    public void setStatus(Integer status) {
        if (status == 0) {
            this.status = "未录取";
        }
        if (status == 1) {
            this.status = "已录取";
        }
    }

    /**
     * 直接设置录取结果展示文字
     *
     * @param status 录取结果文字
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
