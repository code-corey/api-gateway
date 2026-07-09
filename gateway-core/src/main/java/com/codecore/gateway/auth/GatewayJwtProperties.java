package com.codecore.gateway.auth;

/**
 * 网关 JWT 验签配置。
 * <p>
 * Stage 4 起从 JWKS 端点拉取公钥并按 kid 验签 RS256 JWT。
 * </p>
 */
public class GatewayJwtProperties {

    /**
     * JWT 验签默认密钥 ID，测试签发时需与 JWKS 中 kid 一致。
     */
    public static final String DEFAULT_KEY_ID = "dev-key-1";

    /**
     * 是否启用 JWT 验签。
     */
    private boolean enabled = true;

    /**
     * 期望的 JWT 签发者（iss）。
     */
    private String issuer = "https://auth.example.com";

    /**
     * 期望的受众（aud），须与 Token 中的 aud 一致。
     */
    private String audience = "api-gateway";

    /**
     * JWKS 端点 URL，例如 http://localhost:8082/.well-known/jwks.json。
     */
    private String jwksUri = "http://localhost:8082/.well-known/jwks.json";

    /**
     * JWKS 缓存刷新间隔（秒）。
     */
    private long jwksRefreshIntervalSeconds = 300;

    /**
     * 时钟偏移容差（秒），用于缓解多机时间差导致的 exp/nbf 误判。
     */
    private long clockSkewSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public long getJwksRefreshIntervalSeconds() {
        return jwksRefreshIntervalSeconds;
    }

    public void setJwksRefreshIntervalSeconds(long jwksRefreshIntervalSeconds) {
        this.jwksRefreshIntervalSeconds = jwksRefreshIntervalSeconds;
    }

    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
