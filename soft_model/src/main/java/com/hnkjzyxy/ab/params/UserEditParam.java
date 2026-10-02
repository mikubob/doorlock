package com.hnkjzyxy.ab.params;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** 修改当前登录用户的联系方式；身份由认证上下文确定。 */
@Data
public class UserEditParam {
    /** 保留原请求的工号字段及校验，兼容已有客户端。 */
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

}
