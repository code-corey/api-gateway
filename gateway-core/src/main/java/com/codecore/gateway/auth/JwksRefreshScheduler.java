package com.codecore.gateway.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JWKS 定时刷新任务。
 * <p>
 * 按配置间隔从远端重新拉取公钥，支持 Auth 服务密钥轮换。
 * </p>
 */
@Component
public class JwksRefreshScheduler {

    private final GatewayAuthProperties authProperties;
    private final JwksKeyProvider jwksKeyProvider;

    /**
     * @param authProperties  网关认证配置。
     * @param jwksKeyProvider   JWKS 公钥提供者。
     */
    public JwksRefreshScheduler(GatewayAuthProperties authProperties, JwksKeyProvider jwksKeyProvider) {
        this.authProperties = authProperties;
        this.jwksKeyProvider = jwksKeyProvider;
    }

    /**
     * 定时刷新 JWKS 缓存。
     * <p>
     * 内部读取 jwksRefreshIntervalSeconds 作为 fixedDelay 间隔（毫秒）。
     * </p>
     */
    @Scheduled(fixedDelayString = "#{${gateway.auth.jwt.jwks-refresh-interval-seconds:300} * 1000}")
    public void refreshJwks() {
        if (!authProperties.isEnabled() || !authProperties.getJwt().isEnabled()) {
            return;
        }
        jwksKeyProvider.refresh();
    }
}
