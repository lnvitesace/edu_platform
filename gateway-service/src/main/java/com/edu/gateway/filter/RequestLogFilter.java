package com.edu.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求日志过滤器，记录每个请求的方法、路径、状态码和耗时。
 * <p>
 * order=-200 保证最先执行，这样耗时统计能覆盖后续所有过滤器和路由的处理时间。
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String requestId = request.getId();

        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        log.info("[{}] >>> {} {}", requestId, method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value()
                        : 0;
                log.info("[{}] <<< {} {} - {} ({}ms)", requestId, method, path, statusCode, duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保耗时统计覆盖完整请求链路
        return -200;
    }
}
