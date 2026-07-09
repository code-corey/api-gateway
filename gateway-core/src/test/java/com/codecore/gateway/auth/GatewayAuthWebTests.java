package com.codecore.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * JWT 认证过滤器 Web 层集成测试（JWKS 模式）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayAuthWebTests {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 测试环境 JWKS 指向 WireMock。
     *
     * @param registry 动态属性注册器。
     */
    @DynamicPropertySource
    static void configureJwksUri(DynamicPropertyRegistry registry) {
        registry.add("gateway.auth.jwt.jwks-uri", JwksWireMockSupport::jwksUri);
    }

    @Test
    void shouldReturn401WhenTokenMissing() {
        webTestClient.get()
                .uri("/api/hello")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenTokenInvalid() {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenTokenExpired() throws Exception {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokenHelper.expiredToken())
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("JWT 已过期");
    }

    @Test
    void shouldReturn401WhenIssuerInvalid() throws Exception {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokenHelper.wrongIssuerToken())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowActuatorWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldPassAuthWithValidJwt() throws Exception {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokenHelper.validToken())
                .exchange()
                .expectStatus().value(status -> {
                    if (status == 401) {
                        throw new AssertionError("合法 JWT 不应返回 401，实际: " + status);
                    }
                });
    }
}
