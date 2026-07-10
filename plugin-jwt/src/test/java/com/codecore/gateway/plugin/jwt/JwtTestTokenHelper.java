package com.codecore.gateway.plugin.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * 插件模块测试用 JWT 签发工具。
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

    private static String buildToken(String issuer, String audience, Date expiresAt) throws Exception {
        RSAPrivateKey privateKey = parsePrivateKey(readClasspath("jwt/dev-private.pem"));
        JWSSigner signer = new RSASSASigner(privateKey);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("user-001")
                .issueTime(new Date())
                .expirationTime(expiresAt)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(JwksWireMockSupport.DEFAULT_KEY_ID)
                .build();
        SignedJWT signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }

    private static String readClasspath(String path) throws IOException {
        try (InputStream inputStream = JwtTestTokenHelper.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("classpath 资源不存在: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
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
