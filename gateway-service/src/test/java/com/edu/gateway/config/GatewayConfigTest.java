package com.edu.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class GatewayConfigTest {

    private GatewayConfig gatewayConfig;
    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        gatewayConfig = new GatewayConfig();
        keyResolver = gatewayConfig.userKeyResolver();
    }

    @Nested
    @DisplayName("KeyResolver with X-User-Id header")
    class KeyResolverWithUserIdTests {

        @Test
        @DisplayName("should return userId when X-User-Id header is present")
        void resolve_withUserIdHeader_returnsUserId() {
            String expectedUserId = "user123";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses")
                    .header("X-User-Id", expectedUserId)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext(expectedUserId)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return numeric userId when present")
        void resolve_withNumericUserId_returnsUserId() {
            String expectedUserId = "12345";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .header("X-User-Id", expectedUserId)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext(expectedUserId)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return UUID userId when present")
        void resolve_withUuidUserId_returnsUserId() {
            String expectedUserId = "550e8400-e29b-41d4-a716-446655440000";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/orders")
                    .header("X-User-Id", expectedUserId)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext(expectedUserId)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("KeyResolver fallback to IP address")
    class KeyResolverFallbackTests {

        @Test
        @DisplayName("should return IP address when X-User-Id header is missing")
        void resolve_withoutUserIdHeader_returnsIpAddress() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/public/courses")
                    .remoteAddress(new InetSocketAddress("192.168.1.100", 12345))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("192.168.1.100")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return localhost IP when connecting from localhost")
        void resolve_fromLocalhost_returnsLocalhostIp() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/auth/login")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 54321))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("127.0.0.1")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return 'unknown' when remote address is null")
        void resolve_withNullRemoteAddress_returnsUnknown() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/auth/register")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("unknown")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("KeyResolver priority tests")
    class KeyResolverPriorityTests {

        @Test
        @DisplayName("should prefer X-User-Id over IP address when both available")
        void resolve_withBothUserIdAndIp_prefersUserId() {
            String expectedUserId = "user456";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses")
                    .header("X-User-Id", expectedUserId)
                    .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext(expectedUserId)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Bean creation tests")
    class BeanCreationTests {

        @Test
        @DisplayName("should create non-null KeyResolver bean")
        void userKeyResolver_createsNonNullBean() {
            assertNotNull(keyResolver);
        }

        @Test
        @DisplayName("should create KeyResolver that returns Mono")
        void userKeyResolver_returnsMono() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-User-Id", "testUser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            assertNotNull(keyResolver.resolve(exchange));
        }
    }

    @Nested
    @DisplayName("Different HTTP methods tests")
    class HttpMethodTests {

        @Test
        @DisplayName("should resolve key for GET request")
        void resolve_getRequest_resolvesKey() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses")
                    .header("X-User-Id", "getUser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("getUser")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should resolve key for POST request")
        void resolve_postRequest_resolvesKey() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/auth/login")
                    .header("X-User-Id", "postUser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("postUser")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should resolve key for PUT request")
        void resolve_putRequest_resolvesKey() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .put("/api/users/profile")
                    .header("X-User-Id", "putUser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("putUser")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should resolve key for DELETE request")
        void resolve_deleteRequest_resolvesKey() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .delete("/api/courses/123")
                    .header("X-User-Id", "deleteUser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(keyResolver.resolve(exchange))
                    .expectNext("deleteUser")
                    .verifyComplete();
        }
    }
}
