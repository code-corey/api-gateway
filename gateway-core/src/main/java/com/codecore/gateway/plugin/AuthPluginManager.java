package com.codecore.gateway.plugin;

import org.springframework.stereotype.Component;

/**
 * 认证插件管理器（Stage 5 简化版）。
 * <p>
 * 持有当前激活的 {@link AuthPlugin} 实例；Stage 7 起将扩展为动态加载与热部署。
 * </p>
 */
@Component
public class AuthPluginManager {

    private final AuthPlugin currentPlugin;

    /**
     * 注入并初始化 JWT 认证插件。
     * <p>
     * 内部调用插件 {@link AuthPlugin#init(PluginContext)} 完成启动初始化。
     * </p>
     *
     * @param jwtAuthPlugin JWT 认证插件实现。
     * @param pluginContext 插件上下文。
     */
    public AuthPluginManager(JwtAuthPlugin jwtAuthPlugin, SpringPluginContext pluginContext) {
        jwtAuthPlugin.init(pluginContext);
        this.currentPlugin = jwtAuthPlugin;
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
