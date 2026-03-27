package com.edu.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体类
 * 用于存储 JWT refresh token，支持 token 续期和多设备登录
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /** 主键 ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的用户，多对一关系（一个用户可有多个 refresh token） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 刷新令牌字符串，全局唯一 */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /** 过期时间 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 创建时间，自动生成 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
