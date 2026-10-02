package com.hnkjzyxy.ab.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 令牌生成、解析及过期校验工具
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/11 18:47
 */
@Data
@Component
public class JwtUtils {

    /**
     * JWT 签名密钥
     */
    @Value("${absolute.jwt.secret}")
    private String secret;
    /**
     * JWT 有效期，单位为秒
     */
    @Value("${absolute.jwt.expire}")
    private Long expire;
    /**
     * 请求中的认证令牌头名称
     */
    @Value("${absolute.jwt.header}")
    private String header;

    /**
     * 生成token
     *
     * @param subject 主题
     * @return 包含主题及过期时间的 JWT 字符串
     */
    public String generateToken(String subject) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (expire * 1000)))
                .signWith(SignatureAlgorithm.HS256, secret).compact();
    }

    /**
     * 解析token
     *
     * @param token 要解析的token
     * @return JWT 载荷信息；令牌无效或解析失败时返回 null
     */
    public Claims resolveToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断是否过期
     *
     * @param claims JWT 载荷及过期信息
     * @return 令牌过期时返回 true，否则返回 false
     */
    public boolean isTokenExpire(Claims claims) {
        // 失效时间是否在当前时间之前
        return claims.getExpiration().before(new Date());
    }
}
