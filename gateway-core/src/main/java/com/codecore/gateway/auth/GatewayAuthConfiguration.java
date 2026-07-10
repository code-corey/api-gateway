package com.codecore.gateway.auth;

import com.codecore.gateway.plugin.GatewayPluginProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 网关认证相关配置。
 */
@Configuration
@EnableConfigurationProperties({GatewayAuthProperties.class, GatewayPluginProperties.class})
public class GatewayAuthConfiguration {
}
