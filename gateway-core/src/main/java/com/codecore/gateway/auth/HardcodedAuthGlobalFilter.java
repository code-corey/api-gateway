package com.codecore.gateway.auth;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 硬编码 Bearer Token 认证全局过滤器。
 * <p>
 * Stage 2 教学实现：在路由转发前检查 Authorization 头，无 Token 或 Token 不匹配时返回 401。
 * Actuator 等配置的路径可跳过认证。
 * </p>
 */
@Component
public class HardcodedAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewayAuthProperties authProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 注入认证配置。
     *
     * @param authProperties 网关认证配置项。
     */
    public HardcodedAuthGlobalFilter(GatewayAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 在请求进入路由匹配与转发之前执行认证校验。
     * <p>
     * 内部依次判断：认证是否启用 → 路径是否在白名单 → Authorization 头是否合法。
     * 校验失败时直接写 401 响应并结束请求；通过则交给后续过滤器链继续处理。
     * </p>
     *
     * @param exchange 当前请求上下文。
     * @param chain    网关过滤器链。
     * @return 异步完成信号。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!isValidToken(authorization)) {
            return unauthorized(exchange);
        }

        return chain.filter(exchange);
    }

    /**
     * 判断请求路径是否命中免认证白名单。
     *
     * @param path 请求路径。
     * @return 在白名单中返回 true。
     */
    private boolean isExcluded(String path) {
        return authProperties.getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 校验 Authorization 头是否为配置的 Bearer Token。
     *
     * @param authorization Authorization 请求头原始值。
     * @return 合法返回 true。
     */
    private boolean isValidToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return false;
        }
        String expected = BEARER_PREFIX + authProperties.getToken();
        return expected.equals(authorization);
    }

    /**
     * 构造 401 未认证响应并写入统一 JSON 错误体。
     *
     * @param exchange 当前请求上下文。
     * @return 响应写完的 Mono。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
                {"code":401,"message":"未认证，请提供有效的 Bearer Token"}
                """.strip().getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body))
        );
    }

    /**
     * 过滤器执行顺序，数值越小越先执行。
     * <p>
     * 认证应在路由转发之前完成，因此使用较高优先级（较小 order 值）。
     * </p>
     *
     * @return 顺序值。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
