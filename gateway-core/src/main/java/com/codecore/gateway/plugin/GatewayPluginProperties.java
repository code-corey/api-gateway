package com.codecore.gateway.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关插件加载配置。
 * <p>
 * Stage 6 起从 plugins 目录静态加载认证插件 JAR。
 * </p>
 */
@ConfigurationProperties(prefix = "gateway.plugins")
public class GatewayPluginProperties {

    /**
     * 插件 JAR 所在目录，相对路径基于进程工作目录。
     */
    private String directory = "plugins";

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
