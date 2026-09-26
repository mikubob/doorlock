package com.hnkjzyxy.ab.config;

import com.hnkjzyxy.ab.interceptor.RepeatSubmitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.imgUrl}")
    private String imgUrl;

    @Value("${upload.fileUrl}")
    private String fileUrl;

    @Value("${upload.conFileUrl}")
    private String conFileUrl;

    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/pic/file/**").addResourceLocations("file:" + imgUrl);
        registry.addResourceHandler("/pdf/file/**").addResourceLocations("file:" + fileUrl);
        registry.addResourceHandler("/pdf/conFile/**").addResourceLocations("file:" + conFileUrl);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

}
