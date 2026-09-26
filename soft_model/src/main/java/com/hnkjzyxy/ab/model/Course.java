package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 课程信息
 * 记录课程的教学安排（周次、节次、教室、任课教师等）
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/4/24 14:34
 */
@Data
public class Course implements Serializable {

    /**
     * 课程ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 星期
     */
    @NotNull(message = "星期不能为空！")
    private String week;

    /**
     * 周次
     */
    @NotNull(message = "周次不能为空！")
    private String weeks;

    /**
     * 节次
     */
    @NotNull(message = "节次不能为空！")
    private String section;

    /**
     * 教室名称
     */
    @NotNull(message = "教室名不能为空！")
    private String classroom;

    /**
     * 上课班级
     */
    @NotNull(message = "上课班级不能为空！")
    private String classes;

    /**
     * 应到人数
     */
    @NotNull(message = "应到人数不能为空！")
    private Integer shouldArrival;

    /**
     * 课程名称
     */
    @NotNull(message = "课程名称不能为空！")
    private String course;

    /**
     * 辅导员
     */
    @NotNull(message = "辅导员不能为空！")
    private String counsellor;

    /**
     * 任课老师
     */
    @NotNull(message = "任课老师不能为空！")
    private String teacher;

    /**
     * 所属学院
     */
    @NotNull(message = "所属学院不能为空！")
    private String college;

    /**
     * 数据状态（不参与序列化）
     */
    @JsonIgnore
    private Integer state;

}
