package com.edu.gateway.filter;

import com.edu.gateway.config.AuthProperties;
import com.edu.gateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局认证过滤器：校验 JWT 并将用户身份透传给下游微服务。
 * <p>
 * 设计要点：
 * - 通过 X-User-Id 请求头将身份信息传递给下游，下游服务无需重复解析 JWT
 * - 白名单路径（如登录、注册）跳过认证，由 AuthProperties 外部化配置
 * - order=-100，确保在路由过滤器之前执行，但在日志过滤器之后
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthProperties authProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhitelisted(path)) {
            log.debug("Skipping authentication for whitelisted path: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        String userId = jwtTokenProvider.getUserIdIfValid(token);
        if (userId == null) {
            log.warn("Invalid JWT token for path: {}", path);
            return onError(exchange, "Invalid or expired token");
        }
        String role = jwtTokenProvider.getRoleIfValid(token);

        // 将 userId 注入请求头，下游服务直接信任此 header 获取当前用户身份
        log.debug("Authenticated user {} for path: {}", userId, path);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(USER_ID_HEADER, userId)
                .headers(headers -> {
                    if (role != null && !role.isBlank()) {
                        headers.set(USER_ROLE_HEADER, role);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhitelisted(String path) {
        return authProperties.getWhitelist().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, HttpStatus.UNAUTHORIZED.value());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 优先级高于默认过滤器，但低于 RequestLogFilter(-200)，保证日志先记录
        return -100;
    }
}
