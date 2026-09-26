package com.hnkjzyxy.ab.security;

import cn.hutool.json.JSONUtil;
import com.hnkjzyxy.ab.result.ApiResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * 没有携带token或token错误（错误处理的位置）
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/12 11:20
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse resp, AuthenticationException e) throws IOException, ServletException {
        resp.setContentType("application/json;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);//认证失败 401
        ServletOutputStream outputStream = resp.getOutputStream();
        HashMap<String, Object> map = new HashMap<>();
//        System.out.println();
        System.out.println("----------------");
        System.out.println("【401 拦截】请求 URI: " + req.getRequestURI());
        System.out.println("【401 拦截】请求方法: " + req.getMethod());
        System.out.println("【401 拦截】异常信息: " + (e != null ? e.getMessage() : "null"));
        System.out.println("----------------");

        outputStream.write(JSONUtil.toJsonStr(ApiResult.error(401, "认证失败，请登录！")).getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();
    }
}
