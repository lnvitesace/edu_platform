package com.edu.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

/**
 * 网关核心配置：限流键解析与路由定义（Java DSL）。
 * <p>
 * 路由使用 Java DSL 统一定义，避免与 YAML routes 并存时产生重复/冲突。
 */
@Configuration
public class GatewayConfig {

    /**
     * 限流键解析策略：已认证用户按 userId 限流，未认证按 IP 限流。
     * 这样既能防止单用户刷接口，又不会让共享 IP（如公司出口）下的不同用户互相影响。
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            // 未认证请求回退到 IP 限流
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter(
            @Value("${app.rate-limit.replenish-rate:10}") int replenishRate,
            @Value("${app.rate-limit.burst-capacity:20}") long burstCapacity,
            @Value("${app.rate-limit.requested-tokens:1}") int requestedTokens
    ) {
        return new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
    }

    /**
     * 编程式路由定义，使用 "lb://" 前缀走 Nacos 服务发现负载均衡。
     */
    @Bean
    public RouteLocator customRouteLocator(
            RouteLocatorBuilder builder,
            KeyResolver userKeyResolver,
            RedisRateLimiter redisRateLimiter
    ) {
        return builder.routes()
                .route("user-service-auth", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setKeyResolver(userKeyResolver);
                                    c.setRateLimiter(redisRateLimiter);
                                })
                                .circuitBreaker(c -> c
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user")
                                )
                        )
                        .uri("lb://user-service"))
                .route("user-service-users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setKeyResolver(userKeyResolver);
                                    c.setRateLimiter(redisRateLimiter);
                                })
                                .circuitBreaker(c -> c
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user")
                                )
                        )
                        .uri("lb://user-service"))
                .route("course-service", r -> r
                        .path("/api/courses/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setKeyResolver(userKeyResolver);
                                    c.setRateLimiter(redisRateLimiter);
                                })
                                .circuitBreaker(c -> c
                                        .setName("courseServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/course")
                                )
                        )
                        .uri("lb://course-service"))
                .route("enrollment-service", r -> r
                        .path("/api/enrollments/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setKeyResolver(userKeyResolver);
                                    c.setRateLimiter(redisRateLimiter);
                                })
                                .circuitBreaker(c -> c
                                        .setName("courseServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/course")
                                )
                        )
                        .uri("lb://course-service"))
                .route("progress-service", r -> r
                        .path("/api/progress/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setKeyResolver(userKeyResolver);
                                    c.setRateLimiter(redisRateLimiter);
                                })
                                .circuitBreaker(c -> c
                                        .setName("courseServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/course")
                                )
                        )
                        .uri("lb://course-service"))
                .route("search-service", r -> r
                        .path("/api/search/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setKeyResolver(userKeyResolver);
                                    c.setRateLimiter(redisRateLimiter);
                                })
                                .circuitBreaker(c -> c
                                        .setName("searchServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/search")
                                )
                        )
                        .uri("lb://search-service"))
                .build();
    }
}
