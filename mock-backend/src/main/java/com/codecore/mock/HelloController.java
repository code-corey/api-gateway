package com.codecore.mock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模拟下游业务接口。
 * <p>
 * 提供最小化的 HTTP 响应，供网关路由转发联调使用。
 * </p>
 */
@RestController
public class HelloController {

    /**
     * 返回固定的问候响应。
     * <p>
     * 内部直接构造 JSON 结构体，不依赖数据库或其他外部服务。
     * </p>
     *
     * @return 包含问候消息的键值对。
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "hello from mock backend");
    }
}
