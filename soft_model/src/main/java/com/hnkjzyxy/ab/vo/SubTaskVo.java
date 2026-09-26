package com.hnkjzyxy.ab.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 子任务分数查询结果
 * 返回项目名称、起止时间、用户名与工号、子任务名称及分数（分数可编辑）
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/3/18 20:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskVo {

    /**
     * 结果表主键ID
     */
    private String id;

    /**
     * 项目名称
     */
    private String title;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 结束时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 用户名（姓名）
     */
    private String nickName;

    /**
     * 用户工号
     */
    private String userName;

    /**
     * 子任务名称
     */
    private String taskName;

    /**
     * 子任务分数
     */
    private Integer score;

    /**
     * 项目类型（分类名称）
     */
    private String category;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 最高分数
     */
    private Integer highScore;

    /**
     * 佐证材料（附件记录的字符串）
     */
    private String evidence;

}
