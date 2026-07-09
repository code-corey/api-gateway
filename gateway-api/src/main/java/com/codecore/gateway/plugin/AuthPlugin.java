package com.codecore.gateway.plugin;

/**
 * 认证插件 SPI 契约。
 * <p>
 * Stage 5 定义网关与认证插件之间的标准接口；后续 Stage 将把 JWT 实现打包为独立 JAR 插件。
 * </p>
 */
public interface AuthPlugin {

    /**
     * 插件初始化。
     * <p>
     * 网关加载插件后调用一次，用于读取配置、预热资源。
     * </p>
     *
     * @param context 插件运行时上下文。
     */
    void init(PluginContext context);

    /**
     * 执行认证逻辑。
     * <p>
     * 网关在每个受保护请求进入路由前调用；内部应无长时间阻塞。
     * </p>
     *
     * @param request 认证请求上下文。
     * @return 认证结果。
     */
    AuthResult authenticate(AuthRequest request);

    /**
     * 插件销毁。
     * <p>
     * 插件卸载或网关关闭前调用，用于释放连接池、定时任务等资源。
     * </p>
     */
    void destroy();

    /**
     * 返回插件元信息。
     *
     * @return 插件名称、版本等元数据。
     */
    PluginMetadata metadata();
}
