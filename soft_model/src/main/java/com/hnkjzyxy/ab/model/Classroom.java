package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 教室信息表
 */
@Data
@TableName("sys_classroom")
public class Classroom implements Serializable {
    
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
     * 电子班牌SN码
     */
    @TableField("board_sn")
    private int boardSn;

    /**
     * 教室编号
     */
    @TableField("JSH")
    private String classroomNumber;

    /**
     * 教室名称
     */
    @TableField("SKDD")
    private String classroomName;

    /**
     * 校区名称
     */
    @TableField("XQMC")
    private String campusName;

    /**
     * 教学楼名称
     */
    @TableField("JZWMC")
    private String buildingName;

    /**
     * 层
     */
    private Integer floor;
    /**
     * 锁状态
     */
    private Integer switchStatus;
}
