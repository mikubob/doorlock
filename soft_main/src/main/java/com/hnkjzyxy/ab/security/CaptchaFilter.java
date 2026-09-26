package com.hnkjzyxy.ab.security;

import cn.hutool.core.util.ObjectUtil;
import com.hnkjzyxy.ab.exception.CaptchaException;
import com.hnkjzyxy.ab.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 验证码过滤器 （因为security没有验证码验证，需要自定义一个过滤器）
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/11 16:43
 */
@Component
public class CaptchaFilter extends OncePerRequestFilter {

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private LoginFailureHandler loginFailureHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain filterChain) throws ServletException, IOException {
        //只拦截login请求
        String uri = req.getRequestURI();
        //校验验证码是否正确
        if ("/api/login".equals(uri) && "POST".equals(req.getMethod())) {
            try {//捕获验证码异常，交给认证失败处理器
                validate(req);
            } catch (CaptchaException e) {//交给认证失败处理器
                loginFailureHandler.onAuthenticationFailure(req, resp, e);
            }
        }
        //不是login请求或验证码正确直接往下走
        filterChain.doFilter(req, resp);
    }

    public void validate(HttpServletRequest req) {
        String code = req.getParameter("code");
        String key = req.getParameter("token");
        // 如果有错误直接抛异常 然后会被认证失败处理器捕获到
        if (ObjectUtil.isNull(code) || ObjectUtil.isNull(key)) throw new CaptchaException("验证码错误");
        if (!code.equals(redisUtils.get(key))) throw new CaptchaException("验证码错误");
    }
}
