package com.codecore.gateway.auth;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT 认证全局过滤器。
 * <p>
 * Stage 3 实现：从 Authorization 头提取 Bearer JWT，调用 {@link LocalJwtValidator} 进行本地公钥验签。
 * </p>
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewayAuthProperties authProperties;
    private final LocalJwtValidator jwtValidator;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * @param authProperties 网关认证配置。
     * @param jwtValidator   JWT 校验器。
     */
    public JwtAuthGlobalFilter(GatewayAuthProperties authProperties, LocalJwtValidator jwtValidator) {
        this.authProperties = authProperties;
        this.jwtValidator = jwtValidator;
    }

    /**
     * 在路由转发前执行 JWT 验签。
     * <p>
     * 内部判断认证开关与白名单后，解析 Bearer Token 并调用校验器；失败则返回 401。
     * </p>
     *
     * @param exchange 当前请求上下文。
     * @param chain    网关过滤器链。
     * @return 异步完成信号。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!authProperties.isEnabled() || !authProperties.getJwt().isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String rawJwt = extractBearerToken(authorization);
        JwtValidationResult result = jwtValidator.validate(rawJwt);
        if (!result.valid()) {
            return AuthResponseWriter.unauthorized(exchange, result.message());
        }

        return chain.filter(exchange);
    }

    /**
     * 判断路径是否在免认证白名单内。
     *
     * @param path 请求路径。
     * @return 在白名单内返回 true。
     */
    private boolean isExcluded(String path) {
        return authProperties.getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从 Authorization 头解析 Bearer JWT 字符串。
     *
     * @param authorization Authorization 头原始值。
     * @return JWT 字符串；格式不合法时返回 null。
     */
    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * @return 过滤器优先级，认证需先于路由执行。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
