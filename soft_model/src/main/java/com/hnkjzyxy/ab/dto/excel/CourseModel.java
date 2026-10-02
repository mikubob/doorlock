package com.hnkjzyxy.ab.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 课表 Excel 导入行模型
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
public class CourseModel {
    /**
     * 周次
     */
    @ExcelProperty(value = "周次", index = 0)
    private String weeks;

    /**
     * 节次
     */
    @ExcelProperty(value = "节次", index = 1)
    private String section;

    /**
     * 星期
     */
    @ExcelProperty(value = "星期", index = 2)
    private String week;

    /**
     * 教室名
     */
    @ExcelProperty(value = "教室名", index = 3)
    private String classroom;

    /**
     * 上课班级
     */
    @ExcelProperty(value = "上课班级", index = 4)
    private String classes;

    /**
     * 上课人数（除去长期集训同学）
     */
    @ExcelProperty(value = "上课人数（除去长期集训同学）", index = 5)
    private String should_arrival;

    /**
     * 课程名称
     */
    @ExcelProperty(value = "课程名称", index = 6)
    private String course;

    /**
     * 辅导员
     */
    @ExcelProperty(value = "辅导员", index = 7)
    private String counsellor;

    /**
     * 任课教师
     */
    @ExcelProperty(value = "任课教师", index = 8)
    private String teacher;

    /**
     * 学院
     */
    @ExcelProperty(value = "学院", index = 9)
    private String college;

    /**
     * 保留既有导入行构造方式
     *
     * @param weeks 周次
     * @param section 节次
     * @param week 星期
     * @param classroom 教室名
     * @param classes 上课班级
     * @param should_arrival 上课人数（除去长期集训同学）
     * @param course 课程名称
     * @param counsellor 辅导员
     * @param teacher 任课教师
     */
    public CourseModel(String weeks, String section, String week, String classroom, String classes, String should_arrival, String course, String counsellor, String teacher) {
        this.weeks = weeks;
        this.section = section;
        this.week = week;
        this.classroom = classroom;
        this.classes = classes;
        this.should_arrival = should_arrival;
        this.course = course;
        this.counsellor = counsellor;
        this.teacher = teacher;
    }

    /**
     * 返回导入行的原有日志描述
     *
     * @return 行字段描述
     */
    @Override
    public String toString() {
        return "CourseModel{" +
                "weeks='" + weeks + '\'' +
                ", section='" + section + '\'' +
                ", week='" + week + '\'' +
                ", classroom='" + classroom + '\'' +
                ", classes='" + classes + '\'' +
                ", should_arrival=" + should_arrival +
                ", course='" + course + '\'' +
                ", counsellor='" + counsellor + '\'' +
                ", teacher='" + teacher + '\'' +
                '}';
    }
}
