package com.edu.gateway.filter;

import com.edu.gateway.config.AuthProperties;
import com.edu.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GatewayFilterChain chain;

    private AuthenticationFilter authenticationFilter;
    private AuthProperties authProperties;

    private static final List<String> WHITELIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/public/**",
            "/actuator/health",
            "/fallback/**"
    );

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setWhitelist(WHITELIST);
        authenticationFilter = new AuthenticationFilter(jwtTokenProvider, authProperties);
    }

    @Nested
    @DisplayName("Whitelist path tests")
    class WhitelistPathTests {

        @Test
        @DisplayName("should skip authentication for /api/auth/login")
        void filter_withLoginPath_skipsAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/auth/login")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("should skip authentication for /api/auth/register")
        void filter_withRegisterPath_skipsAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/auth/register")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("should skip authentication for /api/auth/refresh")
        void filter_withRefreshPath_skipsAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/auth/refresh")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("should skip authentication for /api/public/** pattern")
        void filter_withPublicPath_skipsAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/public/some/resource")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("should skip authentication for /actuator/health")
        void filter_withActuatorHealthPath_skipsAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/actuator/health")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("should skip authentication for /fallback/** pattern")
        void filter_withFallbackPath_skipsAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/fallback/user")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Nested
    @DisplayName("Missing or invalid Authorization header tests")
    class MissingAuthorizationHeaderTests {

        @Test
        @DisplayName("should return 401 when Authorization header is missing")
        void filter_withMissingAuthHeader_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header has no Bearer prefix")
        void filter_withNoBearerPrefix_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "token123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header has wrong prefix")
        void filter_withWrongPrefix_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header is empty")
        void filter_withEmptyAuthHeader_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header has empty Bearer token")
        void filter_withEmptyBearerToken_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(jwtTokenProvider.getUserIdIfValid("")).thenReturn(null);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(jwtTokenProvider).getUserIdIfValid("");
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Invalid token tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("should return 401 when token is invalid")
        void filter_withInvalidToken_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalidToken")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(jwtTokenProvider.getUserIdIfValid("invalidToken")).thenReturn(null);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(jwtTokenProvider).getUserIdIfValid("invalidToken");
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when token is expired")
        void filter_withExpiredToken_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer expiredToken")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(jwtTokenProvider.getUserIdIfValid("expiredToken")).thenReturn(null);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(jwtTokenProvider).getUserIdIfValid("expiredToken");
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Valid token tests")
    class ValidTokenTests {

        @Test
        @DisplayName("should add X-User-Id header when token is valid")
        void filter_withValidToken_addsUserIdHeader() {
            String validToken = "validToken123";
            String userId = "user456";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(jwtTokenProvider.getUserIdIfValid(validToken)).thenReturn(userId);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(jwtTokenProvider).getUserIdIfValid(validToken);
            verify(chain).filter(argThat(ex -> {
                ServerHttpRequest mutatedRequest = ex.getRequest();
                String userIdHeader = mutatedRequest.getHeaders().getFirst("X-User-Id");
                return userId.equals(userIdHeader);
            }));
        }

        @Test
        @DisplayName("should add X-User-Role header when role is present in token")
        void filter_withValidToken_addsUserRoleHeader() {
            String validToken = "validToken123";
            String userId = "user456";
            String role = "ADMIN";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(jwtTokenProvider.getUserIdIfValid(validToken)).thenReturn(userId);
            when(jwtTokenProvider.getRoleIfValid(validToken)).thenReturn(role);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(argThat(ex -> {
                ServerHttpRequest mutatedRequest = ex.getRequest();
                return role.equals(mutatedRequest.getHeaders().getFirst("X-User-Role"));
            }));
        }

        @Test
        @DisplayName("should pass through with valid token on protected path")
        void filter_withValidTokenOnProtectedPath_passesThrough() {
            String validToken = "validToken789";
            String userId = "user789";
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/courses/create")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(jwtTokenProvider.getUserIdIfValid(validToken)).thenReturn(userId);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("should return 401 when userId cannot be extracted")
        void filter_withValidTokenButUserIdMissing_returns401() {
            String validToken = "validToken123";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(jwtTokenProvider.getUserIdIfValid(validToken)).thenReturn(null);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Filter order tests")
    class FilterOrderTests {

        @Test
        @DisplayName("should have order -100 for high priority")
        void getOrder_returnsHighPriority() {
            assertEquals(-100, authenticationFilter.getOrder());
        }
    }

    @Nested
    @DisplayName("Response body tests")
    class ResponseBodyTests {

        @Test
        @DisplayName("should return JSON error response with correct format")
        void filter_withMissingAuth_returnsJsonErrorResponse() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(authenticationFilter.filter(exchange, chain))
                    .verifyComplete();

            ServerHttpResponse response = exchange.getResponse();
            assertEquals("application/json", response.getHeaders().getContentType().toString());
        }
    }
}
