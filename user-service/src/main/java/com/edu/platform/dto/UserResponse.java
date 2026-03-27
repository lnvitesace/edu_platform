package com.edu.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应 DTO
 * 返回用户基本信息，空字段不序列化
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱地址 */
    private String email;

    /** 名 */
    private String firstName;

    /** 姓 */
    private String lastName;

    /** 手机号码 */
    private String phone;

    /** 头像 URL */
    private String avatarUrl;

    /** 用户角色：STUDENT、INSTRUCTOR、ADMIN */
    private String role;

    /** 账号状态：ACTIVE、INACTIVE、SUSPENDED */
    private String status;

    /** 邮箱是否已验证 */
    private Boolean emailVerified;

    /** 账号创建时间 */
    private LocalDateTime createdAt;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 用户扩展资料 */
    private UserProfileResponse profile;
}
