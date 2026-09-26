package com.hnkjzyxy.ab.security;

import cn.hutool.json.JSONUtil;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.utils.JwtUtils;
import com.hnkjzyxy.ab.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/15 12:59
 */
@Component
public class JwtLogoutSuccessHandler implements LogoutSuccessHandler {
    @Value("${absolute.jwt.suffix}")
    private String suffix;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication authentication) throws IOException, ServletException {
        resp.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = resp.getOutputStream();
        String id = req.getParameter("id");
        if (id != null) {
            redisUtils.del(suffix.concat("-").concat(id));
            resp.setHeader(jwtUtils.getHeader(), "");
            HashMap<String, Object> map = new HashMap<>();
            map.put("msg", "退出登录成功");
            map.put("code", 200);
            outputStream.write(JSONUtil.toJsonStr(ApiResult.ok("data", map)).getBytes("utf-8"));
        }
        outputStream.flush();
        outputStream.close();
    }
}
