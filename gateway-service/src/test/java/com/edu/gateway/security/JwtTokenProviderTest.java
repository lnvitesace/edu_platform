package com.edu.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "yourSecretKeyMustBeAtLeast256BitsLongForHS256AlgorithmToWorkProperlyWithJWT";
    private static final String DIFFERENT_SECRET = "aDifferentSecretKeyThatIsAlsoAtLeast256BitsLongForHS256AlgorithmTest";

    private JwtTokenProvider jwtTokenProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String generateToken(String subject, Date expiration) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    private String generateTokenWithDifferentKey(String subject) {
        SecretKey differentKey = Keys.hmacShaKeyFor(DIFFERENT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(differentKey)
                .compact();
    }

    @Nested
    @DisplayName("validateToken tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return true for valid token")
        void validateToken_withValidToken_returnsTrue() {
            String token = generateToken("user123", Date.from(Instant.now().plusSeconds(3600)));

            boolean result = jwtTokenProvider.validateToken(token);

            assertTrue(result);
        }

        @Test
        @DisplayName("should return false for expired token")
        void validateToken_withExpiredToken_returnsFalse() {
            String token = generateToken("user123", Date.from(Instant.now().minusSeconds(3600)));

            boolean result = jwtTokenProvider.validateToken(token);

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for malformed token")
        void validateToken_withMalformedToken_returnsFalse() {
            String malformedToken = "this.is.not.a.valid.jwt";

            boolean result = jwtTokenProvider.validateToken(malformedToken);

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for token with invalid signature")
        void validateToken_withInvalidSignature_returnsFalse() {
            String tokenWithDifferentKey = generateTokenWithDifferentKey("user123");

            boolean result = jwtTokenProvider.validateToken(tokenWithDifferentKey);

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for empty token")
        void validateToken_withEmptyToken_returnsFalse() {
            boolean result = jwtTokenProvider.validateToken("");

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for null token")
        void validateToken_withNullToken_returnsFalse() {
            boolean result = jwtTokenProvider.validateToken(null);

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for token with missing parts")
        void validateToken_withMissingParts_returnsFalse() {
            String incompleteToken = "eyJhbGciOiJIUzI1NiJ9";

            boolean result = jwtTokenProvider.validateToken(incompleteToken);

            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for token with random string")
        void validateToken_withRandomString_returnsFalse() {
            String randomString = "randomString123456";

            boolean result = jwtTokenProvider.validateToken(randomString);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken tests")
    class GetUserIdFromTokenTests {

        @Test
        @DisplayName("should extract userId from valid token")
        void getUserIdFromToken_withValidToken_returnsUserId() {
            String expectedUserId = "user123";
            String token = generateToken(expectedUserId, Date.from(Instant.now().plusSeconds(3600)));

            String userId = jwtTokenProvider.getUserIdFromToken(token);

            assertEquals(expectedUserId, userId);
        }

        @Test
        @DisplayName("should extract numeric userId from valid token")
        void getUserIdFromToken_withNumericUserId_returnsUserId() {
            String expectedUserId = "12345";
            String token = generateToken(expectedUserId, Date.from(Instant.now().plusSeconds(3600)));

            String userId = jwtTokenProvider.getUserIdFromToken(token);

            assertEquals(expectedUserId, userId);
        }

        @Test
        @DisplayName("should extract UUID userId from valid token")
        void getUserIdFromToken_withUuidUserId_returnsUserId() {
            String expectedUserId = "550e8400-e29b-41d4-a716-446655440000";
            String token = generateToken(expectedUserId, Date.from(Instant.now().plusSeconds(3600)));

            String userId = jwtTokenProvider.getUserIdFromToken(token);

            assertEquals(expectedUserId, userId);
        }

        @Test
        @DisplayName("should throw exception for expired token")
        void getUserIdFromToken_withExpiredToken_throwsException() {
            String token = generateToken("user123", Date.from(Instant.now().minusSeconds(3600)));

            assertThrows(Exception.class, () -> jwtTokenProvider.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("should throw exception for invalid token")
        void getUserIdFromToken_withInvalidToken_throwsException() {
            String invalidToken = "invalid.token.here";

            assertThrows(Exception.class, () -> jwtTokenProvider.getUserIdFromToken(invalidToken));
        }
    }

    @Nested
    @DisplayName("getUserIdIfValid tests")
    class GetUserIdIfValidTests {

        @Test
        @DisplayName("should return userId for valid token")
        void getUserIdIfValid_withValidToken_returnsUserId() {
            String expectedUserId = "user123";
            String token = generateToken(expectedUserId, Date.from(Instant.now().plusSeconds(3600)));

            String userId = jwtTokenProvider.getUserIdIfValid(token);

            assertEquals(expectedUserId, userId);
        }

        @Test
        @DisplayName("should return null for expired token")
        void getUserIdIfValid_withExpiredToken_returnsNull() {
            String token = generateToken("user123", Date.from(Instant.now().minusSeconds(3600)));

            assertNull(jwtTokenProvider.getUserIdIfValid(token));
        }

        @Test
        @DisplayName("should return null for invalid token")
        void getUserIdIfValid_withInvalidToken_returnsNull() {
            assertNull(jwtTokenProvider.getUserIdIfValid("invalid.token.here"));
        }

        @Test
        @DisplayName("should return null for null token")
        void getUserIdIfValid_withNullToken_returnsNull() {
            assertNull(jwtTokenProvider.getUserIdIfValid(null));
        }

        @Test
        @DisplayName("should return null for empty token")
        void getUserIdIfValid_withEmptyToken_returnsNull() {
            assertNull(jwtTokenProvider.getUserIdIfValid(""));
        }
    }
}
