package com.codecore.gateway.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关插件加载配置。
 * <p>
 * Stage 7 起支持扫描 plugins 目录，并按 {@code active} 名称激活指定认证插件。
 * </p>
 */
@ConfigurationProperties(prefix = "gateway.plugins")
public class GatewayPluginProperties {

    /**
     * 插件 JAR 所在目录，相对路径基于进程工作目录。
     */
    private String directory = "plugins";

    /**
     * 启动时激活的插件名称，须与 {@link PluginMetadata#name()} 一致。
     */
    private String active = "jwt-auth";

    /**
     * @return 插件目录配置。
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @param directory 插件目录。
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * @return 激活插件名称。
     */
    public String getActive() {
        return active;
    }

    /**
     * @param active 激活插件名称。
     */
    public void setActive(String active) {
        this.active = active;
    }
}
