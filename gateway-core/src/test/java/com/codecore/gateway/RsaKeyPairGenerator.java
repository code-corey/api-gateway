package com.codecore.gateway;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * RSA 密钥对生成与 PEM 编码工具。
 * <p>
 * 供测试与本地开发使用：生成 PKCS#8 私钥与 X.509 公钥的 PEM 文本，
 * 格式与 {@link com.codecore.gateway.auth.PublicKeyLoader} 及 JWT 测试工具保持一致。
 * </p>
 */
public final class RsaKeyPairGenerator {

    /**
     * 默认 RSA 密钥长度（位）。
     */
    public static final int DEFAULT_KEY_SIZE = 2048;

    private RsaKeyPairGenerator() {
    }

    /**
     * 生成 RSA 密钥对（默认 2048 位）。
     * <p>
     * 内部调用 {@link KeyPairGenerator} 生成随机密钥材料。
     * </p>
     *
     * @return 包含 PEM 文本的密钥对结果。
     */
    public static RsaKeyPairPem generate() throws Exception {
        return generate(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成指定长度的 RSA 密钥对。
     *
     * @param keySizeBits 密钥长度（位），常用 2048 或 4096。
     * @return 包含 PEM 文本的密钥对结果。
     */
    public static RsaKeyPairPem generate(int keySizeBits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySizeBits);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        return new RsaKeyPairPem(toPrivateKeyPem(privateKey), toPublicKeyPem(publicKey));
    }

    /**
     * 将 RSA 私钥编码为 PKCS#8 PEM 字符串。
     *
     * @param privateKey RSA 私钥。
     * @return PEM 文本。
     */
    public static String toPrivateKeyPem(RSAPrivateKey privateKey) {
        return toPem("PRIVATE KEY", privateKey.getEncoded());
    }

    /**
     * 将 RSA 公钥编码为 X.509 PEM 字符串。
     *
     * @param publicKey RSA 公钥。
     * @return PEM 文本。
     */
    public static String toPublicKeyPem(RSAPublicKey publicKey) {
        return toPem("PUBLIC KEY", publicKey.getEncoded());
    }

    /**
     * 将 DER 字节编码为 PEM 块。
     *
     * @param type PEM 类型标签，如 PRIVATE KEY、PUBLIC KEY。
     * @param der  DER 编码密钥字节。
     * @return PEM 文本。
     */
    private static String toPem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    /**
     * RSA 密钥对 PEM 结果。
     *
     * @param privateKeyPem PKCS#8 私钥 PEM。
     * @param publicKeyPem  X.509 公钥 PEM。
     */
    public record RsaKeyPairPem(String privateKeyPem, String publicKeyPem) {
    }
}
