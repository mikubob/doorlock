package com.hnkjzyxy.ab.service.listener;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-05 19:02
 */

public class CheckResultModel {

    @ExcelProperty(value = "日期", index = 1)
    private String date;


    @ExcelProperty(value = "周次", index = 2)
    private String weeks;


    @ExcelProperty(value = "节次", index = 3)
    private String section;


    @ExcelProperty(value = "教室名", index = 4)
    private String classroom;
    @ExcelProperty(value = "上课班级", index = 5)
    private String classes;
    @ExcelProperty(value = "应到人数", index = 6)
    private String shouldArrival;


    @ExcelProperty(value = "实到人数", index = 7)
    private String arrival;

    @ExcelProperty(value = "出勤率", index = 8)
    private String arrivalRate;


    @ExcelProperty(value = "课堂纪律", index = 9)
    private String discipline;


    @ExcelProperty(value = "带食物进教室人数", index = 10)
    private String foodBringPerson;

    @ExcelProperty(value = "带食物率", index = 11)
    private String foodBringRate;

    @ExcelProperty(value = "是否迟到、早退", index = 12)
    private String isLate;

    @ExcelProperty(value = "是否违反八不准", index = 13)
    private String isViolate;

    @ExcelProperty(value = "是否正常组织上课", index = 14)
    private String isNormal;

    @ExcelProperty(value = "辅导员", index = 15)
    private String counsellor;

    @ExcelProperty(value = "任课老师", index = 16)
    private String teacher;

    @ExcelProperty(value = "巡查人", index = 17)
    private String checkPerson;

    public CheckResultModel() {
    }

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getShouldArrival() {
        return shouldArrival;
    }

    public void setShouldArrival(String shouldArrival) {
        this.shouldArrival = shouldArrival;
    }

    public String getArrival() {
        return arrival;
    }

    public void setArrival(String arrival) {
        this.arrival = arrival;
    }

    public String getArrivalRate() {
        return arrivalRate;
    }

    public void setArrivalRate(String arrivalRate) {
        this.arrivalRate = arrivalRate;
    }

    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public String getFoodBringPerson() {
        return foodBringPerson;
    }

    public void setFoodBringPerson(String foodBringPerson) {
        this.foodBringPerson = foodBringPerson;
    }

    public String getFoodBringRate() {
        return foodBringRate;
    }

    public void setFoodBringRate(String foodBringRate) {
        this.foodBringRate = foodBringRate;
    }

    public String getIsLate() {
        return isLate;
    }

    public void setIsLate(String isLate) {
        this.isLate = isLate;
    }

    public String getIsViolate() {
        return isViolate;
    }

    public void setIsViolate(String isViolate) {
        this.isViolate = isViolate;
    }

    public String getIsNormal() {
        return isNormal;
    }

    public void setIsNormal(String isNormal) {
        this.isNormal = isNormal;
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

    public String getCheckPerson() {
        return checkPerson;
    }

    public void setCheckPerson(String checkPerson) {
        this.checkPerson = checkPerson;
    }
}


