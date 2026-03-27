package com.edu.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求 DTO
 * 支持用户名或邮箱登录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /** 用户名或邮箱，用于身份识别 */
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    /** 密码 */
    @NotBlank(message = "Password is required")
    private String password;
}
