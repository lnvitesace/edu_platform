package com.edu.platform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌提供者 - 生成与验证 JWT
 *
 * <p>使用 HMAC-SHA 对称签名（而非 RSA 非对称）：单服务场景下无需分发公钥，
 * 性能更好。如果后续需要跨服务验证 token，应切换为 RSA。</p>
 *
 * <p>Access Token 包含用户身份 Claims（username, email, role），
 * 减少认证过程中的 DB 查询；Refresh Token 只含 userId，最小化泄露风险。</p>
 */
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Getter
    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId()))
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getRole())
                // jti 用于令牌唯一标识，配合 Redis session 可检测重放
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Refresh Token 不含 username/email/role，最小化敏感信息暴露
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            throw new JwtException("Invalid JWT token");
        }
        return Long.parseLong(claims.getSubject());
    }

    public Long getUserIdIfValid(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        return parseClaims(token) != null;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
