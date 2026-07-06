package com.codecore.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关硬编码认证配置。
 * <p>
 * 绑定 application.yml 中 gateway.auth 前缀的配置项，供 Stage 2 认证过滤器读取。
 * </p>
 */
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /**
     * 是否启用认证过滤器。
     */
    private boolean enabled = true;

    /**
     * 允许的 Bearer Token 固定值（Stage 2 教学用，生产环境应替换为 JWT 等机制）。
     */
    private String token = "test-token-123";

    /**
     * 跳过认证的路径模式（Ant 风格），例如 /actuator/**。
     */
    private List<String> excludePaths = new ArrayList<>(List.of("/actuator/**"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
