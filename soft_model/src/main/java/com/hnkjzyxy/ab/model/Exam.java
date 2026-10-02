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
 * 考试信息表
 */
@Data
@TableName("sys_exam")
public class Exam implements Serializable {
    
    /**
     * 序列化版本标识
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 考试号
     */
    @TableField("exam_code")
    private String examCode;

    /**
     * sn码
     */
    @TableField("board_sn")
    private Integer boardSn;

    /**
     * 考试内容
     */
    @TableField("exam_content")
    private String examContent;

    /**
     * 图片路径
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 视频路径
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 考试开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 考试结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 状态 0=未开始 1=进行中 2=已结束
     */
    @TableField("status")
    private Integer status;
}
