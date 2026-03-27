package com.edu.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应 DTO
 * 登录或刷新 token 成功后返回的数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** JWT 访问令牌 */
    private String accessToken;

    /** JWT 刷新令牌 */
    private String refreshToken;

    /** 令牌类型，固定为 Bearer */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 访问令牌有效期（秒） */
    private Long expiresIn;

    /** 用户基本信息 */
    private UserResponse user;
}
