package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 新增用户参数
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserSaveParam implements Serializable {

    /**
     * 用户名（工号），同时作为初始密码
     */
    @NotBlank(message = "请输入工号")
    private String userName;

    /**
     * 手机号
     */
    @Min(value = 11, message = "请输入正确的手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "请输入正确的邮箱号")
    private String email;

    /**
     * 用户昵称（姓名）
     */
    @NotBlank(message = "请输入用户名")
    private String nickName;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 所属学院
     */
    private String college;
}
