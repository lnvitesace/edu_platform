package com.edu.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 存储平台用户的基本信息，包括账号凭证、个人资料和状态信息。
 * 与 UserProfile 存在一对一关系，用于存储扩展的用户资料。
 * </p>
 *
 * @author EduPlatform
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * 用户唯一标识，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名，用于登录，全局唯一
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 邮箱地址，用于登录和通知，全局唯一
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 密码，BCrypt 加密存储
     */
    @Column(nullable = false)
    private String password;

    /**
     * 名字
     */
    @Column(name = "first_name", length = 50)
    private String firstName;

    /**
     * 姓氏
     */
    @Column(name = "last_name", length = 50)
    private String lastName;

    /**
     * 手机号码
     */
    @Column(length = 20)
    private String phone;

    /**
     * 头像 URL
     */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * 用户角色：STUDENT(学生)、INSTRUCTOR(讲师)、ADMIN(管理员)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.STUDENT;

    /**
     * 账号状态：ACTIVE(正常)、INACTIVE(未激活)、SUSPENDED(暂停)、DELETED(已删除)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 邮箱是否已验证
     */
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * 账号创建时间，自动填充
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间，自动更新
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 用户扩展资料，延迟加载
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        /** 学生 - 可浏览和学习课程 */
        STUDENT,
        /** 讲师 - 可创建和管理课程 */
        INSTRUCTOR,
        /** 管理员 - 拥有系统最高权限 */
        ADMIN
    }

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        /** 正常状态 */
        ACTIVE,
        /** 未激活，需要邮箱验证 */
        INACTIVE,
        /** 已暂停，违规用户 */
        SUSPENDED,
        /** 已删除，软删除 */
        DELETED
    }
}
