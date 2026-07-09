package com.codecore.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LocalJwtValidator} 单元测试（JWKS 模式）。
 */
@SpringBootTest
class LocalJwtValidatorTests {

    @Autowired
    private LocalJwtValidator jwtValidator;

    @DynamicPropertySource
    static void configureJwksUri(DynamicPropertyRegistry registry) {
        registry.add("gateway.auth.jwt.jwks-uri", JwksWireMockSupport::jwksUri);
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
}
