package com.codecore.gateway.plugin.jwt;

import com.codecore.gateway.plugin.PluginContext;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 插件内 JWT 校验器。
 * <p>
 * 通过 JWKS 按 kid 获取公钥，验证 RS256 签名并校验 iss、aud、exp。
 * </p>
 */
public class PluginJwtValidator {

    private final PluginContext pluginContext;
    private final PluginJwksKeyProvider jwksKeyProvider;

    /**
     * @param pluginContext   插件上下文，用于读取 JWT 配置。
     * @param jwksKeyProvider JWKS 公钥提供者。
     */
    public PluginJwtValidator(PluginContext pluginContext, PluginJwksKeyProvider jwksKeyProvider) {
        this.pluginContext = pluginContext;
        this.jwksKeyProvider = jwksKeyProvider;
    }

    /**
     * 校验 JWT 字符串是否合法。
     *
     * @param rawJwt 不含 Bearer 前缀的 JWT 字符串。
     * @return 校验结果。
     */
    public JwtValidationResult validate(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            return JwtValidationResult.failure("缺少 JWT");
        }

        try {
            SignedJWT signedJwt = SignedJWT.parse(rawJwt);
            String keyId = signedJwt.getHeader().getKeyID();
            if (keyId == null || keyId.isBlank()) {
                return JwtValidationResult.failure("JWT 缺少 kid");
            }

            RSAPublicKey publicKey = jwksKeyProvider.getPublicKey(keyId);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJwt.verify(verifier)) {
                return JwtValidationResult.failure("JWT 签名无效");
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            return validateClaims(claims);
        } catch (ParseException ex) {
            return JwtValidationResult.failure("JWT 格式错误");
        } catch (IllegalStateException ex) {
            return JwtValidationResult.failure(ex.getMessage());
        } catch (Exception ex) {
            return JwtValidationResult.failure("JWT 校验失败");
        }
    }

    /**
     * 校验标准 Claims：签发者、受众、过期时间。
     *
     * @param claims JWT 载荷。
     * @return 校验结果。
     */
    private JwtValidationResult validateClaims(JWTClaimsSet claims) {
        String expectedIssuer = pluginContext.getProperty("gateway.auth.jwt.issuer");
        String expectedAudience = pluginContext.getProperty("gateway.auth.jwt.audience");
        long clockSkewSeconds = Long.parseLong(
                pluginContext.getProperty("gateway.auth.jwt.clock-skew-seconds", "30"));

        String issuer = claims.getIssuer();
        if (issuer == null || !issuer.equals(expectedIssuer)) {
            return JwtValidationResult.failure("JWT 签发者无效");
        }

        List<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains(expectedAudience)) {
            return JwtValidationResult.failure("JWT 受众无效");
        }

        Date expiration = claims.getExpirationTime();
        if (expiration == null) {
            return JwtValidationResult.failure("JWT 缺少过期时间");
        }

        Instant expireAt = expiration.toInstant().plusSeconds(clockSkewSeconds);
        if (expireAt.isBefore(Instant.now())) {
            return JwtValidationResult.failure("JWT 已过期");
        }

        return JwtValidationResult.success();
    }
}
