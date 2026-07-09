package com.codecore.gateway.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * 测试用 JWT 签发工具。
 * <p>
 * 使用 test/resources 下的 dev-private.pem 生成 RS256 JWT，Header 含 kid 供 JWKS 匹配。
 * </p>
 */
public final class JwtTestTokenHelper {

    private static final String DEFAULT_ISSUER = "https://auth.example.com";
    private static final String DEFAULT_AUDIENCE = "api-gateway";

    private JwtTestTokenHelper() {
    }

    /**
     * 生成默认有效 JWT（1 小时过期）。
     *
     * @return 序列化后的 JWT 字符串。
     */
    public static String validToken() throws Exception {
        return buildToken(DEFAULT_ISSUER, DEFAULT_AUDIENCE, Date.from(Instant.now().plusSeconds(3600)));
    }

    /**
     * 生成已过期 JWT。
     *
     * @return 序列化后的 JWT 字符串。
     */
    public static String expiredToken() throws Exception {
        return buildToken(DEFAULT_ISSUER, DEFAULT_AUDIENCE, Date.from(Instant.now().minusSeconds(3600)));
    }

    /**
     * 生成签发者不匹配的 JWT。
     *
     * @return 序列化后的 JWT 字符串。
     */
    public static String wrongIssuerToken() throws Exception {
        return buildToken("https://evil.example.com", DEFAULT_AUDIENCE, Date.from(Instant.now().plusSeconds(3600)));
    }

    /**
     * 使用测试私钥签发 JWT。
     */
    private static String buildToken(String issuer, String audience, Date expiresAt) throws Exception {
        ClassPathResource resource = new ClassPathResource("jwt/dev-private.pem");
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        RSAPrivateKey privateKey = parsePrivateKey(pem);
        JWSSigner signer = new RSASSASigner(privateKey);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("user-001")
                .issueTime(new Date())
                .expirationTime(expiresAt)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(GatewayJwtProperties.DEFAULT_KEY_ID)
                .build();
        SignedJWT signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }

    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
