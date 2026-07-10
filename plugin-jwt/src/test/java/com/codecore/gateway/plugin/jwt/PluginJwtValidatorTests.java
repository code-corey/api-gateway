package com.codecore.gateway.plugin.jwt;

import com.codecore.gateway.plugin.PluginContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PluginJwtValidator} 单元测试。
 */
class PluginJwtValidatorTests {

    private PluginJwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        PluginContext context = new TestPluginContext(JwksWireMockSupport.jwksUri());
        PluginJwksKeyProvider keyProvider = new PluginJwksKeyProvider(JwksWireMockSupport.jwksUri());
        keyProvider.initialize();
        jwtValidator = new PluginJwtValidator(context, keyProvider);
    }

    @Test
    void shouldValidateCorrectJwt() throws Exception {
        JwtValidationResult result = jwtValidator.validate(JwtTestTokenHelper.validToken());
        assertTrue(result.valid());
    }

    @Test
    void shouldRejectExpiredJwt() throws Exception {
        JwtValidationResult result = jwtValidator.validate(JwtTestTokenHelper.expiredToken());
        assertFalse(result.valid());
    }

    /**
     * 测试用插件上下文。
     */
    private static final class TestPluginContext implements PluginContext {

        private final String jwksUri;

        private TestPluginContext(String jwksUri) {
            this.jwksUri = jwksUri;
        }

        @Override
        public String getProperty(String key) {
            return switch (key) {
                case "gateway.auth.jwt.jwks-uri" -> jwksUri;
                case "gateway.auth.jwt.issuer" -> "https://auth.example.com";
                case "gateway.auth.jwt.audience" -> "api-gateway";
                case "gateway.auth.jwt.clock-skew-seconds" -> "30";
                default -> null;
            };
        }

        @Override
        public void logInfo(String message) {
            // 测试忽略
        }

        @Override
        public void logWarn(String message) {
            // 测试忽略
        }
    }
}
