package com.edu.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Resilience4j 熔断降级端点。
 * <p>
 * 当下游服务不可用触发熔断时，网关将请求转发到此控制器对应的路径，
 * 返回统一的 503 响应，避免客户端收到连接超时等不友好的错误。
 * 每个下游服务对应一个 fallback 方法，便于客户端区分哪个服务不可用。
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "User service is currently unavailable",
                        "service", "user-service",
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value()
                )));
    }

    @GetMapping("/course")
    public Mono<ResponseEntity<Map<String, Object>>> courseFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Course service is currently unavailable",
                        "service", "course-service",
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value()
                )));
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Search service is currently unavailable",
                        "service", "search-service",
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value()
                )));
    }
}
