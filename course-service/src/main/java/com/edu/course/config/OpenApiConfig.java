package com.edu.course.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 文档配置。
 *
 * 配置了两个服务器地址：
 * 1. 直连地址 (8002) - 用于开发调试，绕过网关直接访问服务
 * 2. 网关地址 (8080) - 生产环境入口，所有请求经网关鉴权后转发
 *
 * 安全方案说明：虽然文档中声明了 Bearer Token，但实际鉴权由网关完成，
 * 课程服务通过 X-User-Id/X-User-Role 请求头获取已验证的用户信息。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Course Service API")
                        .description("课程服务 API - 提供课程、章节、课时的增删改查功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduPlatform Team")
                                .email("contact@eduplatform.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8002").description("课程服务直连"),
                        new Server().url("http://localhost:8080").description("API 网关")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token (由网关传递 X-User-Id)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
