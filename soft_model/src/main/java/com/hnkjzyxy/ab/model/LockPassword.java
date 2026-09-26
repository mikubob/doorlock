package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 开锁密码实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_lock_password")
public class LockPassword implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 开锁密码(加密存储)
     */
    private String password;
    
    /**
     * 密码描述/备注
     */
    private String description;
    
    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
