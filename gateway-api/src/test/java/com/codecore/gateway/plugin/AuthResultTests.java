package com.codecore.gateway.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AuthResult} 单元测试。
 */
class AuthResultTests {

    @Test
    void shouldIdentifyAuthenticatedResult() {
        AuthResult result = AuthResult.authenticated();
        assertTrue(result.isAuthenticated());
    }

    @Test
    void shouldIdentifyUnauthenticatedResult() {
        AuthResult result = AuthResult.unauthenticated("未认证");
        assertFalse(result.isAuthenticated());
    }
}
