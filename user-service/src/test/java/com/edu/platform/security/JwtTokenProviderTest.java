package com.edu.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private static final String JWT_SECRET = "testSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong12345678";
    private static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours
    private static final long REFRESH_EXPIRATION_MS = 604800000L; // 7 days
    private static final Long USER_ID = 1L;

    private JwtTokenProvider tokenProvider;
    private SecretKey signingKey;
    private UserPrincipal userPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = new JwtTokenProvider();
        signingKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        setField(tokenProvider, "jwtSecret", JWT_SECRET);
        setField(tokenProvider, "jwtExpirationMs", JWT_EXPIRATION_MS);
        setField(tokenProvider, "refreshExpirationMs", REFRESH_EXPIRATION_MS);
        setField(tokenProvider, "signingKey", signingKey);

        userPrincipal = new UserPrincipal(
                USER_ID,
                "testuser",
                "test@example.com",
                "password",
                "STUDENT",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Generate access token - contains expected claims")
    void generateAccessToken_ContainsExpectedClaims() {
        String token = tokenProvider.generateAccessToken(authentication);

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(USER_ID));
        assertThat(claims.get("username", String.class)).isEqualTo("testuser");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("STUDENT");
        assertThat(claims.get("jti", String.class)).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Generate access token - has correct expiration")
    void generateAccessToken_HasCorrectExpiration() {
        long beforeGeneration = System.currentTimeMillis();
        String token = tokenProvider.generateAccessToken(authentication);
        long afterGeneration = System.currentTimeMillis();

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long expectedMinExpiration = beforeGeneration + JWT_EXPIRATION_MS - 1000;
        long expectedMaxExpiration = afterGeneration + JWT_EXPIRATION_MS + 1000;
        long actualExpiration = claims.getExpiration().getTime();

        assertThat(actualExpiration).isBetween(expectedMinExpiration, expectedMaxExpiration);
    }

    @Test
    @DisplayName("Generate refresh token - contains userId")
    void generateRefreshToken_ContainsUserId() {
        String token = tokenProvider.generateRefreshToken(USER_ID);

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(USER_ID));
        assertThat(claims.get("jti", String.class)).isNotNull();
    }

    @Test
    @DisplayName("Generate access token - unique jti per call")
    void generateAccessToken_UniqueJtiPerCall() {
        String token1 = tokenProvider.generateAccessToken(authentication);
        String token2 = tokenProvider.generateAccessToken(authentication);

        Claims claims1 = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token1).getPayload();
        Claims claims2 = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token2).getPayload();

        assertThat(claims1.get("jti", String.class)).isNotEqualTo(claims2.get("jti", String.class));
    }

    @Test
    @DisplayName("Get userId from token - valid token returns userId")
    void getUserIdFromToken_ValidToken_ReturnsUserId() {
        String token = tokenProvider.generateAccessToken(authentication);

        Long extractedUserId = tokenProvider.getUserIdFromToken(token);

        assertThat(extractedUserId).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Validate token - valid token returns true")
    void validateToken_ValidToken_ReturnsTrue() {
        String token = tokenProvider.generateAccessToken(authentication);

        boolean isValid = tokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Validate token - expired token returns false")
    void validateToken_ExpiredToken_ReturnsFalse() {
        String expiredToken = Jwts.builder()
                .subject(String.valueOf(USER_ID))
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 50000))
                .signWith(signingKey)
                .compact();

        boolean isValid = tokenProvider.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate token - tampered token returns false")
    void validateToken_TamperedToken_ReturnsFalse() {
        String token = tokenProvider.generateAccessToken(authentication);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        boolean isValid = tokenProvider.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate token - malformed token returns false")
    void validateToken_MalformedToken_ReturnsFalse() {
        boolean isValid = tokenProvider.validateToken("not.a.valid.jwt.token");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate token - null token returns false")
    void validateToken_NullToken_ReturnsFalse() {
        boolean isValid = tokenProvider.validateToken(null);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate token - empty token returns false")
    void validateToken_EmptyToken_ReturnsFalse() {
        boolean isValid = tokenProvider.validateToken("");

        assertThat(isValid).isFalse();
    }
}
