package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User implements Serializable {

    /**
     * 用户ID
     */
    @TableId
    private Integer userId;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 用户昵称（姓名）
     */
    private String nickName;

    /**
     * 用户名（工号）
     */
    private String userName;

    /**
     * 登录密码（BCrypt 加密存储）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别
     */
    private String sex;

    /**
     * 所属专业
     */
    private String major;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 入职时间
     */
    private Date entryTime;

    /**
     * 最近登录时间
     */
    private Date lastLogin;

    /**
     * 状态（1=启用，0=禁用）
     * <p>
     * 注意：这里是**业务状态**，不是逻辑删除标记，因此**不要**加 {@code @TableLogic}。
     * 本项目 MyBatis-Plus 版本为 3.2.0，其 {@code GlobalConfig.DbConfig} 没有
     * {@code logicDeleteField} 属性，yml 里的 {@code logic-delete-field} 实际不生效，
     * 逻辑删除只认实体上显式标注的 {@code @TableLogic}。
     */
    private Integer status;

    /**
     * 所属学院
     */
    private String college;

    /**
     * 角色列表（非数据库字段）
     */
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Role> roles;
}
