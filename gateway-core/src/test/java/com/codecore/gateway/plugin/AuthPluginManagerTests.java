package com.codecore.gateway.plugin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AuthPluginManager} 与 JWT 插件集成测试。
 */
@SpringBootTest
class AuthPluginManagerTests {

    @Autowired
    private AuthPluginManager authPluginManager;

    @DynamicPropertySource
    static void configureJwksUri(DynamicPropertyRegistry registry) {
        registry.add("gateway.auth.jwt.jwks-uri",
                com.codecore.gateway.auth.JwksWireMockSupport::jwksUri);
    }

    @Test
    void shouldExposeJwtPluginMetadata() {
        PluginMetadata metadata = authPluginManager.getCurrentMetadata();
        assertEquals("jwt-auth", metadata.name());
        assertEquals("1.0.0", metadata.version());
    }

    @Test
    void shouldDiscoverJwtPluginInPluginsDirectory() {
        assertTrue(authPluginManager.listDiscoveredMetadata().stream()
                .anyMatch(meta -> "jwt-auth".equals(meta.name())));
    }

    @Test
    void shouldAuthenticateViaPlugin() throws Exception {
        String token = com.codecore.gateway.auth.JwtTestTokenHelper.validToken();
        AuthRequest request = AuthRequest.of("/api/hello", "GET");
        request = new AuthRequest(
                request.path(),
                request.method(),
                request.clientIp(),
                request.traceId(),
                java.util.Map.of("Authorization", "Bearer " + token)
        );

        AuthResult result = authPluginManager.getCurrentPlugin().authenticate(request);
        org.junit.jupiter.api.Assertions.assertTrue(result.isAuthenticated());
    }
}
