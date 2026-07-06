package com.codecore.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关主应用入口。
 * <p>
 * Stage 3 在 Stage 2 基础上将硬编码 Token 替换为 RS256 JWT 本地公钥验签。
 * </p>
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动网关应用。
     * <p>
     * 内部会初始化 Spring 容器、加载配置文件并暴露 Actuator 健康检查端点。
     * </p>
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
