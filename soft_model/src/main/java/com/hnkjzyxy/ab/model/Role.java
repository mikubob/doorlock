package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 角色信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Role implements Serializable {

    /**
     * 角色ID
     */
    @TableId
    private Integer roleId;

    /**
     * 角色名称
     */
    @NotBlank(message = "请输入角色名称")
    private String roleName;

    /**
     * 角色唯一编码
     */
    @NotBlank(message = "请输入唯一编码")
    private String roleCode;

    /**
     * 角色描述
     */
    @NotBlank(message = "请输入描述")
    private String roleDesc;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 角色状态
     */
    @TableLogic
    @NotNull(message = "请选择状态")
    private Integer roleStatus;

    /**
     * 角色权重（数值越大权限越高）
     */
    private Integer weight;
}
