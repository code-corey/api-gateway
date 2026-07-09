package com.codecore.jwks;

import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.nimbusds.jose.jwk.JWKSet;

/**
 * JWKS 密钥库。
 * <p>
 * 启动时从 classpath 加载开发公钥；支持轮换生成新密钥对并更新 kid。
 * </p>
 */
@Service
public class JwksKeyStore {

    /**
     * 初始密钥 ID，需与测试 JWT 签发时的 kid 一致。
     */
    public static final String INITIAL_KEY_ID = "dev-key-1";

    private final AtomicReference<RSAKey> activeKey = new AtomicReference<>();

    /**
     * 初始化密钥库，加载默认开发公钥。
     */
    @PostConstruct
    void init() throws Exception {
        ClassPathResource resource = new ClassPathResource("jwt/dev-public.pem");
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        RSAPublicKey publicKey = parsePublicKey(pem);
        activeKey.set(new RSAKey.Builder(publicKey).keyID(INITIAL_KEY_ID).build());
    }

    /**
     * 返回当前 JWKS JSON 结构。
     *
     * @return JWKS 文档。
     */
    public Map<String, Object> currentJwks() {
        return new JWKSet(activeKey.get()).toJSONObject();
    }

    /**
     * 轮换密钥：生成新 RSA 密钥对并更新 active kid。
     *
     * @return 新的 kid。
     */
    public String rotateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String newKid = "rotated-key-" + System.currentTimeMillis();
        activeKey.set(new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(newKid)
                .build());
        return newKid;
    }

    /**
     * 获取当前 active kid。
     *
     * @return kid 字符串。
     */
    public String currentKeyId() {
        return activeKey.get().getKeyID();
    }

    private static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }
}
