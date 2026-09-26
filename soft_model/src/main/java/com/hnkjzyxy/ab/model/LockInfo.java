package com.hnkjzyxy.ab.model;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 智能锁实体类
 */
@Data
public class LockInfo {
    /**
     * 智能锁唯一标识符
     */
    @TableId(value = "lock_id", type = IdType.AUTO)
    private Integer lockId;

    /**
     * 智能锁IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 智能锁SN序列号
     */
    @TableField("sn_code")
    private String snCode;

    /**
     * 智能锁通信端口号
     */
    @TableField("port_number")
    private Integer portNumber;

    /**
     * 当前开关状态（0-关 1-开）
     */
    @TableField("switch_status")
    private Integer switchStatus;
    
    /**
     * 教室编号（对应数据库JSH字段）
     */
    @TableField("JSH")
    private String classroomNumber;

    /**
     * 教室名称（查询时从sys_classroom表联表获取，不存在于sys_lock_info表）
     */
    @TableField(exist = false)
    private String classroomName;

    /**
     * 校区名称（查询时从sys_classroom表联表获取，不存在于sys_lock_info表）
     */
    @TableField(exist = false)
    private String campusName;

    /**
     * 教学楼名称（查询时从sys_classroom表联表获取，不存在于sys_lock_info表）
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 楼层（查询时从sys_classroom表联表获取，不存在于sys_lock_info表）
     */
    @TableField(exist = false)
    private Integer floor;
    /**
     * 电子班牌的sn
     */

    private String boardSn;
    
    /**
     * 通道信息
     */
    private String remarks;


}
