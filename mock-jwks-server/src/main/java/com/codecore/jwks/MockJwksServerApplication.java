package com.codecore.jwks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模拟 Auth 服务入口。
 * <p>
 * Stage 4 提供 JWKS 端点，供网关拉取 RSA 公钥并演示密钥轮换。
 * </p>
 */
@SpringBootApplication
public class MockJwksServerApplication {

    /**
     * 启动模拟 JWKS 服务。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(MockJwksServerApplication.class, args);
    }
}
