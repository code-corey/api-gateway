package com.codecore.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 硬编码认证过滤器 Web 层集成测试。
 * <p>
 * 验证无 Token、错误 Token、白名单路径等场景下的 HTTP 状态码行为。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayAuthWebTests {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * 访问受保护路径且未携带 Token 时应返回 401。
     * <p>
     * 内部向 /api/hello 发起 GET 请求，不设置 Authorization 头，期望网关拦截。
     * </p>
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
     * 携带错误 Token 访问受保护路径时应返回 401。
     */
    @Test
    void shouldReturn401WhenTokenInvalid() {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Actuator 健康检查在白名单中，无需 Token 也应返回 200。
     */
    @Test
    void shouldAllowActuatorWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    /**
     * 携带正确 Token 时不应被认证过滤器以 401 拒绝。
     * <p>
     * 下游 mock-backend 未启动时可能返回 502，但状态码一定不是 401。
     * </p>
     */
    @Test
    void shouldPassAuthWithValidToken() {
        webTestClient.get()
                .uri("/api/hello")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token-123")
                .exchange()
                .expectStatus().value(status -> {
                    if (status == 401) {
                        throw new AssertionError("有效 Token 不应返回 401，实际: " + status);
                    }
                });
    }
}
