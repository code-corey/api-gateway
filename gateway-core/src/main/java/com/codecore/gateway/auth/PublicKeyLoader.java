package com.codecore.gateway.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT 验签公钥加载器。
 * <p>
 * 应用启动时从配置的 PEM 文件读取 RSA 公钥，供 {@link LocalJwtValidator} 复用。
 * </p>
 */
@Component
public class PublicKeyLoader {

    private final GatewayAuthProperties authProperties;
    private final ResourceLoader resourceLoader;
    private RSAPublicKey publicKey;

    /**
     * @param authProperties 网关认证配置。
     * @param resourceLoader Spring 资源加载器。
     */
    public PublicKeyLoader(GatewayAuthProperties authProperties, ResourceLoader resourceLoader) {
        this.authProperties = authProperties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 启动时加载 PEM 公钥并解析为 {@link RSAPublicKey}。
     * <p>
     * 内部读取 publicKeyLocation 指向的文件，剥离 PEM 头尾后按 X.509 解码。
     * </p>
     */
    @PostConstruct
    void loadPublicKey() throws Exception {
        String location = authProperties.getJwt().getPublicKeyLocation();
        Resource resource = resourceLoader.getResource(location);
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        this.publicKey = parsePublicKey(pem);
    }

    /**
     * 将 PEM 格式公钥字符串解析为 {@link RSAPublicKey}。
     *
     * @param pem PEM 文本。
     * @return RSA 公钥。
     */
    static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(der));
    }

    /**
     * 获取已加载的 RSA 公钥。
     *
     * @return RSA 公钥实例。
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }
}
