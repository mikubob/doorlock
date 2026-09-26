package com.hnkjzyxy.ab.security;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.security.user.AccountUser;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.utils.JwtUtils;
import com.hnkjzyxy.ab.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * Security 登录成功处理器
 *
 * @author Shinelon
 * @version 1.0
 * @time 2022/7/11 15:53
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserService userService;
    @Value("${absolute.jwt.suffix}")
    private String suffix;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication authentication) throws IOException, ServletException {
        resp.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = resp.getOutputStream();

        AccountUser user = (AccountUser) authentication.getPrincipal();//security中的用户信息
        String token = jwtUtils.generateToken(user.getUsername().concat(",").concat(user.getUserId().toString()));

        redisUtils.set(suffix.concat("-").concat(user.getUserId().toString()), token, jwtUtils.getExpire());
//        redisUtils.set(suffix.concat("-").concat(IpUtil.getIpAddr(req)),"1",10, TimeUnit.SECONDS);

        User currentUser = userService.getUserByName(user.getUsername());
        if (ObjectUtil.isNull(currentUser)) {
            throw new RuntimeException("用户不能为空！");
        }
        System.out.println("用户登录：" + user.getUsername());
        currentUser.setPassword("");
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("header", jwtUtils.getHeader());
        hashMap.put("token", token);
        hashMap.put("user", currentUser);
        outputStream.write(JSONUtil.toJsonStr(ApiResult.ok("data", hashMap)).getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();
    }

}
