package com.hnkjzyxy.ab.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * 项目考核导出数据
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-12 15:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectVo {

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 流程ID
     */
    private Integer flowId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 项目开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 用户昵称（姓名）
     */
    private String nickName;

    /**
     * 用户名（工号）
     */
    private String userName;

    /**
     * 专业
     */
    private String major;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 按分数的排名
     */
    private Integer rank;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 考核明细列表
     */
    private List<AssessVo> assessList;
}
