package com.codecore.gateway.plugin;

import java.nio.file.Path;

/**
 * 已发现的插件实例及其加载上下文。
 * <p>
 * 内部绑定插件实例、所属 ClassLoader 与 JAR 路径，供 {@link AuthPluginManager} 管理生命周期。
 * </p>
 *
 * @param plugin      插件实例（可能尚未 init）。
 * @param classLoader 加载该插件的 ClassLoader。
 * @param jarPath     插件 JAR 路径。
 */
public record LoadedPlugin(AuthPlugin plugin, PluginClassLoader classLoader, Path jarPath) {

    /**
     * 返回插件元信息。
     *
     * @return 插件元数据。
     */
    public PluginMetadata metadata() {
        return plugin.metadata();
    }
}
