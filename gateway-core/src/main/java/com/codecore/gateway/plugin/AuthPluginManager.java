package com.codecore.gateway.plugin;

import org.springframework.stereotype.Component;

/**
 * 认证插件管理器（Stage 6 静态加载版）。
 * <p>
 * 启动时通过 {@link StaticPluginLoader} 从 plugins 目录加载 JWT 插件 JAR；
 * Stage 7 起将扩展为动态扫描与热部署。
 * </p>
 */
@Component
public class AuthPluginManager {

    private final AuthPlugin currentPlugin;

    /**
     * 加载并初始化 JWT 认证插件。
     *
     * @param staticPluginLoader 静态插件加载器。
     * @param pluginContext      插件上下文。
     */
    public AuthPluginManager(StaticPluginLoader staticPluginLoader, SpringPluginContext pluginContext) {
        this.currentPlugin = staticPluginLoader.loadAndInit(pluginContext);
    }

    /**
     * 获取当前激活的认证插件。
     *
     * @return 当前插件实例。
     */
    public AuthPlugin getCurrentPlugin() {
        return currentPlugin;
    }

    /**
     * 返回当前插件元信息。
     *
     * @return 插件元数据。
     */
    public PluginMetadata getCurrentMetadata() {
        return currentPlugin.metadata();
    }
}
