package com.codecore.gateway.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PluginClassLoader} 单元测试。
 */
class PluginClassLoaderTests {

    /**
     * 验证 ClassLoader 能关联 JAR 路径且 parent 持有 AuthPlugin。
     *
     * @param tempDir 临时目录。
     */
    @Test
    void shouldExposeJarPathAndShareAuthPluginFromParent(@TempDir Path tempDir) throws Exception {
        Path emptyJar = tempDir.resolve("empty-plugin.jar");
        Files.write(emptyJar, new byte[]{0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        try (PluginClassLoader classLoader = new PluginClassLoader(emptyJar)) {
            assertEquals(emptyJar, classLoader.getPluginJar());
            assertNotNull(classLoader.getParent());
            assertEquals(AuthPlugin.class, classLoader.loadClass(AuthPlugin.class.getName()));
            assertTrue(classLoader.loadClass(AuthPlugin.class.getName()) == AuthPlugin.class);
        }
    }
}
