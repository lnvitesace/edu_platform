package com.edu.gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private FallbackController fallbackController;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
    }

    @Nested
    @DisplayName("User service fallback tests")
    class UserFallbackTests {

        @Test
        @DisplayName("should return 503 for user service fallback")
        void userFallback_returns503() {
            StepVerifier.create(fallbackController.userFallback())
                    .assertNext(response -> {
                        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals("User service is currently unavailable", body.get("error"));
                        assertEquals("user-service", body.get("service"));
                        assertEquals(503, body.get("status"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return correct service name for user fallback")
        void userFallback_returnsCorrectServiceName() {
            StepVerifier.create(fallbackController.userFallback())
                    .assertNext(response -> {
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals("user-service", body.get("service"));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Course service fallback tests")
    class CourseFallbackTests {

        @Test
        @DisplayName("should return 503 for course service fallback")
        void courseFallback_returns503() {
            StepVerifier.create(fallbackController.courseFallback())
                    .assertNext(response -> {
                        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals("Course service is currently unavailable", body.get("error"));
                        assertEquals("course-service", body.get("service"));
                        assertEquals(503, body.get("status"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return correct service name for course fallback")
        void courseFallback_returnsCorrectServiceName() {
            StepVerifier.create(fallbackController.courseFallback())
                    .assertNext(response -> {
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals("course-service", body.get("service"));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Search service fallback tests")
    class SearchFallbackTests {

        @Test
        @DisplayName("should return 503 for search service fallback")
        void searchFallback_returns503() {
            StepVerifier.create(fallbackController.searchFallback())
                    .assertNext(response -> {
                        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals("Search service is currently unavailable", body.get("error"));
                        assertEquals("search-service", body.get("service"));
                        assertEquals(503, body.get("status"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return correct service name for search fallback")
        void searchFallback_returnsCorrectServiceName() {
            StepVerifier.create(fallbackController.searchFallback())
                    .assertNext(response -> {
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals("search-service", body.get("service"));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Response structure tests")
    class ResponseStructureTests {

        @Test
        @DisplayName("should return all required fields in response")
        void fallback_returnsAllRequiredFields() {
            StepVerifier.create(fallbackController.userFallback())
                    .assertNext(response -> {
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertTrue(body.containsKey("error"));
                        assertTrue(body.containsKey("service"));
                        assertTrue(body.containsKey("status"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return status as integer 503")
        void fallback_returnsStatusAsInteger() {
            StepVerifier.create(fallbackController.courseFallback())
                    .assertNext(response -> {
                        Map<String, Object> body = response.getBody();
                        assertNotNull(body);
                        assertEquals(503, body.get("status"));
                        assertTrue(body.get("status") instanceof Integer);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return ResponseEntity wrapped in Mono")
        void fallback_returnsMonoOfResponseEntity() {
            StepVerifier.create(fallbackController.userFallback())
                    .expectNextMatches(response -> response instanceof ResponseEntity)
                    .verifyComplete();
        }
    }
}
