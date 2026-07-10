package com.codecore.gateway.plugin.jwt;

import com.nimbusds.jose.jwk.JWKSet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 插件内 JWKS 公钥提供者。
 * <p>
 * 使用 JDK {@link HttpClient} 拉取 JWKS，不依赖 Spring WebClient。
 * </p>
 */
public class PluginJwksKeyProvider {

    private final String jwksUri;
    private final HttpClient httpClient;
    private final AtomicReference<JWKSet> cachedJwkSet = new AtomicReference<>();

    /**
     * @param jwksUri JWKS 端点 URL。
     */
    public PluginJwksKeyProvider(String jwksUri) {
        this.jwksUri = jwksUri;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * 启动时拉取 JWKS 并写入内存缓存。
     */
    public void initialize() {
        refresh();
    }

    /**
     * 按 kid 获取 RSA 公钥；缓存未命中时会尝试刷新 JWKS 后重试一次。
     *
     * @param keyId JWT Header 中的 kid。
     * @return RSA 公钥。
     */
    public RSAPublicKey getPublicKey(String keyId) throws Exception {
        RSAPublicKey publicKey = resolvePublicKey(keyId, cachedJwkSet.get());
        if (publicKey != null) {
            return publicKey;
        }

        refresh();
        publicKey = resolvePublicKey(keyId, cachedJwkSet.get());
        if (publicKey == null) {
            throw new IllegalStateException("JWKS 中未找到 kid=" + keyId);
        }
        return publicKey;
    }

    /**
     * 从 JWKS 端点拉取最新公钥集合并更新缓存。
     */
    public synchronized void refresh() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUri))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        String jwksJson;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("JWKS 拉取失败: HTTP " + response.statusCode());
            }
            jwksJson = response.body();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("JWKS 拉取失败: " + ex.getMessage(), ex);
        }

        if (jwksJson == null || jwksJson.isBlank()) {
            throw new IllegalStateException("JWKS 响应为空");
        }

        try {
            cachedJwkSet.set(JWKSet.parse(jwksJson));
        } catch (Exception ex) {
            throw new IllegalStateException("JWKS 解析失败", ex);
        }
    }

    /**
     * 从 JWK 集合中解析指定 kid 的 RSA 公钥。
     *
     * @param keyId  密钥 ID。
     * @param jwkSet JWK 集合，可为 null。
     * @return 公钥；未找到时返回 null。
     */
    private RSAPublicKey resolvePublicKey(String keyId, JWKSet jwkSet) throws Exception {
        if (jwkSet == null || keyId == null || keyId.isBlank()) {
            return null;
        }
        if (jwkSet.getKeyByKeyId(keyId) == null) {
            return null;
        }
        return jwkSet.getKeyByKeyId(keyId).toRSAKey().toRSAPublicKey();
    }
}
