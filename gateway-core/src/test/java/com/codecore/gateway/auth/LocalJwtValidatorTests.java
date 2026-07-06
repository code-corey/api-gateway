package com.codecore.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LocalJwtValidator} 单元测试。
 */
@SpringBootTest
class LocalJwtValidatorTests {

    @Autowired
    private LocalJwtValidator jwtValidator;

    /**
     * 合法 JWT 应校验通过。
     */
    @Test
    void shouldValidateCorrectJwt() throws Exception {
        JwtValidationResult result = jwtValidator.validate(JwtTestTokenHelper.validToken());
        assertTrue(result.valid());
    }

    /**
     * 过期 JWT 应校验失败。
     */
    @Test
    void shouldRejectExpiredJwt() throws Exception {
        JwtValidationResult result = jwtValidator.validate(JwtTestTokenHelper.expiredToken());
        assertFalse(result.valid());
    }
}
