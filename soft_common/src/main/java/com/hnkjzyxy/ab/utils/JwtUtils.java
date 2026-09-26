package com.hnkjzyxy.ab.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/11 18:47
 */
@Data
@Component
public class JwtUtils {

    @Value("${absolute.jwt.secret}")
    private String secret;
    @Value("${absolute.jwt.expire}")
    private Long expire;
    @Value("${absolute.jwt.header}")
    private String header;

    /**
     * 生成token
     *
     * @param subject 主题
     * @return
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
     * @return
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
     * @param claims
     * @return
     */
    public boolean isTokenExpire(Claims claims) {
        // 失效时间是否在当前时间之前
        return claims.getExpiration().before(new Date());
    }
}
