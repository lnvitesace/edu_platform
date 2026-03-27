package com.edu.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证白名单配置，对应 app.auth.whitelist。
 * 白名单中的路径跳过 JWT 校验，支持 Ant 风格通配符（如 /api/auth/**）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private List<String> whitelist = new ArrayList<>();
}
