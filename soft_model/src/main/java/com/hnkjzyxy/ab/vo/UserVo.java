package com.hnkjzyxy.ab.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

/**
 * 用户信息（含角色岗位）
 *
 * @author 16702
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo implements Serializable {

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
    @NotBlank(message = "工号不能为空！")
    private String userName;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱地址不能为空！")
    @Email(message = "邮箱格式错误！")
    private String email;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空！")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误！")
    private String phone;

    /**
     * 性别
     */
    private String sex;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 专业
     */
    private String major;

    /**
     * 角色职称列表
     */
    private List<String> jobTitle;

    /**
     * 项目ID
     */
    private Integer pId;

    /**
     * 角色权重
     */
    private Integer weight;

    /**
     * 所属学院
     */
    private String college;

}
