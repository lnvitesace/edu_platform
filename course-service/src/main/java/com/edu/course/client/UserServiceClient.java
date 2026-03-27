package com.edu.course.client;

import com.edu.course.client.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端。
 *
 * 通过 Nacos 服务发现解析 user-service 地址，无需硬编码 URL。
 * 调用受服务间令牌保护的 internal API，用于服务间内部通信。
 * fallback 指定降级处理类，在 user-service 不可用时返回占位数据。
 */
@FeignClient(
        name = "user-service",
        fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    @GetMapping("/api/internal/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);
}
