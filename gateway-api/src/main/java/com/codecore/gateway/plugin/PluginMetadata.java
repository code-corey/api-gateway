package com.codecore.gateway.plugin;

/**
 * 插件元信息。
 *
 * @param name              插件名称，如 jwt-auth。
 * @param version           语义化版本，如 1.0.0。
 * @param author            作者或组织。
 * @param minGatewayVersion 要求的最低网关版本。
 */
public record PluginMetadata(
        String name,
        String version,
        String author,
        String minGatewayVersion
) {
}
