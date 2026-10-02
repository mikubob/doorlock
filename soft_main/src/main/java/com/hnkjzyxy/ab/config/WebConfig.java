package com.hnkjzyxy.ab.config;

import com.hnkjzyxy.ab.interceptor.RepeatSubmitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 资源映射及重复提交拦截器配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 图片文件存储根目录
     */
    @Value("${upload.imgUrl}")
    private String imgUrl;

    /**
     * 佐证材料文件存储根目录
     */
    @Value("${upload.fileUrl}")
    private String fileUrl;

    /**
     * 建设项目材料文件存储根目录
     */
    @Value("${upload.conFileUrl}")
    private String conFileUrl;

    /**
     * 重复提交拦截器
     */
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/pic/file/**").addResourceLocations("file:" + imgUrl);
        registry.addResourceHandler("/pdf/file/**").addResourceLocations("file:" + fileUrl);
        registry.addResourceHandler("/pdf/conFile/**").addResourceLocations("file:" + conFileUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

}
