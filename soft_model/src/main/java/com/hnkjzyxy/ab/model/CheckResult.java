package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 教学巡查记录
 * 记录每次教学巡查的班级出勤、纪律等检查结果
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/4/23 20:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckResult implements Serializable {

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 课程登记身份，历史不依赖全量替换行号。
     */
    private String courseKey;
    /**
     * 巡查时课程、班级、教师和区间的不可变快照。
     */
    private String scheduleSnapshot;
    /**
     * 请假人数来源，人工确认或历史快照。
     */
    private String leaveSource;
    /**
     * 无可靠课程关联时的补录理由。
     */
    private String supplementReason;

    /**
     * 巡查日期
     */
    @NotNull(message = "日期不能为空！")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;

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
     * 教室
     */
    @NotNull(message = "教室不能为空！")
    private String classroom;

    /**
     * 班级
     */
    @NotNull(message = "班级不能为空！")
    private String classes;

    /**
     * 应到人数
     */
    @NotNull(message = "应到人数不能为空！")
    private Integer shouldArrival;

    /**
     * 实到人数
     */
    @NotNull(message = "实到人数不能为空！")
    private Integer arrival;

    /**
     * 出勤率
     */
    @NotNull(message = "出勤率不能为空！")
    private String arrivalRate;

    /**
     * 课堂纪律情况
     */
    private String discipline;

    /**
     * 带食物人数
     */
    @NotNull(message = "带食物人数不能为空！")
    private Integer foodBringPerson;

    /**
     * 带食物率
     */
    private String foodBringRate;

    /**
     * 是否迟到（0=否，1=是）
     */
    @NotNull(message = "是否迟到不能为空！")
    private Integer isLate;

    /**
     * 是否违反八不准（0=否，1=是）
     */
    @NotNull(message = "是否违法八不准不能为空！")
    private Integer isViolate;

    /**
     * 是否正常组织上课（0=否，1=是）
     */
    @NotNull(message = "是否正常组织上课不能为空！")
    private Integer isNormal;

    /**
     * 辅导员
     */
    @NotNull(message = "辅导员不能为空！")
    private String counsellor;

    /**
     * 科任老师
     */
    @NotNull(message = "科任老师不能为空！")
    private String teacher;

    /**
     * 巡查人
     */
    @NotNull(message = "巡查人不能为空！")
    private String checkPerson;

    /**
     * 学院
     */
    @NotNull(message = "学院！")
    private String college;

    /**
     * 请假人次
     */
    private Integer peopleLeave;

    /**
     * 备注
     */
    private String remark;

    /**
     * 查询结束时间（非数据库字段）
     */
    @JsonIgnore
    @TableField(exist = false)
    private String endTime;

    /**
     * 查询开始时间（非数据库字段）
     */
    @JsonIgnore
    @TableField(exist = false)
    private String startTime;

    /**
     * 是否站立上课（0=否，1=是）
     */
    private Integer isStand;

    /**
     * 是否与课表一致（0=否，1=是）
     */
    private Integer isConsist;

}
