package com.hnkjzyxy.ab.config;

import com.hnkjzyxy.ab.security.CaptchaFilter;
import com.hnkjzyxy.ab.security.JwtAccessDeniedHandler;
import com.hnkjzyxy.ab.security.JwtAuthenticationEntryPoint;
import com.hnkjzyxy.ab.security.JwtAuthenticationFilter;
import com.hnkjzyxy.ab.security.JwtLogoutSuccessHandler;
import com.hnkjzyxy.ab.security.LoginFailureHandler;
import com.hnkjzyxy.ab.security.LoginSuccessHandler;
import com.hnkjzyxy.ab.security.user.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 认证、权限校验、过滤器及跨域配置
 *
 * @author Shinelon
 * @version 1.0
 * @time 2022/7/11 14:19
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) // 启用方法级别的权限认证
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 登录失败响应处理器
     */
    @Autowired
    private LoginFailureHandler loginFailureHandler;//登录成功处理器

    /**
     * 登录成功响应处理器
     */
    @Autowired
    private LoginSuccessHandler loginSuccessHandler;//登录失败处理器

    /**
     * 登录验证码校验过滤器
     */
    @Autowired
    private CaptchaFilter captchaFilter;//验证码过滤器

    /**
     * 未认证请求处理器
     */
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 访问权限不足处理器
     */
    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * 认证用户信息加载服务
     */
    @Autowired
    private UserDetailsImpl userDetails;

    /**
     * 退出登录响应处理器
     */
    @Autowired
    private JwtLogoutSuccessHandler jwtLogoutSuccessHandler;

    /**
     * 匿名访问接口白名单配置
     */
    @Autowired
    private AuthUrlConfig config;

    /**
     * 告知security加密方式
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * 创建 JWT 认证过滤器并注入认证管理器
     *
     * @return JWT 认证过滤器
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        return new JwtAuthenticationFilter(authenticationManager());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 调试：打印白名单配置
        System.out.println("========== Security 配置加载 ==========");
        System.out.println("白名单 URL 配置：");
        for (String url : config.getUrl()) {
            System.out.println("  - " + url);
        }
        System.out.println("=======================================");
        
        http.cors().configurationSource(corsConfigurationSource())
                .and().csrf().disable()
                //登录配置 （成功处理器，失败处理器）
                .formLogin()
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .and()

                //禁用sessi
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)//session生成策略，不生成session

                .and()
                .authorizeRequests()
                // 将 courseSchedule 放在最前面，确保优先匹配
                .antMatchers("/api/courseSchedule/**").permitAll()
                .antMatchers(config.getUrl()).permitAll()//放行白名单
                .antMatchers("/api/studentInfo/synchronization").permitAll() // 新增：放行 /api/studentInfo/synchronization 接口
                .anyRequest().authenticated()//其他请求都需要进行验证
                .and()
                .logout()
                .logoutSuccessHandler(jwtLogoutSuccessHandler)

                //异常处理器
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)//401 还没登录
                .accessDeniedHandler(jwtAccessDeniedHandler)//403 权限不足

                //配置自定义过滤器
                .and()
                .addFilter(jwtAuthenticationFilter())//自动登录过滤器（带了token的）
                .addFilterBefore(captchaFilter, UsernamePasswordAuthenticationFilter.class)//配置图片验证码过滤器在密码验证之前
                .headers()
                .frameOptions().disable()
        ;
    }

    /**
     * 注入userDetails，然后security会与数据库完成密码的配对
     *
     * @param auth 当前登录认证信息
     * @throws Exception 读取、校验或处理相关数据失败时抛出
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetails);
    }

    /**
     * 百度找到的跨域方法（上面的configure()无法直接跨域）
     *
     * @return 跨域配置源
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        configuration.setAllowCredentials(false);//是否支持安全证书(必需参数)
        configuration.addAllowedOrigin(CorsConfiguration.ALL); //允许任何域名
        configuration.addAllowedHeader(CorsConfiguration.ALL); //允许任何请求头
        configuration.addAllowedMethod(CorsConfiguration.ALL); //允许任何请求方法
        source.registerCorsConfiguration("/**", configuration);//访问路径
        return source;
    }
}
