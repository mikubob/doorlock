package com.hnkjzyxy.ab.security;

import cn.hutool.json.JSONUtil;
import com.hnkjzyxy.ab.result.ApiResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 权限不足处理器
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/12 11:21
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, AccessDeniedException e) throws IOException, ServletException {
        resp.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = resp.getOutputStream();
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        outputStream.write(JSONUtil.toJsonStr(ApiResult.error(403, e.getMessage())).getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();
    }
}
