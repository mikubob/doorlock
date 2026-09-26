package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程安排表（教学排班/电子班牌）
 */
@Data
@TableName("sys_course_schedule")
public class CourseSchedule implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 课程名称
     */
    @TableField("KCMC")
    private String courseName;

    /**
     * 学年
     */
    @TableField("KKXND")
    private String academicYear;

    /**
     * 学期
     */
    @TableField("KKXQM")
    private String semester;

    /**
     * 周次
     */
    @TableField("ZC")
    private String week;

    /**
     * 星期几
     */
    @TableField("XQJ")
    private String dayOfWeek;

    /**
     * 上课节次
     */
    @TableField("SKJC")
    private String classPeriod;

    /**
     * 教室号
     */
    @TableField("JSH")
    private String classroomNumber;

    /**
     * 上课地点
     */
    @TableField("SKDD")
    private String teachingLocation;

    /**
     * 校区
     */
    @TableField("XQ")
    private String campus;

    /**
     * 建筑物名称（教学楼）
     */
    @TableField("JZWMC")
    private String buildingName;

    /**
     * 教工号（教师工号）
     */
    @TableField("JGH")
    private String teacherId;

    /**
     * 教师姓名
     */
    @TableField("JSXM")
    private String teacherName;

    /**
     * 所在单位名称（学院/部门）
     */
    @TableField("SZDWMC")
    private String departmentName;

    /**
     * 班级名称
     */
    @TableField("BJMC")
    private String className;

    /**
     * 辅导员姓名
     */
    @TableField("FDYXM")
    private String counselorName;

    /**
     * 教学班人数
     */
    @TableField("JXBRS")
    private Integer classSize;

    /**
     * 请假人数
     */
    @TableField("QJRS")
    private Integer leaveCount;

    /**
     * 是否有请假人数（0否1是）
     */
    @TableField("SFYQJRS")
    private String hasLeave;

    /**
     * 上课日期（格式yyyyMMdd）
     */
    @TableField("SKRQ")
    private String classDate;

    /**
     * 记录创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


    /**
     * 层
     */
    private Integer floor;

    /**
     * 电子班牌的sn（查询条件，不对应数据库字段）
     */
    @TableField(exist = false)
    private String boardSn;
}
