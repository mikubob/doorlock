package com.hnkjzyxy.ab.security;

import cn.hutool.core.util.ObjectUtil;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.utils.JwtUtils;
import com.hnkjzyxy.ab.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 非登录请求，进行自动登录
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/11 23:28
 */
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    /**
     * JWT 生成及解析工具
     */
    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;

    /**
     * 令牌缓存键后缀
     */
    @Value("${absolute.jwt.suffix}")
    private String suffix;

    /**
     * Redis 数据操作工具
     */
    @Autowired
    private RedisUtils redisUtils;
    /**
     * Redis 数据操作模板
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 初始化JwtAuthenticationFilter
     *
     * @param authenticationManager 认证管理器
     */
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
//        String ipAddr = IpUtil.getIpAddr(request);
//        boolean back = redisUtils.hHasKey("BlackList", ipAddr);
//        if(back){
//            throw new JwtException("你没有权限访问！赶紧想想自己干了什么事！");
//        }
//        String ips = (suffix.concat("-").concat(ipAddr));
//        String ip = redisUtils.get(ips);
//        if(ObjectUtil.isNotEmpty(ip)){
//            int value = Integer.valueOf(ip).intValue();
//            value += 1;
//            redisUtils.set(ips,String.valueOf(value),1,TimeUnit.SECONDS);
//            if(value > 100){
//                redisUtils.hset("BlackList",ipAddr,ipAddr);
//                throw new RuntimeException("请重新登录！");
//            }
//        } else{
//            redisUtils.set(ips,String.valueOf(1),1, TimeUnit.SECONDS);
//        }

        String token = request.getHeader(jwtUtils.getHeader());
        if (ObjectUtil.isNull(token)) {
            chain.doFilter(request, response);
            return;
        }

        //解析token
        Claims claims = jwtUtils.resolveToken(token);
        if (ObjectUtil.isNull(claims)) {
            throw new JwtException("token解析失败");
        }
        if (jwtUtils.isTokenExpire(claims)) {
            throw new JwtException("token已过期");
        }
        
        String subject = claims.getSubject();
        
        // 判断是否为开锁专用token
        if (subject.startsWith("lock_password:")) {
            // 开锁专用token验证逻辑
            // key格式: absolute:lock_token:{token前8位}
            String redisKey = suffix.concat("-lock_token-").concat(token.substring(0, 8));
            Object key = redisUtils.get(redisKey);
            if (ObjectUtil.isNull(key)) {
                throw new JwtException("开锁token已过期或无效!");
            }
            if (!token.equals(key.toString())) {
                throw new JwtException("开锁token无效!");
            }
            
            // 为开锁token设置特殊的认证信息
            // 使用一个虚拟用户名标识这是开锁权限
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "LOCK_ONLY_USER", 
                "", 
                AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_LOCK_ONLY")
            );
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            chain.doFilter(request, response);
            return;
        }
        
        // 普通用户token验证逻辑
        String[] subjectParts = subject.split(",");
        if (subjectParts.length != 2) {
            throw new JwtException("token被篡改");
        }
        User user = userService.getById(subjectParts[1]);
        if (user == null) {
            throw new JwtException("token被篡改");
        }
        Object key = redisUtils.get(suffix.concat("-").concat(user.getUserId().toString()));
        if (ObjectUtil.isNull(key)) {
            throw new JwtException("token已过期!请重新登录！");
        }

        if (!token.equals(key.toString())) {
            throw new JwtException("您已在异地登录！请退出重新登录！");
        }


        //一参是用户名 二参为密码 三参是权限信息
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword(),
                AuthorityUtils.commaSeparatedStringToAuthorityList(userService.getUserAuthority(user.getUserId())));

        //设置security上下文的认证主体（会自动完成登录）
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        chain.doFilter(request, response);
    }
}
