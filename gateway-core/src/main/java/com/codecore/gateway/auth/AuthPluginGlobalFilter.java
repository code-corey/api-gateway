package com.codecore.gateway.auth;

import com.codecore.gateway.plugin.AuthPluginManager;
import com.codecore.gateway.plugin.AuthRequest;
import com.codecore.gateway.plugin.AuthResult;
import com.codecore.gateway.plugin.AuthStatus;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证插件全局过滤器。
 * <p>
 * Stage 5：通过 {@link AuthPluginManager} 调用 {@link com.codecore.gateway.plugin.AuthPlugin} SPI，
 * 不再在 Filter 内硬编码 JWT 逻辑。
 * </p>
 */
@Component
public class AuthPluginGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayAuthProperties authProperties;
    private final AuthPluginManager authPluginManager;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * @param authProperties    网关认证配置。
     * @param authPluginManager 认证插件管理器。
     */
    public AuthPluginGlobalFilter(GatewayAuthProperties authProperties, AuthPluginManager authPluginManager) {
        this.authProperties = authProperties;
        this.authPluginManager = authPluginManager;
    }

    /**
     * 在路由转发前调用当前认证插件。
     * <p>
     * 内部构造 {@link AuthRequest}，执行 authenticate，失败则写 401。
     * </p>
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

        AuthRequest authRequest = toAuthRequest(exchange.getRequest());
        AuthResult result = authPluginManager.getCurrentPlugin().authenticate(authRequest);

        if (result.isAuthenticated()) {
            ServerWebExchange mutated = applyForwardHeaders(exchange, result.forwardHeaders());
            return chain.filter(mutated);
        }

        if (result.status() == AuthStatus.ERROR) {
            return AuthResponseWriter.unauthorized(exchange, result.message() != null ? result.message() : "认证插件错误");
        }

        return AuthResponseWriter.unauthorized(exchange, result.message());
    }

    /**
     * 将 Spring 请求转换为插件 SPI 请求模型。
     *
     * @param request 网关 HTTP 请求。
     * @return 认证请求。
     */
    private AuthRequest toAuthRequest(ServerHttpRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        request.getHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });

        String clientIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : null;
        String traceId = request.getHeaders().getFirst("traceparent");

        return new AuthRequest(
                request.getPath().value(),
                request.getMethod() != null ? request.getMethod().name() : null,
                clientIp,
                traceId,
                headers
        );
    }

    /**
     * 将插件返回的 Header 写入下游转发请求。
     *
     * @param exchange        当前交换对象。
     * @param forwardHeaders  待转发 Header。
     * @return 变更后的 exchange。
     */
    private ServerWebExchange applyForwardHeaders(ServerWebExchange exchange, Map<String, String> forwardHeaders) {
        if (forwardHeaders == null || forwardHeaders.isEmpty()) {
            return exchange;
        }
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        forwardHeaders.forEach(builder::header);
        return exchange.mutate().request(builder.build()).build();
    }

    private boolean isExcluded(String path) {
        return authProperties.getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
