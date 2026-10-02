package com.hnkjzyxy.ab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 巡查结果 Excel 导入行模型
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
public class CheckResultModel {

    /**
     * 日期
     */
    @ExcelProperty(value = "日期", index = 1)
    private String date;

    /**
     * 周次
     */
    @ExcelProperty(value = "周次", index = 2)
    private String weeks;

    /**
     * 节次
     */
    @ExcelProperty(value = "节次", index = 3)
    private String section;

    /**
     * 教室名
     */
    @ExcelProperty(value = "教室名", index = 4)
    private String classroom;

    /**
     * 上课班级
     */
    @ExcelProperty(value = "上课班级", index = 5)
    private String classes;

    /**
     * 应到人数
     */
    @ExcelProperty(value = "应到人数", index = 6)
    private String shouldArrival;

    /**
     * 实到人数
     */
    @ExcelProperty(value = "实到人数", index = 7)
    private String arrival;

    /**
     * 出勤率
     */
    @ExcelProperty(value = "出勤率", index = 8)
    private String arrivalRate;

    /**
     * 课堂纪律
     */
    @ExcelProperty(value = "课堂纪律", index = 9)
    private String discipline;

    /**
     * 带食物进教室人数
     */
    @ExcelProperty(value = "带食物进教室人数", index = 10)
    private String foodBringPerson;

    /**
     * 带食物率
     */
    @ExcelProperty(value = "带食物率", index = 11)
    private String foodBringRate;

    /**
     * 是否迟到、早退
     */
    @ExcelProperty(value = "是否迟到、早退", index = 12)
    private String isLate;

    /**
     * 是否违反八不准
     */
    @ExcelProperty(value = "是否违反八不准", index = 13)
    private String isViolate;

    /**
     * 是否正常组织上课
     */
    @ExcelProperty(value = "是否正常组织上课", index = 14)
    private String isNormal;

    /**
     * 辅导员
     */
    @ExcelProperty(value = "辅导员", index = 15)
    private String counsellor;

    /**
     * 任课老师
     */
    @ExcelProperty(value = "任课老师", index = 16)
    private String teacher;

    /**
     * 巡查人
     */
    @ExcelProperty(value = "巡查人", index = 17)
    private String checkPerson;

    /**
     * 保留既有导入行构造方式
     *
     * @param date 日期
     * @param weeks 周次
     * @param section 节次
     * @param week 兼容原构造签名的星期参数，巡查模板不保存该值
     * @param classroom 教室名
     * @param classes 上课班级
     * @param shouldArrival 应到人数
     * @param arrival 实到人数
     * @param arrivalRate 出勤率
     * @param discipline 课堂纪律
     * @param foodBringPerson 带食物进教室人数
     * @param foodBringRate 带食物率
     * @param isLate 是否迟到、早退
     * @param isViolate 是否违反八不准
     * @param isNormal 是否正常组织上课
     * @param counsellor 辅导员
     * @param teacher 任课老师
     * @param checkPerson 巡查人
     */
    public CheckResultModel(String date, String weeks, String section, String week, String classroom, String classes, String shouldArrival, String arrival, String arrivalRate, String discipline, String foodBringPerson, String foodBringRate, String isLate, String isViolate, String isNormal, String counsellor, String teacher, String checkPerson) {
        this.date = date;
        this.weeks = weeks;
        this.section = section;
        this.classroom = classroom;
        this.classes = classes;
        this.shouldArrival = shouldArrival;
        this.arrival = arrival;
        this.arrivalRate = arrivalRate;
        this.discipline = discipline;
        this.foodBringPerson = foodBringPerson;
        this.foodBringRate = foodBringRate;
        this.isLate = isLate;
        this.isViolate = isViolate;
        this.isNormal = isNormal;
        this.counsellor = counsellor;
        this.teacher = teacher;
        this.checkPerson = checkPerson;
    }
}
