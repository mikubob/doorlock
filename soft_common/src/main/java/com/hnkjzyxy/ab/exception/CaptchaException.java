package com.hnkjzyxy.ab.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 验证码错误异常
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/11 16:50
 */

public class CaptchaException extends AuthenticationException {
    /**
     * 初始化CaptchaException
     *
     * @param msg 提示信息
     */
    public CaptchaException(String msg) {
        super(msg);
    }
}
