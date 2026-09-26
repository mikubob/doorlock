package com.hnkjzyxy.ab.service.listener;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-05 19:02
 */
public class CourseModel {
    @ExcelProperty(value = "周次", index = 0)
    private String weeks;


    @ExcelProperty(value = "节次", index = 1)
    private String section;
    @ExcelProperty(value = "星期", index = 2)
    private String week;

    @ExcelProperty(value = "教室名", index = 3)
    private String classroom;
    @ExcelProperty(value = "上课班级", index = 4)
    private String classes;
    @ExcelProperty(value = "上课人数（除去长期集训同学）", index = 5)
    private String should_arrival;

    @ExcelProperty(value = "课程名称", index = 6)
    private String course;


    @ExcelProperty(value = "辅导员", index = 7)
    private String counsellor;
    @ExcelProperty(value = "任课教师", index = 8)
    private String teacher;
    @ExcelProperty(value = "学院", index = 9)
    private String college;

    public CourseModel() {
    }


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

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getWeeks() {
        return weeks;
    }

    public void setWeeks(String weeks) {
        this.weeks = weeks;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public String getShould_arrival() {
        return should_arrival;
    }

    public void setShould_arrival(String should_arrival) {
        this.should_arrival = should_arrival;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getCounsellor() {
        return counsellor;
    }

    public void setCounsellor(String counsellor) {
        this.counsellor = counsellor;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

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
