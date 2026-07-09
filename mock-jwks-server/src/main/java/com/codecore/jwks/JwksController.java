package com.codecore.jwks;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWKS 与管理接口。
 */
@RestController
public class JwksController {

    private final JwksKeyStore jwksKeyStore;

    /**
     * @param jwksKeyStore JWKS 密钥库。
     */
    public JwksController(JwksKeyStore jwksKeyStore) {
        this.jwksKeyStore = jwksKeyStore;
    }

    /**
     * 标准 JWKS 端点。
     *
     * @return JWKS JSON 文档。
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwksKeyStore.currentJwks();
    }

    /**
     * 轮换密钥（教学演示用）。
     *
     * @return 包含新 kid 的响应。
     */
    @PostMapping("/admin/rotate-key")
    public Map<String, String> rotateKey() throws Exception {
        String newKid = jwksKeyStore.rotateKey();
        return Map.of("kid", newKid, "message", "JWKS 密钥已轮换");
    }
}
