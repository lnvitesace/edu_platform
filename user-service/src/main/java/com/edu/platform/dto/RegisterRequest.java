package com.edu.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求 DTO
 * 包含注册所需的用户名、邮箱、密码等信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /** 用户名，3-50 字符，仅支持字母、数字和下划线 */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    /** 邮箱地址，用于账号验证和找回密码 */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /** 密码，8-100 字符，需包含大小写字母和数字 */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
    )
    private String password;

    /** 名 */
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    /** 姓 */
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    /** 手机号码 */
    @Pattern(regexp = "^[0-9+\\-()\\s]*$", message = "Phone number format is invalid")
    private String phone;

    /** 用户角色：STUDENT、INSTRUCTOR、ADMIN */
    private String role;
}
