package com.codecore.gateway.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * 测试用 JWKS WireMock 支持。
 * <p>
 * JVM 内单例启动内存 JWKS 端点，供各 Spring 测试类通过 {@link #jwksUri()} 引用。
 * </p>
 */
public final class JwksWireMockSupport {

    private static final WireMockServer SERVER;

    static {
        try {
            SERVER = startWithDevPublicKey();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private JwksWireMockSupport() {
    }

    /**
     * @return WireMock JWKS 完整 URI。
     */
    public static String jwksUri() {
        return SERVER.baseUrl() + "/.well-known/jwks.json";
    }

    /**
     * 启动 WireMock 并 stub JWKS 端点。
     *
     * @return 已启动的 WireMock 实例。
     */
    private static WireMockServer startWithDevPublicKey() throws Exception {
        WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());

        server.stubFor(get(urlEqualTo("/.well-known/jwks.json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildDevJwksJson())));

        return server;
    }

    /**
     * 构建与 dev-public.pem 对应的 JWKS JSON。
     *
     * @return JWKS JSON 字符串。
     */
    public static String buildDevJwksJson() throws Exception {
        ClassPathResource resource = new ClassPathResource("jwt/dev-public.pem");
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        RSAPublicKey publicKey = parsePublicKey(pem);
        RSAKey rsaKey = new RSAKey.Builder(publicKey).keyID(GatewayJwtProperties.DEFAULT_KEY_ID).build();
        return new JWKSet(rsaKey).toString();
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
}
