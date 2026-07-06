package com.codecore.gateway.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 认证失败响应写入工具。
 * <p>
 * 统一网关 401 JSON 响应格式，供各认证过滤器复用。
 * </p>
 */
public final class AuthResponseWriter {

    private AuthResponseWriter() {
    }

    /**
     * 向客户端写入 401 未认证 JSON 响应。
     *
     * @param exchange 当前请求上下文。
     * @param message  错误提示信息。
     * @return 响应写完的 Mono。
     */
    public static Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String safeMessage = message == null || message.isBlank() ? "未认证" : message.replace("\"", "\\\"");
        byte[] body = ("{\"code\":401,\"message\":\"" + safeMessage + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body))
        );
    }
}
