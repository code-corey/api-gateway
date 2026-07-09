package com.codecore.gateway.plugin;

import com.codecore.gateway.auth.JwtValidationResult;
import com.codecore.gateway.auth.LocalJwtValidator;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/**
 * 内置 JWT 认证插件（Stage 5 仍打包在 gateway-core 内）。
 * <p>
 * 实现 {@link AuthPlugin} SPI，内部委托 {@link LocalJwtValidator} 完成 JWKS 验签。
 * Stage 6 将迁移为独立 JAR 插件。
 * </p>
 */
@Component
public class JwtAuthPlugin implements AuthPlugin {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PLUGIN_NAME = "jwt-auth";
    private static final String PLUGIN_VERSION = "1.0.0";

    private final LocalJwtValidator jwtValidator;
    private PluginContext pluginContext;

    /**
     * @param jwtValidator JWT 校验器。
     */
    public JwtAuthPlugin(LocalJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 内部保存上下文并记录插件启动日志。
     * </p>
     */
    @Override
    public void init(PluginContext context) {
        this.pluginContext = context;
        context.logInfo("JWT 认证插件已初始化: " + PLUGIN_NAME + " v" + PLUGIN_VERSION);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 从 Authorization 头提取 Bearer JWT，调用校验器验签并映射为 {@link AuthResult}。
     * </p>
     */
    @Override
    public AuthResult authenticate(AuthRequest request) {
        String authorization = request.getHeader("Authorization");
        String rawJwt = extractBearerToken(authorization);
        JwtValidationResult result = jwtValidator.validate(rawJwt);
        if (result.valid()) {
            return AuthResult.authenticated();
        }
        return AuthResult.unauthenticated(result.message());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreDestroy
    public void destroy() {
        if (pluginContext != null) {
            pluginContext.logInfo("JWT 认证插件已销毁: " + PLUGIN_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(PLUGIN_NAME, PLUGIN_VERSION, "code-corey", "0.0.1");
    }

    /**
     * 从 Authorization 头解析 Bearer JWT。
     *
     * @param authorization Authorization 头值。
     * @return JWT 字符串；格式不合法时返回 null。
     */
    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
