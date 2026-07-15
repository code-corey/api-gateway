package com.codecore.gateway.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * 插件专用 ClassLoader。
 * <p>
 * 内部为每个插件 JAR 创建独立的 {@link URLClassLoader}，父加载器指向
 * {@link AuthPlugin}（gateway-api）所在 ClassLoader，保证 SPI 接口只有一份；
 * 支持 {@link #close()} 释放 JAR 文件句柄，为 Stage 8 热替换做准备。
 * </p>
 */
public class PluginClassLoader extends URLClassLoader {

    private final Path pluginJar;

    /**
     * 为指定插件 JAR 创建 ClassLoader。
     * <p>
     * 内部将 JAR 转为 URL，并以 AuthPlugin 的 ClassLoader 作为 parent。
     * </p>
     *
     * @param pluginJar 插件 JAR 路径。
     */
    public PluginClassLoader(Path pluginJar) {
        super(toUrls(pluginJar), AuthPlugin.class.getClassLoader());
        this.pluginJar = pluginJar;
    }

    /**
     * 返回本 ClassLoader 对应的插件 JAR 路径。
     *
     * @return 插件 JAR 绝对路径。
     */
    public Path getPluginJar() {
        return pluginJar;
    }

    /**
     * 关闭本 ClassLoader，释放对 JAR 文件的占用。
     * <p>
     * 内部调用父类 {@link URLClassLoader#close()}，便于后续替换同名 JAR。
     * </p>
     *
     * @throws IOException 关闭资源失败时抛出。
     */
    @Override
    public void close() throws IOException {
        super.close();
    }

    /**
     * 将 JAR 路径转为 URL 数组。
     *
     * @param pluginJar 插件 JAR 路径。
     * @return 仅含该 JAR 的 URL 数组。
     */
    private static URL[] toUrls(Path pluginJar) {
        try {
            return new URL[]{pluginJar.toUri().toURL()};
        } catch (Exception ex) {
            throw new IllegalStateException("无法将插件 JAR 转为 URL: " + pluginJar, ex);
        }
    }
}
