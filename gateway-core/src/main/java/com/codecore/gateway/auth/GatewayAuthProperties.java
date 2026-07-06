package com.codecore.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关认证配置。
 * <p>
 * 绑定 application.yml 中 gateway.auth 前缀；Stage 3 起通过 jwt 子配置进行本地公钥验签。
 * </p>
 */
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /**
     * 是否启用认证过滤器。
     */
    private boolean enabled = true;

    /**
     * 跳过认证的路径模式（Ant 风格），例如 /actuator/**。
     */
    private List<String> excludePaths = new ArrayList<>(List.of("/actuator/**"));

    /**
     * JWT 验签相关配置。
     */
    private GatewayJwtProperties jwt = new GatewayJwtProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public GatewayJwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(GatewayJwtProperties jwt) {
        this.jwt = jwt;
    }
}
