package com.codecore.gateway.plugin.jwt;

import com.codecore.gateway.plugin.AuthPlugin;
import com.codecore.gateway.plugin.AuthRequest;
import com.codecore.gateway.plugin.AuthResult;
import com.codecore.gateway.plugin.PluginContext;
import com.codecore.gateway.plugin.PluginMetadata;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证插件（Stage 6 独立 JAR）。
 * <p>
 * 实现 {@link AuthPlugin} SPI，内部完成 JWKS 拉取、缓存与 RS256 验签。
 * </p>
 */
public class JwtAuthPlugin implements AuthPlugin {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PLUGIN_NAME = "jwt-auth";
    private static final String PLUGIN_VERSION = "1.0.0";

    private PluginContext pluginContext;
    private PluginJwksKeyProvider jwksKeyProvider;
    private PluginJwtValidator jwtValidator;
    private ScheduledExecutorService refreshScheduler;

    /**
     * {@inheritDoc}
     * <p>
     * 内部拉取 JWKS、启动定时刷新任务并记录插件启动日志。
     * </p>
     */
    @Override
    public void init(PluginContext context) {
        this.pluginContext = context;
        String jwksUri = context.getProperty("gateway.auth.jwt.jwks-uri");
        if (jwksUri == null || jwksUri.isBlank()) {
            throw new IllegalStateException("缺少配置 gateway.auth.jwt.jwks-uri");
        }

        this.jwksKeyProvider = new PluginJwksKeyProvider(jwksUri);
        this.jwksKeyProvider.initialize();
        this.jwtValidator = new PluginJwtValidator(context, jwksKeyProvider);
        startRefreshScheduler(context);

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
     * <p>
     * 停止 JWKS 定时刷新并释放资源。
     * </p>
     */
    @Override
    public void destroy() {
        if (refreshScheduler != null) {
            refreshScheduler.shutdownNow();
        }
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
     * 启动 JWKS 定时刷新任务。
     *
     * @param context 插件上下文。
     */
    private void startRefreshScheduler(PluginContext context) {
        long intervalSeconds = Long.parseLong(
                context.getProperty("gateway.auth.jwt.jwks-refresh-interval-seconds", "300"));
        refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "plugin-jwt-jwks-refresh");
            thread.setDaemon(true);
            return thread;
        });
        refreshScheduler.scheduleWithFixedDelay(() -> {
            try {
                jwksKeyProvider.refresh();
            } catch (Exception ex) {
                context.logWarn("JWKS 定时刷新失败: " + ex.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
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
