package com.edu.gateway.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GatewayExceptionHandlerTest {

    private GatewayExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GatewayExceptionHandler();
    }

    @Nested
    @DisplayName("ResponseStatusException handling tests")
    class ResponseStatusExceptionTests {

        @Test
        @DisplayName("should return 404 for NOT_FOUND ResponseStatusException")
        void handle_notFoundException_returns404() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/999")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should return 400 for BAD_REQUEST ResponseStatusException")
        void handle_badRequestException_returns400() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/auth/register")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should return 401 for UNAUTHORIZED ResponseStatusException")
        void handle_unauthorizedException_returns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should return 403 for FORBIDDEN ResponseStatusException")
        void handle_forbiddenException_returns403() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .delete("/api/admin/users/1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should return 503 for SERVICE_UNAVAILABLE ResponseStatusException")
        void handle_serviceUnavailableException_returns503() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should use exception message as reason when available")
        void handle_withReason_usesReason() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom error message");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("Custom error message"));
        }
    }

    @Nested
    @DisplayName("Generic exception handling tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 for generic RuntimeException")
        void handle_runtimeException_returns500() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Unexpected error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should return 500 for generic Exception")
        void handle_genericException_returns500() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/courses")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            Exception ex = new Exception("Generic error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should use exception message for generic exceptions")
        void handle_genericException_usesExceptionMessage() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Something went wrong");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("Something went wrong"));
        }

        @Test
        @DisplayName("should return default message when exception message is null")
        void handle_nullMessage_returnsDefaultMessage() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException((String) null);

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("Internal Server Error"));
        }
    }

    @Nested
    @DisplayName("Response format tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("should set content type to JSON")
        void handle_setsJsonContentType() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
        }

        @Test
        @DisplayName("should include path in response body")
        void handle_includesPathInResponse() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/users/123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("/api/users/123"));
        }

        @Test
        @DisplayName("should include status code in response body")
        void handle_includesStatusInResponse() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("404"));
        }

        @Test
        @DisplayName("should return valid JSON format")
        void handle_returnsValidJson() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Test error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.startsWith("{"));
            assertTrue(body.endsWith("}"));
            assertTrue(body.contains("\"error\""));
            assertTrue(body.contains("\"status\""));
            assertTrue(body.contains("\"path\""));
        }
    }

    @Nested
    @DisplayName("JSON escaping tests")
    class JsonEscapingTests {

        @Test
        @DisplayName("should escape double quotes in error message")
        void handle_escapesDoubleQuotes() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Error with \"quotes\"");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("\\\"quotes\\\""));
        }

        @Test
        @DisplayName("should escape backslashes in error message")
        void handle_escapesBackslashes() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Error with \\backslash");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("\\\\backslash"));
        }

        @Test
        @DisplayName("should escape newlines in error message")
        void handle_escapesNewlines() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Error with\nnewline");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("\\n"));
        }

        @Test
        @DisplayName("should escape tabs in error message")
        void handle_escapesTabs() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("Error with\ttab");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
            String body = response.getBodyAsString().block();
            assertTrue(body.contains("\\t"));
        }
    }

    @Nested
    @DisplayName("Committed response tests")
    class CommittedResponseTests {

        @Test
        @DisplayName("should propagate error when response is already committed")
        void handle_committedResponse_propagatesError() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // Set the response as committed
            exchange.getResponse().setComplete().block();

            RuntimeException ex = new RuntimeException("Error after commit");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Different HTTP methods tests")
    class HttpMethodTests {

        @Test
        @DisplayName("should handle exception for POST request")
        void handle_postRequest_handlesException() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/auth/login")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("POST error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should handle exception for PUT request")
        void handle_putRequest_handlesException() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .put("/api/users/profile")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("PUT error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        }

        @Test
        @DisplayName("should handle exception for DELETE request")
        void handle_deleteRequest_handlesException() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .delete("/api/courses/123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            RuntimeException ex = new RuntimeException("DELETE error");

            StepVerifier.create(exceptionHandler.handle(exchange, ex))
                    .verifyComplete();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        }
    }
}
