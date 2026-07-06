package com.codecore.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * JWT 认证过滤器 Web 层集成测试。
 * <p>
 * 验证 JWT 缺失、伪造、过期、签发者错误等场景下的 HTTP 状态码行为。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayAuthWebTests {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 未携带 Token 时应返回 401。
     */
    @Test
    void shouldReturn401WhenTokenMissing() {
        webTestClient.get()
                .uri("/api/hello")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401);
    }

    /**
     * 非 JWT 字符串应返回 401。
     */
    @Test
    void shouldReturn401WhenTokenInvalid() {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * 过期 JWT 应返回 401。
     */
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

    /**
     * 签发者错误的 JWT 应返回 401。
     */
    @Test
    void shouldReturn401WhenIssuerInvalid() throws Exception {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokenHelper.wrongIssuerToken())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Actuator 健康检查在白名单中，无需 Token。
     */
    @Test
    void shouldAllowActuatorWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    /**
     * 合法 JWT 不应被认证过滤器以 401 拒绝。
     */
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
