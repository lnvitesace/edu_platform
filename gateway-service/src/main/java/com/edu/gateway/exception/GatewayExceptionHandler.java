package com.edu.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局异常处理器，将未捕获异常统一转为 JSON 响应。
 * <p>
 * @Order(-1) 确保优先于 Spring 默认的 DefaultErrorWebExceptionHandler，
 * 避免返回 HTML 错误页。WebFlux 环境下不能使用 @ControllerAdvice，
 * 必须实现 WebExceptionHandler。
 */
@Slf4j
@Component
@Order(-1)
public class GatewayExceptionHandler implements WebExceptionHandler {

    @Override
    public @NonNull Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 响应已提交（如 SSE 流中途异常），无法再写入，只能向上传播
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = determineHttpStatus(ex);
        String message = determineMessage(ex);

        log.error("Gateway error: {} - {}", status, message, ex);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"path\":\"%s\"}",
                escapeJson(message),
                status.value(),
                exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatus determineHttpStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String determineMessage(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getReason() != null ? rse.getReason() : rse.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : "Internal Server Error";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
