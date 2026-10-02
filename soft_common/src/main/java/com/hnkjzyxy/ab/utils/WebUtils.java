package com.hnkjzyxy.ab.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP 响应内容输出工具
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-15 14:14
 */
public class WebUtils {

    /**
     * 将字符串作为 JSON 内容写入响应
     *
     * @param response HTTP 响应对象
     * @param string 待输出的字符串
     */
    public static void renderString(HttpServletResponse response, String string) {
        try {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
