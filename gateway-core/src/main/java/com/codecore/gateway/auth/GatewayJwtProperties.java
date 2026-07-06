package com.codecore.gateway.auth;

/**
 * 网关 JWT 验签配置。
 * <p>
 * Stage 3 使用本地 PEM 公钥文件校验 RS256 JWT，绑定 gateway.auth.jwt 前缀。
 * </p>
 */
public class GatewayJwtProperties {

    /**
     * 是否启用 JWT 验签（Stage 3 起默认启用）。
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
     * RSA 公钥 PEM 文件位置，支持 classpath: 与 file:。
     */
    private String publicKeyLocation = "classpath:jwt/dev-public.pem";

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

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
