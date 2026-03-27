package com.edu.course.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应 DTO。
 * 与 user-service 的 /api/internal/users/{id} 接口响应结构保持一致。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
}
