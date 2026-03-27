package com.edu.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLogFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private RequestLogFilter requestLogFilter;

    @BeforeEach
    void setUp() {
        requestLogFilter = new RequestLogFilter();
    }

    @Nested
    @DisplayName("Filter execution tests")
    class FilterExecutionTests {

        @Test
        @DisplayName("should set startTime attribute on exchange")
        void filter_setsStartTimeAttribute() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            Long startTime = exchange.getAttribute("startTime");
            assertNotNull(startTime);
            assertTrue(startTime > 0);
        }

        @Test
        @DisplayName("should call chain filter")
        void filter_callsChainFilter() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should handle GET requests")
        void filter_handlesGetRequest() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals(HttpMethod.GET, exchange.getRequest().getMethod());
        }

        @Test
        @DisplayName("should handle POST requests")
        void filter_handlesPostRequest() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/auth/login")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals(HttpMethod.POST, exchange.getRequest().getMethod());
        }

        @Test
        @DisplayName("should handle PUT requests")
        void filter_handlesPutRequest() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .put("/api/users/profile")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals(HttpMethod.PUT, exchange.getRequest().getMethod());
        }

        @Test
        @DisplayName("should handle DELETE requests")
        void filter_handlesDeleteRequest() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .delete("/api/courses/123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals(HttpMethod.DELETE, exchange.getRequest().getMethod());
        }
    }

    @Nested
    @DisplayName("Response logging tests")
    class ResponseLoggingTests {

        @Test
        @DisplayName("should complete after chain filter completes")
        void filter_completesAfterChain() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle successful response status")
        void filter_handlesSuccessfulResponse() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should handle error response status")
        void filter_handlesErrorResponse() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should handle null status code gracefully")
        void filter_handlesNullStatusCode() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Filter order tests")
    class FilterOrderTests {

        @Test
        @DisplayName("should have order -200 to execute before AuthenticationFilter")
        void getOrder_returnsHigherPriorityThanAuthFilter() {
            assertEquals(-200, requestLogFilter.getOrder());
        }

        @Test
        @DisplayName("should have lower order value than AuthenticationFilter (-100)")
        void getOrder_isLowerThanAuthFilterOrder() {
            assertTrue(requestLogFilter.getOrder() < -100);
        }
    }

    @Nested
    @DisplayName("Path handling tests")
    class PathHandlingTests {

        @Test
        @DisplayName("should handle root path")
        void filter_handlesRootPath() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals("/", exchange.getRequest().getURI().getPath());
        }

        @Test
        @DisplayName("should handle nested path")
        void filter_handlesNestedPath() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/users/123/profile")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals("/api/v1/users/123/profile", exchange.getRequest().getURI().getPath());
        }

        @Test
        @DisplayName("should handle path with query parameters")
        void filter_handlesPathWithQueryParams() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses?page=1&size=10")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .verifyComplete();

            assertEquals("/api/courses", exchange.getRequest().getURI().getPath());
        }
    }

    @Nested
    @DisplayName("Error propagation tests")
    class ErrorPropagationTests {

        @Test
        @DisplayName("should propagate error from chain")
        void filter_propagatesChainError() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException expectedException = new RuntimeException("Chain error");
            when(chain.filter(any())).thenReturn(Mono.error(expectedException));

            StepVerifier.create(requestLogFilter.filter(exchange, chain))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
