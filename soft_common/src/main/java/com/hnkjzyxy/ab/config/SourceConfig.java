package com.hnkjzyxy.ab.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据库连接及 MyBatis-Plus 分页配置
 */
@Configuration
public class SourceConfig {

    /**
     * 创建绑定 spring.datasource 配置的 Druid 数据源
     *
     * @return 数据源
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        return dataSource;
    }

    /**
     * MySQL 分页插件
     *
     * @return 分页拦截器
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        paginationInterceptor.setDialectType("mysql");
        return paginationInterceptor;
    }

}
