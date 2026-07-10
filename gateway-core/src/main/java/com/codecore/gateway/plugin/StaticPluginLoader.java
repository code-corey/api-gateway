package com.codecore.gateway.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Stage 6 静态插件加载器。
 * <p>
 * 从 {@code plugins/} 目录扫描 plugin-jwt JAR，通过 Java SPI 实例化 {@link AuthPlugin}。
 * Stage 7 将演进为统一的 PluginManager + PluginClassLoader。
 * </p>
 */
@Component
public class StaticPluginLoader {

    private static final Logger log = LoggerFactory.getLogger(StaticPluginLoader.class);
    private static final String JWT_PLUGIN_PREFIX = "plugin-jwt";

    private final GatewayPluginProperties pluginProperties;

    /**
     * @param pluginProperties 插件目录配置。
     */
    public StaticPluginLoader(GatewayPluginProperties pluginProperties) {
        this.pluginProperties = pluginProperties;
    }

    /**
     * 从 plugins 目录加载 JWT 认证插件并初始化。
     *
     * @param pluginContext 插件运行时上下文。
     * @return 已初始化的认证插件实例。
     */
    public AuthPlugin loadAndInit(SpringPluginContext pluginContext) {
        Path pluginsDir = resolvePluginsDirectory();
        Path pluginJar = findJwtPluginJar(pluginsDir);
        log.info("从目录 {} 加载认证插件: {}", pluginsDir.toAbsolutePath(), pluginJar.getFileName());

        URLClassLoader pluginClassLoader = createPluginClassLoader(pluginJar);
        ServiceLoader<AuthPlugin> serviceLoader = ServiceLoader.load(AuthPlugin.class, pluginClassLoader);
        AuthPlugin plugin = serviceLoader.findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "插件 JAR 中未找到 AuthPlugin SPI 实现: " + pluginJar.getFileName()));

        plugin.init(pluginContext);
        return plugin;
    }

    /**
     * 解析插件目录路径。
     *
     * @return 插件目录绝对路径。
     */
    private Path resolvePluginsDirectory() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(pluginProperties.getDirectory()));
        candidates.add(Paths.get("target", pluginProperties.getDirectory()));
        candidates.add(resolveJarSiblingPluginsDirectory());

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        throw new IllegalStateException("插件目录不存在，已尝试: "
                + candidates.stream().map(path -> path.toAbsolutePath().normalize().toString()).toList()
                + "（请先执行 mvn package 生成 plugin-jwt JAR）");
    }

    /**
     * 解析与 gateway-core 可执行 JAR 同级的 plugins 目录。
     *
     * @return plugins 目录路径；无法解析时返回不存在路径占位。
     */
    private Path resolveJarSiblingPluginsDirectory() {
        try {
            URL location = StaticPluginLoader.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return Paths.get("__missing_plugins__");
            }
            Path codeSource = Paths.get(location.toURI());
            if (Files.isRegularFile(codeSource)) {
                return codeSource.getParent().resolve(pluginProperties.getDirectory());
            }
            Path moduleTarget = codeSource.getParent();
            if (moduleTarget != null && "classes".equals(moduleTarget.getFileName().toString())) {
                return moduleTarget.getParent().resolve(pluginProperties.getDirectory());
            }
        } catch (Exception ex) {
            log.debug("无法解析 JAR 同级 plugins 目录: {}", ex.getMessage());
        }
        return Paths.get("__missing_plugins__");
    }

    /**
     * 在插件目录中查找 plugin-jwt JAR。
     *
     * @param pluginsDir 插件目录。
     * @return 匹配的 JAR 路径。
     */
    private Path findJwtPluginJar(Path pluginsDir) {
        try (Stream<Path> files = Files.list(pluginsDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(JWT_PLUGIN_PREFIX) && name.endsWith(".jar");
                    })
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElseThrow(() -> new IllegalStateException(
                            "插件目录中未找到 " + JWT_PLUGIN_PREFIX + " JAR: " + pluginsDir));
        } catch (IOException ex) {
            throw new IllegalStateException("扫描插件目录失败: " + pluginsDir, ex);
        }
    }

    /**
     * 为插件 JAR 创建独立 ClassLoader，父加载器为 gateway-api。
     *
     * @param pluginJar 插件 JAR 路径。
     * @return 插件 ClassLoader。
     */
    private URLClassLoader createPluginClassLoader(Path pluginJar) {
        try {
            URL jarUrl = pluginJar.toUri().toURL();
            ClassLoader parent = AuthPlugin.class.getClassLoader();
            return new URLClassLoader(new URL[]{jarUrl}, parent);
        } catch (Exception ex) {
            throw new IllegalStateException("创建插件 ClassLoader 失败: " + pluginJar, ex);
        }
    }
}
