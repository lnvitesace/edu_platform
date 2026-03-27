package com.edu.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关层 JWT 令牌校验器（只读）。
 * <p>
 * 职责仅限于验签和提取 claims，不负责签发令牌——签发由 user-service 完成。
 * 密钥必须与 user-service 配置的 app.jwt.secret 保持一致。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        return parseClaims(token) != null;
    }

    /**
     * Parse once for "validate + extract". Returns null when invalid/expired.
     */
    public String getUserIdIfValid(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getRoleIfValid(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    public String getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
        return claims.getSubject();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            // Keep this low-noise; the filter decides when/what to log.
            log.debug("JWT parse failed: {}", ex.getMessage());
            return null;
        }
    }
}
