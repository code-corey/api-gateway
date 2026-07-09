package com.codecore.gateway.plugin;

import java.util.Collections;
import java.util.Map;

/**
 * 认证结果。
 *
 * @param status         认证状态。
 * @param message        失败或提示信息；成功时可为 null。
 * @param forwardHeaders 需转发给下游的 Header（如 X-User-Id）。
 */
public record AuthResult(
        AuthStatus status,
        String message,
        Map<String, String> forwardHeaders
) {

    /**
     * 构造认证通过结果。
     *
     * @return 通过结果。
     */
    public static AuthResult authenticated() {
        return new AuthResult(AuthStatus.AUTHENTICATED, null, Collections.emptyMap());
    }

    /**
     * 构造认证通过结果，并携带需转发的 Header。
     *
     * @param forwardHeaders 转发 Header。
     * @return 通过结果。
     */
    public static AuthResult authenticated(Map<String, String> forwardHeaders) {
        return new AuthResult(AuthStatus.AUTHENTICATED, null,
                forwardHeaders == null ? Collections.emptyMap() : Map.copyOf(forwardHeaders));
    }

    /**
     * 构造未认证结果。
     *
     * @param message 失败原因。
     * @return 未认证结果。
     */
    public static AuthResult unauthenticated(String message) {
        return new AuthResult(AuthStatus.UNAUTHENTICATED, message, Collections.emptyMap());
    }

    /**
     * 构造插件错误结果。
     *
     * @param message 错误说明。
     * @return 错误结果。
     */
    public static AuthResult error(String message) {
        return new AuthResult(AuthStatus.ERROR, message, Collections.emptyMap());
    }

    /**
     * 是否认证通过。
     *
     * @return 通过返回 true。
     */
    public boolean isAuthenticated() {
        return status == AuthStatus.AUTHENTICATED;
    }
}
