package com.edu.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 教育平台用户服务启动类 - Spring Boot 应用程序入口
 *
 * <p>用户服务（User Service）是教育平台微服务架构中的核心服务之一，
 * 负责用户认证、授权和个人资料管理。服务默认运行在 8001 端口。</p>
 *
 * <h3>服务职责:</h3>
 * <ul>
 *   <li>用户注册与登录认证</li>
 *   <li>JWT 令牌的签发与刷新</li>
 *   <li>用户信息和个人资料管理</li>
 *   <li>为其他微服务提供用户信息查询接口</li>
 * </ul>
 *
 * <h3>技术栈:</h3>
 * <ul>
 *   <li>Spring Boot 4.0.1 - 应用框架</li>
 *   <li>Spring Security - 安全认证</li>
 *   <li>Spring Data JPA - 数据持久化</li>
 *   <li>MySQL - 关系数据库</li>
 *   <li>Redis - 会话缓存</li>
 *   <li>Nacos - 服务注册与发现</li>
 * </ul>
 *
 * <h3>启用的特性:</h3>
 * <ul>
 *   <li>{@code @SpringBootApplication} - 启用自动配置、组件扫描和配置类</li>
 *   <li>{@code @EnableJpaAuditing} - 启用 JPA 审计功能，自动填充 createdAt、updatedAt 字段</li>
 * </ul>
 *
 * @author EduPlatform
 * @since 1.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class EduPlatformApplication {

    /**
     * 应用程序主入口方法
     *
     * <p>启动 Spring Boot 应用，初始化 Spring 容器，
     * 加载所有配置并启动内嵌的 Tomcat 服务器。</p>
     *
     * @param args 命令行参数，可用于覆盖配置属性
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(EduPlatformApplication.class, args);
    }
}
