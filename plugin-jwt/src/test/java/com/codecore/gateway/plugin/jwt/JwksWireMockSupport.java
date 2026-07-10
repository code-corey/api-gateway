package com.codecore.gateway.plugin.jwt;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.io.InputStream;
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
 * 插件模块测试用 JWKS WireMock 支持。
 */
public final class JwksWireMockSupport {

    /**
     * 测试 JWT 默认 kid，与 dev-public.pem 对应。
     */
    public static final String DEFAULT_KEY_ID = "dev-key-1";

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

    private static String buildDevJwksJson() throws Exception {
        RSAPublicKey publicKey = parsePublicKey(readClasspath("jwt/dev-public.pem"));
        RSAKey rsaKey = new RSAKey.Builder(publicKey).keyID(DEFAULT_KEY_ID).build();
        return new JWKSet(rsaKey).toString();
    }

    private static String readClasspath(String path) throws IOException {
        try (InputStream inputStream = JwksWireMockSupport.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("classpath 资源不存在: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
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
