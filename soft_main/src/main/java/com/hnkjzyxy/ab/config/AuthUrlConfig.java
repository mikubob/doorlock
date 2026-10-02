package com.hnkjzyxy.ab.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证白名单配置，绑定允许匿名访问的接口路径
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "yue")
public class AuthUrlConfig {
    /**
     * 允许匿名访问的接口路径集合
     */
    private String[] url;
}
