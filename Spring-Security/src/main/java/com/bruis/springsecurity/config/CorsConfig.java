package com.bruis.springsecurity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author LuoHaiYang
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许跨域访问的路径
        registry.addMapping("/**")
        // 允许跨域访问的源
        .allowedOrigins("*")
        // 允许请求方法
        .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
        // 预检间隔时间
        .maxAge(1680000)
        // 允许头部
        .allowedHeaders("*")
        // 允许发送cookie
        .allowCredentials(true);
    }
}
