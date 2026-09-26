package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 学生信息
 * 用于招生录取，记录学生成绩、志愿及录取结果
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName("sys_student_info")
public class StudentInfo {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 学号
     */
    private Long studentId;

    /**
     * 班级
     */
    private String className;

    /**
     * 网页设计与制作成绩
     */
    private Integer webScore;

    /**
     * 面向对象程序设计（Java）成绩
     */
    private Integer javaScore;

    /**
     * 程序设计基础成绩
     */
    private Integer programScore;

    /**
     * 数据库应用技术成绩
     */
    private Integer databaseScore;

    /**
     * 志愿填报一
     */
    private String applicationOne;

    /**
     * 志愿填报二
     */
    private String applicationTwo;

    /**
     * 录取的志愿
     */
    private String result;

    /**
     * 录取状态（0=未录取，1=已录取）
     */
    private Integer status;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 填报结束时间
     */
    private Date endTime;

    /**
     * 删除标记（0=未删除，1=已删除）
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 网页设计与制作满分
     */
    private float webTotal;

    /**
     * 面向对象程序设计满分
     */
    private float javaTotal;

    /**
     * 程序设计基础满分
     */
    private float programTotal;

    /**
     * 数据库应用技术满分
     */
    private float databaseTotal;

}
