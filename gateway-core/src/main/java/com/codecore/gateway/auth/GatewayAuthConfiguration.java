package com.codecore.gateway.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 网关认证相关配置注册。
 * <p>
 * 启用 {@link GatewayAuthProperties} 的配置绑定，使 YAML 中的 gateway.auth 项生效。
 * </p>
 */
@Configuration
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayAuthConfiguration {
}
