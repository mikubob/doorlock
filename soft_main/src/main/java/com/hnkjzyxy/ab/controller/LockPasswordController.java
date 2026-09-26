package com.hnkjzyxy.ab.controller;

import cn.hutool.json.JSONUtil;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.LockPasswordService;
import com.hnkjzyxy.ab.utils.JwtUtils;
import com.hnkjzyxy.ab.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * 开锁密码管理
 * 提供开锁密码登录（生成仅可用于开锁接口的 token）与开锁密码设置功能
 */
@RestController
@RequestMapping("/lock")
public class LockPasswordController {
    
    @Value("${absolute.jwt.suffix}")
    private String suffix;
    
    @Autowired
    private LockPasswordService lockPasswordService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private RedisUtils redisUtils;
    
    /**
     * 开锁密码登录接口
     * 验证开锁密码并生成专用token
     * @param password 开锁密码
     * @param response HTTP响应
     * @return 登录结果,包含专用token
     */
    @PostMapping("/login")
    public void lockPasswordLogin(@RequestParam("password") String password, 
                                   HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = response.getOutputStream();
        
        // 验证开锁密码
        if (!lockPasswordService.verifyPassword(password)) {
            outputStream.write(JSONUtil.toJsonStr(ApiResult.error(401, "开锁密码错误")).getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();
            return;
        }
        
        // 生成专用token,subject格式: lock_password:{timestamp}
        String tokenSubject = "lock_password:" + System.currentTimeMillis();
        String token = jwtUtils.generateToken(tokenSubject);
        
        // 将token存入Redis,设置过期时间(与JWT相同)
        // key格式: absolute:lock_token:{token前8位}
        String redisKey = suffix.concat("-lock_token-").concat(token.substring(0, 8));
        redisUtils.set(redisKey, token, jwtUtils.getExpire());
        
        // 返回token
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("header", jwtUtils.getHeader());
        hashMap.put("token", token);
        hashMap.put("tokenType", "LOCK_ONLY"); // 标识这是开锁专用token
        
        outputStream.write(JSONUtil.toJsonStr(ApiResult.ok("data", hashMap)).getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();
    }
    
    /**
     * 设置开锁密码(需要管理员权限)
     * @param password 新密码
     * @param description 密码描述
     * @return 操作结果
     */
    @PostMapping("/setPassword")
    public ApiResult setLockPassword(@RequestParam("password") String password,
                                      @RequestParam("oldPassword") String oldPassword,
                                      @RequestParam(value = "description", required = false) String description) {
        if (password == null || password.trim().isEmpty()) {
            return ApiResult.error("密码不能为空");
        }
        if (password.length() > 6){
            return ApiResult.error("密码长度不能超过6位");
        }
        if (oldPassword==null || oldPassword.trim().isEmpty()){
            return ApiResult.error("旧密码不能为空");
        }
        
        // 验证旧密码
        if (!lockPasswordService.verifyOldPassword(oldPassword)) {
            return ApiResult.error("旧密码错误");
        }
        
        boolean result = lockPasswordService.setPassword(password, description);
        if (result) {
            return ApiResult.ok("开锁密码设置成功");
        } else {
            return ApiResult.error("开锁密码设置失败");
        }
    }
}
