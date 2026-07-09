package com.codecore.gateway.plugin;

import java.util.Collections;
import java.util.Map;

/**
 * 认证请求上下文。
 * <p>
 * 由网关在 Filter 链中构造，传递给 {@link AuthPlugin#authenticate(AuthRequest)}。
 * </p>
 *
 * @param path    请求路径。
 * @param method  HTTP 方法。
 * @param clientIp 客户端 IP。
 * @param traceId 链路追踪 ID，可能为 null。
 * @param headers 请求头键值对（key 为 Header 名）。
 */
public record AuthRequest(
        String path,
        String method,
        String clientIp,
        String traceId,
        Map<String, String> headers
) {

    /**
     * 获取指定请求头的值（忽略大小写）。
     *
     * @param name Header 名称。
     * @return Header 值；不存在时返回 null。
     */
    public String getHeader(String name) {
        if (headers == null || name == null) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 构造空 headers 的便捷工厂（测试用）。
     *
     * @param path   路径。
     * @param method 方法。
     * @return 认证请求。
     */
    public static AuthRequest of(String path, String method) {
        return new AuthRequest(path, method, null, null, Collections.emptyMap());
    }
}
