package com.codecore.gateway.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 网关认证与 JWKS 相关配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayAuthConfiguration {

    /**
     * 提供 WebClient.Builder 供 JWKS 拉取使用。
     *
     * @return WebClient 构建器。
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
