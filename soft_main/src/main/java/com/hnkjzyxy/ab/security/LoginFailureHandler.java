package com.hnkjzyxy.ab.security;

import cn.hutool.json.JSONUtil;
import com.hnkjzyxy.ab.result.ApiResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * security 登录失败处理器
 *
 * @author Shinelon
 * @version 1.0
 * @time 2022/7/11 15:47
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse resp, AuthenticationException e) throws IOException, ServletException {
        resp.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = resp.getOutputStream();
        String message = e.getMessage().equals("验证码错误") ? e.getMessage() : "用户名或密码错误";
        outputStream.write(JSONUtil.toJsonStr(ApiResult.error(406, message)).getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();
    }
}
