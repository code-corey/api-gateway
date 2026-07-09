package com.codecore.gateway;

import com.codecore.gateway.auth.JwksWireMockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 网关应用上下文加载测试。
 */
@SpringBootTest
class GatewayApplicationTests {

    @DynamicPropertySource
    static void configureJwksUri(DynamicPropertyRegistry registry) {
        registry.add("gateway.auth.jwt.jwks-uri", JwksWireMockSupport::jwksUri);
    }

    /**
     * 验证应用上下文可以成功加载。
     */
    @Test
    void contextLoads() {
    }
}
