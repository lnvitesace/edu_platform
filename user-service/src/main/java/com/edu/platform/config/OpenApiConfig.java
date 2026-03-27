package com.edu.platform.config;

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
 * OpenAPI (Swagger) 文档配置类。
 * <p>
 * 本配置类负责生成 API 文档，提供以下功能：
 * <ul>
 *   <li>API 基本信息描述（标题、版本、联系方式）</li>
 *   <li>服务器地址配置</li>
 *   <li>JWT Bearer Token 认证方案配置</li>
 * </ul>
 * <p>
 * 生成的文档可通过 /swagger-ui.html 或 /v3/api-docs 访问。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 文档配置 Bean。
     * <p>
     * 配置内容包括：
     * <ul>
     *   <li>API 标题和描述信息</li>
     *   <li>版本号和联系方式</li>
     *   <li>开源许可证信息</li>
     *   <li>可用的服务器地址（直连和网关）</li>
     *   <li>JWT Bearer Token 安全认证方案</li>
     * </ul>
     *
     * @return OpenAPI 配置实例
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // API 基本信息
                .info(new Info()
                        .title("User Service API")
                        .description("用户服务 API - 提供用户认证、授权和个人资料管理功能")
                        .version("1.0.0")
                        // 联系方式
                        .contact(new Contact()
                                .name("EduPlatform Team")
                                .email("contact@eduplatform.com"))
                        // 开源许可证
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // 可用服务器列表
                .servers(List.of(
                        // 用户服务直连地址（端口 8001）
                        new Server().url("http://localhost:8001").description("用户服务直连"),
                        // 通过 API 网关访问（端口 8080）
                        new Server().url("http://localhost:8080").description("API 网关")))
                // 安全组件配置
                .components(new Components()
                        // 添加 Bearer Token 认证方案
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                // 认证类型为 HTTP
                                .type(SecurityScheme.Type.HTTP)
                                // 使用 Bearer 方案
                                .scheme("bearer")
                                // Token 格式为 JWT
                                .bearerFormat("JWT")
                                .description("JWT Access Token")))
                // 全局应用 Bearer Token 认证
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
