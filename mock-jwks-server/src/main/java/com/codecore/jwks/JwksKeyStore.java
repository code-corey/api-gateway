package com.codecore.jwks;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWKS 密钥库（生产级轮换模型）。
 * <p>
 * 维护 kid → RSAKey 映射，支持多钥并存：轮换时追加新钥而非替换旧钥，
 * 使已签发且未过期的 JWT 在重叠窗口内仍可验签。
 * </p>
 */
@Service
public class JwksKeyStore {

    /**
     * 初始密钥 ID，需与开发环境测试 JWT 的 kid 一致。
     */
    public static final String INITIAL_KEY_ID = "dev-key-1";

    private static final String DEFAULT_ISSUER = "https://auth.example.com";
    private static final String DEFAULT_AUDIENCE = "api-gateway";

    private final Map<String, RSAKey> keys = new ConcurrentHashMap<>();
    private volatile String activeSigningKeyId = INITIAL_KEY_ID;

    /**
     * 加载初始开发密钥对（dev-key-1）。
     */
    @PostConstruct
    void init() throws Exception {
        RSAPublicKey publicKey = parsePublicKey(readClasspath("jwt/dev-public.pem"));
        RSAPrivateKey privateKey = parsePrivateKey(readClasspath("jwt/dev-private.pem"));
        keys.put(INITIAL_KEY_ID, new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(INITIAL_KEY_ID)
                .build());
        activeSigningKeyId = INITIAL_KEY_ID;
    }

    /**
     * 返回当前 JWKS（包含所有未撤销的公钥）。
     *
     * @return JWKS JSON 文档。
     */
    public Map<String, Object> currentJwks() {
        return new JWKSet(new ArrayList<>(keys.values())).toJSONObject();
    }

    /**
     * 列出当前 JWKS 中所有 kid。
     *
     * @return kid 列表。
     */
    public List<String> listKeyIds() {
        return new ArrayList<>(keys.keySet());
    }

    /**
     * 获取当前用于签发新 Token 的 kid。
     *
     * @return 活跃签名密钥 ID。
     */
    public String getActiveSigningKeyId() {
        return activeSigningKeyId;
    }

    /**
     * 轮换密钥：生成新 RSA 密钥对并【追加】到 JWKS，旧钥保留。
     * <p>
     * 新钥成为后续 {@link #issueDemoToken()} 的默认签名钥。
     * </p>
     *
     * @return 新 kid。
     */
    public synchronized String rotateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String newKid = "rotated-key-" + System.currentTimeMillis();
        keys.put(newKid, new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(newKid)
                .build());
        activeSigningKeyId = newKid;
        return newKid;
    }

    /**
     * 撤销指定 kid（模拟重叠窗口结束后的旧钥清理）。
     * <p>
     * 至少保留一把密钥，且不能撤销当前活跃签名钥。
     * </p>
     *
     * @param keyId 待撤销的 kid。
     * @return 是否撤销成功。
     */
    public synchronized boolean revokeKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            return false;
        }
        if (keys.size() <= 1) {
            return false;
        }
        if (keyId.equals(activeSigningKeyId)) {
            return false;
        }
        return keys.remove(keyId) != null;
    }

    /**
     * 使用当前活跃签名钥签发演示用 JWT（1 小时有效）。
     *
     * @return 序列化 JWT 字符串。
     */
    public String issueDemoToken() throws Exception {
        RSAKey signingKey = keys.get(activeSigningKeyId);
        if (signingKey == null || signingKey.toRSAPrivateKey() == null) {
            throw new IllegalStateException("当前签名钥不可用: " + activeSigningKeyId);
        }

        JWSSigner signer = new RSASSASigner(signingKey.toRSAPrivateKey());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(DEFAULT_ISSUER)
                .audience(DEFAULT_AUDIENCE)
                .subject("demo-user")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(activeSigningKeyId).build(),
                claims
        );
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }

    private static String readClasspath(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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

    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
