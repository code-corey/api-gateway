package com.codecore.gateway.plugin;

/**
 * 插件运行时上下文。
 * <p>
 * 插件通过此接口访问配置与日志，禁止直接依赖网关内部类。
 * </p>
 */
public interface PluginContext {

    /**
     * 读取字符串配置项。
     *
     * @param key 配置键。
     * @return 配置值；不存在时返回 null。
     */
    String getProperty(String key);

    /**
     * 读取字符串配置项，带默认值。
     *
     * @param key          配置键。
     * @param defaultValue 默认值。
     * @return 配置值或默认值。
     */
    default String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    /**
     * 记录 INFO 级别日志。
     *
     * @param message 日志内容。
     */
    void logInfo(String message);

    /**
     * 记录 WARN 级别日志。
     *
     * @param message 日志内容。
     */
    void logWarn(String message);
}
