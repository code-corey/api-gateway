package com.codecore.jwks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWKS 多钥并存轮换模型测试。
 */
@SpringBootTest
class JwksKeyStoreRotationTests {

    @Autowired
    private JwksKeyStore jwksKeyStore;

    /**
     * 轮换后应追加新钥，旧钥 dev-key-1 仍保留在 JWKS 中。
     */
    @Test
    void shouldKeepOldKeyAfterRotation() throws Exception {
        int before = jwksKeyStore.listKeyIds().size();
        assertTrue(jwksKeyStore.listKeyIds().contains(JwksKeyStore.INITIAL_KEY_ID));

        String newKid = jwksKeyStore.rotateKey();

        assertEquals(before + 1, jwksKeyStore.listKeyIds().size());
        assertTrue(jwksKeyStore.listKeyIds().contains(JwksKeyStore.INITIAL_KEY_ID));
        assertTrue(jwksKeyStore.listKeyIds().contains(newKid));
        assertEquals(newKid, jwksKeyStore.getActiveSigningKeyId());
    }

    /**
     * 撤销旧钥后，JWKS 中不应再包含该 kid。
     */
    @Test
    void shouldRevokeOldKeyAfterGracePeriod() throws Exception {
        jwksKeyStore.rotateKey();
        boolean revoked = jwksKeyStore.revokeKey(JwksKeyStore.INITIAL_KEY_ID);
        assertTrue(revoked);
        assertTrue(!jwksKeyStore.listKeyIds().contains(JwksKeyStore.INITIAL_KEY_ID));
    }
}
