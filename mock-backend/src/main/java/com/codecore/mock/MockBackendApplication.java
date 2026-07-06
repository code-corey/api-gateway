package com.codecore.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 下游模拟服务入口。
 * <p>
 * Stage 1 用于配合网关验证静态路由转发，提供固定端口的简单 HTTP 接口。
 * </p>
 */
@SpringBootApplication
public class MockBackendApplication {

    /**
     * 启动模拟下游服务。
     * <p>
     * 内部会初始化 Spring Web 容器并监听 8081 端口。
     * </p>
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(MockBackendApplication.class, args);
    }
}
