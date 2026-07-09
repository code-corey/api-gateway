package com.codecore.gateway.auth;

import com.nimbusds.jose.jwk.JWKSet;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JWKS 公钥提供者。
 * <p>
 * Stage 4 从远端 Auth 服务拉取 JWKS 并缓存，按 JWT Header 中的 kid 匹配 RSA 公钥。
 * </p>
 */
@Component
public class JwksKeyProvider {

    private final GatewayAuthProperties authProperties;
    private final WebClient webClient;
    private final AtomicReference<JWKSet> cachedJwkSet = new AtomicReference<>();

    /**
     * @param authProperties 网关认证配置。
     * @param webClientBuilder WebClient 构建器。
     */
    public JwksKeyProvider(GatewayAuthProperties authProperties, WebClient.Builder webClientBuilder) {
        this.authProperties = authProperties;
        this.webClient = webClientBuilder.build();
    }

    /**
     * 启动时拉取 JWKS 并写入内存缓存。
     */
    @PostConstruct
    void initialize() {
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
     * <p>
     * 内部使用 WebClient 同步请求 jwksUri，解析为 {@link JWKSet} 后原子替换缓存。
     * </p>
     */
    public synchronized void refresh() {
        GatewayJwtProperties jwt = authProperties.getJwt();
        String jwksJson = webClient.get()
                .uri(jwt.getJwksUri())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> Mono.error(new IllegalStateException("JWKS 拉取失败: " + ex.getMessage(), ex)))
                .block();

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
