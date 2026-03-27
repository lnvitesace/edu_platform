package com.edu.platform.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Redis 缓存的用户认证信息（user:details:{userId}）
 *
 * <p>使用 Record 而非 UserPrincipal 直接序列化：
 * UserPrincipal 实现了 UserDetails 接口，包含 authorities 等复杂字段，
 * 序列化/反序列化容易出问题。Record 只存最小必要字段，干净可控。</p>
 */
public record CachedUserDetails(
    Long id,
    String username,
    String email,
    String password,
    String role
) {
    public static CachedUserDetails from(UserPrincipal principal) {
        return new CachedUserDetails(
            principal.getId(),
            principal.getUsername(),
            principal.getEmail(),
            principal.getPassword(),
            principal.getRole()
        );
    }

    public UserPrincipal toUserPrincipal() {
        return new UserPrincipal(
            id,
            username,
            email,
            password,
            role,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
