package com.edu.course.client;

import com.edu.course.client.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户服务调用降级处理。
 *
 * 当 user-service 不可达或响应超时时，返回占位用户信息而非抛出异常。
 * 这种柔性设计确保课程列表即使在用户服务故障时也能正常展示，
 * 只是讲师名称显示为 "Unknown"。
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserResponse getUserById(Long id) {
        log.warn("Fallback: Unable to fetch user with id {}", id);
        return UserResponse.builder()
                .id(id)
                .username("Unknown")
                .nickname("Unknown User")
                .build();
    }
}
