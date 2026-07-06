package com.codecore.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 网关应用上下文加载测试。
 * <p>
 * 验证 Spring Boot 应用能够正常启动并加载基础配置。
 * </p>
 */
@SpringBootTest
class GatewayApplicationTests {

    /**
     * 验证应用上下文可以成功加载。
     * <p>
     * 内部会启动完整的 Spring 容器；若配置或依赖有误则测试失败。
     * </p>
     */
    @Test
    void contextLoads() {
    }
}
