package com.codecore.gateway.plugin;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 认证插件管理器（Stage 7 冷加载版）。
 * <p>
 * 启动时扫描 plugins 目录中的全部 JAR，通过 {@link PluginClassLoader} + SPI 发现
 * {@link AuthPlugin} 实现，按配置名称激活并 init；进程关闭时 destroy 并关闭 ClassLoader。
 * Stage 8 将在此基础上支持运行时热替换。
 * </p>
 */
@Component
public class AuthPluginManager {

    private static final Logger log = LoggerFactory.getLogger(AuthPluginManager.class);

    private final GatewayPluginProperties pluginProperties;
    private final SpringPluginContext pluginContext;
    private final LoadedPlugin activePlugin;
    private final List<PluginMetadata> discoveredMetadata;

    /**
     * 扫描 plugins 目录并冷加载激活插件。
     * <p>
     * 内部依次执行：解析目录 → 扫描 JAR → SPI 发现 → 按 active 名称选中 → init。
     * </p>
     *
     * @param pluginProperties 插件目录与激活名称配置。
     * @param pluginContext    插件运行时上下文。
     */
    public AuthPluginManager(GatewayPluginProperties pluginProperties, SpringPluginContext pluginContext) {
        this.pluginProperties = pluginProperties;
        this.pluginContext = pluginContext;

        Path pluginsDir = resolvePluginsDirectory();
        List<LoadedPlugin> candidates = discoverPlugins(pluginsDir);
        this.discoveredMetadata = candidates.stream()
                .map(LoadedPlugin::metadata)
                .toList();

        try {
            this.activePlugin = selectAndInit(candidates);
        } catch (RuntimeException ex) {
            closeAllClassLoaders(candidates);
            throw ex;
        }

        log.info("认证插件已激活: {} v{}（JAR: {}）",
                activePlugin.metadata().name(),
                activePlugin.metadata().version(),
                activePlugin.jarPath().getFileName());
    }

    /**
     * 获取当前激活的认证插件。
     *
     * @return 当前插件实例。
     */
    public AuthPlugin getCurrentPlugin() {
        return activePlugin.plugin();
    }

    /**
     * 返回当前激活插件的元信息。
     *
     * @return 插件元数据。
     */
    public PluginMetadata getCurrentMetadata() {
        return activePlugin.metadata();
    }

    /**
     * 返回启动时发现的全部插件元信息（含未激活的）。
     *
     * @return 已发现插件元数据列表。
     */
    public List<PluginMetadata> listDiscoveredMetadata() {
        return List.copyOf(discoveredMetadata);
    }

    /**
     * 销毁当前插件并关闭 ClassLoader。
     * <p>
     * 内部先调用 {@link AuthPlugin#destroy()}，再关闭 {@link PluginClassLoader}。
     * </p>
     */
    @PreDestroy
    public void destroy() {
        try {
            activePlugin.plugin().destroy();
        } catch (Exception ex) {
            log.warn("插件 destroy 失败: {}", ex.getMessage());
        }
        try {
            activePlugin.classLoader().close();
            log.info("插件 ClassLoader 已关闭: {}", activePlugin.jarPath().getFileName());
        } catch (IOException ex) {
            log.warn("关闭插件 ClassLoader 失败: {}", ex.getMessage());
        }
    }

    /**
     * 扫描目录下全部 JAR，通过 SPI 发现 AuthPlugin 实现。
     *
     * @param pluginsDir 插件目录。
     * @return 已发现但尚未激活的插件列表。
     */
    private List<LoadedPlugin> discoverPlugins(Path pluginsDir) {
        List<Path> jars = listPluginJars(pluginsDir);
        if (jars.isEmpty()) {
            throw new IllegalStateException("插件目录中未找到任何 JAR: " + pluginsDir);
        }

        log.info("扫描插件目录 {}，共 {} 个 JAR", pluginsDir.toAbsolutePath(), jars.size());
        List<LoadedPlugin> discovered = new ArrayList<>();
        for (Path jar : jars) {
            discovered.addAll(loadFromJar(jar));
        }
        if (discovered.isEmpty()) {
            throw new IllegalStateException("plugins 目录中未发现任何 AuthPlugin SPI 实现: " + pluginsDir);
        }
        return discovered;
    }

    /**
     * 从单个 JAR 加载全部 AuthPlugin SPI 实现。
     *
     * @param pluginJar 插件 JAR 路径。
     * @return 该 JAR 中发现的插件列表；无 SPI 时返回空列表并关闭 ClassLoader。
     */
    private List<LoadedPlugin> loadFromJar(Path pluginJar) {
        PluginClassLoader classLoader = new PluginClassLoader(pluginJar);
        try {
            ServiceLoader<AuthPlugin> serviceLoader = ServiceLoader.load(AuthPlugin.class, classLoader);
            List<LoadedPlugin> plugins = new ArrayList<>();
            for (AuthPlugin plugin : serviceLoader) {
                log.info("发现插件: {} v{} ← {}",
                        plugin.metadata().name(),
                        plugin.metadata().version(),
                        pluginJar.getFileName());
                plugins.add(new LoadedPlugin(plugin, classLoader, pluginJar));
            }
            if (plugins.isEmpty()) {
                classLoader.close();
                log.warn("JAR 中未找到 AuthPlugin SPI，已跳过: {}", pluginJar.getFileName());
            }
            return plugins;
        } catch (Exception ex) {
            try {
                classLoader.close();
            } catch (IOException ignored) {
                // 忽略关闭失败
            }
            throw new IllegalStateException("加载插件 JAR 失败: " + pluginJar.getFileName(), ex);
        }
    }

    /**
     * 按配置名称选中插件并 init；关闭未选中插件的 ClassLoader。
     *
     * @param candidates 已发现的插件候选。
     * @return 已 init 的激活插件。
     */
    private LoadedPlugin selectAndInit(List<LoadedPlugin> candidates) {
        String activeName = pluginProperties.getActive();
        LoadedPlugin selected = candidates.stream()
                .filter(candidate -> candidate.metadata().name().equals(activeName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "未找到名为 '" + activeName + "' 的插件，已发现: "
                                + candidates.stream()
                                .map(c -> c.metadata().name())
                                .collect(Collectors.joining(", "))));

        for (LoadedPlugin candidate : candidates) {
            if (candidate == selected) {
                continue;
            }
            // 同一 JAR 多个实现时共享 ClassLoader，只有非选中且独立 CL 才关闭
            if (candidate.classLoader() != selected.classLoader()) {
                try {
                    candidate.classLoader().close();
                } catch (IOException ex) {
                    log.warn("关闭未激活插件 ClassLoader 失败: {}", ex.getMessage());
                }
            }
        }

        selected.plugin().init(pluginContext);
        return selected;
    }

    /**
     * 关闭候选插件列表中的全部 ClassLoader（去重）。
     *
     * @param candidates 插件候选列表。
     */
    private void closeAllClassLoaders(List<LoadedPlugin> candidates) {
        candidates.stream()
                .map(LoadedPlugin::classLoader)
                .distinct()
                .forEach(classLoader -> {
                    try {
                        classLoader.close();
                    } catch (IOException ex) {
                        log.warn("关闭插件 ClassLoader 失败: {}", ex.getMessage());
                    }
                });
    }

    /**
     * 列出插件目录下全部 JAR（按文件名排序）。
     *
     * @param pluginsDir 插件目录。
     * @return JAR 路径列表。
     */
    private List<Path> listPluginJars(Path pluginsDir) {
        try (Stream<Path> files = Files.list(pluginsDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("扫描插件目录失败: " + pluginsDir, ex);
        }
    }

    /**
     * 解析插件目录路径。
     * <p>
     * 依次尝试：配置路径 → target/plugins → 可执行 JAR 同级 plugins。
     * </p>
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
            URL location = AuthPluginManager.class.getProtectionDomain().getCodeSource().getLocation();
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
}
