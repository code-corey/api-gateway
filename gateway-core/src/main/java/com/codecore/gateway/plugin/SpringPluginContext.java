package com.codecore.gateway.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring Environment 的插件上下文实现。
 * <p>
 * 将 gateway.auth.* 等配置与 SLF4J 日志暴露给 {@link AuthPlugin} 实现类。
 * </p>
 */
@Component
public class SpringPluginContext implements PluginContext {

    private static final Logger log = LoggerFactory.getLogger(SpringPluginContext.class);

    private final Environment environment;

    /**
     * @param environment Spring 环境配置。
     */
    public SpringPluginContext(Environment environment) {
        this.environment = environment;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 内部从 Spring Environment 按 key 读取属性值。
     * </p>
     */
    @Override
    public String getProperty(String key) {
        return environment.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logInfo(String message) {
        log.info(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logWarn(String message) {
        log.warn(message);
    }
}
