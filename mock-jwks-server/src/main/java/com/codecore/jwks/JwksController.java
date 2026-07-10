package com.codecore.jwks;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
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
     * 标准 JWKS 端点（返回当前所有有效公钥）。
     *
     * @return JWKS JSON 文档。
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwksKeyStore.currentJwks();
    }

    /**
     * 查看当前 JWKS 中的 kid 列表及活跃签名钥。
     *
     * @return 密钥状态。
     */
    @GetMapping("/admin/keys")
    public Map<String, Object> listKeys() {
        List<String> kids = jwksKeyStore.listKeyIds();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("keys", kids);
        body.put("activeSigningKeyId", jwksKeyStore.getActiveSigningKeyId());
        body.put("message", "JWKS 为多钥并存；验签按 JWT Header 的 kid 匹配，而非「最新钥」");
        return body;
    }

    /**
     * 轮换密钥：追加新钥，旧钥保留（生产级重叠窗口模型）。
     *
     * @return 轮换结果。
     */
    @PostMapping("/admin/rotate-key")
    public Map<String, Object> rotateKey() throws Exception {
        String newKid = jwksKeyStore.rotateKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("newKid", newKid);
        body.put("activeSigningKeyId", jwksKeyStore.getActiveSigningKeyId());
        body.put("keys", jwksKeyStore.listKeyIds());
        body.put("message", "已追加新钥；旧钥仍保留，此前签发的 JWT 在 exp 前仍可验签");
        return body;
    }

    /**
     * 撤销指定 kid（模拟重叠窗口结束后移除旧公钥）。
     *
     * @param kid 密钥 ID。
     * @return 撤销结果。
     */
    @DeleteMapping("/admin/keys/{kid}")
    public Map<String, Object> revokeKey(@PathVariable("kid") String kid) {
        boolean revoked = jwksKeyStore.revokeKey(kid);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kid", kid);
        body.put("revoked", revoked);
        body.put("keys", jwksKeyStore.listKeyIds());
        body.put("message", revoked
                ? "已撤销；携带该 kid 的 JWT 将无法再验签（若未过期也会因找不到公钥而失败）"
                : "撤销失败：kid 不存在、为当前活跃签名钥、或 JWKS 至少需保留一把钥");
        return body;
    }

    /**
     * 使用当前活跃签名钥签发演示 JWT。
     *
     * @return 含 token 与 kid 的响应。
     */
    @PostMapping("/admin/issue-token")
    public Map<String, String> issueToken() throws Exception {
        String token = jwksKeyStore.issueDemoToken();
        return Map.of(
                "kid", jwksKeyStore.getActiveSigningKeyId(),
                "token", token,
                "authorization", "Bearer " + token
        );
    }
}
