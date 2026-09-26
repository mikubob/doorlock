package com.hnkjzyxy.ab.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改密码参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/4/24 21:18
 */
@Data
public class UserPasswordParam {

    /**
     * 原密码
     */
    @NotBlank(message = "旧密码不能为空！")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空！")
    private String newPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空！")
    private String confirmPassword;

}
