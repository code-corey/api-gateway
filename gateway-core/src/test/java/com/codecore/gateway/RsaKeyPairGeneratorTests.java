package com.codecore.gateway;

import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RsaKeyPairGenerator} 单元测试。
 */
class RsaKeyPairGeneratorTests {

    /**
     * 生成的 PEM 应能被标准 RSA 解析逻辑正确识别。
     */
    @Test
    void shouldGenerateParseableKeyPair() throws Exception {
        RsaKeyPairGenerator.RsaKeyPairPem keyPair = RsaKeyPairGenerator.generate();

        assertNotNull(parsePublicKey(keyPair.publicKeyPem()));
        RSAPrivateKey privateKey = parsePrivateKey(keyPair.privateKeyPem());
        assertTrue(privateKey.getModulus().bitLength() >= RsaKeyPairGenerator.DEFAULT_KEY_SIZE);
    }

    private static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
