package com.edu.course.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * CORS 配置为允许所有来源，因为：
 * 1. 生产环境中请求经由 API Gateway 转发，真实 CORS 由网关控制
 * 2. 开发/测试环境需要前端直连服务调试
 *
 * 注意：此配置在 Spring Security 启用时可能被覆盖，需配合 Security CORS 配置使用。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
