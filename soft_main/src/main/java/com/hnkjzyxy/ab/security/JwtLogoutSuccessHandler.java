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
 * 退出登录成功处理器，清理令牌缓存并输出统一响应
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/15 12:59
 */
@Component
public class JwtLogoutSuccessHandler implements LogoutSuccessHandler {
    /**
     * 令牌缓存键后缀
     */
    @Value("${absolute.jwt.suffix}")
    private String suffix;
    /**
     * JWT 生成及解析工具
     */
    @Autowired
    private JwtUtils jwtUtils;
    /**
     * Redis 数据操作工具
     */
    @Autowired
    private RedisUtils redisUtils;

    /**
     * {@inheritDoc}
     */
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
