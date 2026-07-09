package com.codecore.gateway.plugin;

/**
 * 认证结果状态。
 */
public enum AuthStatus {

    /** 认证通过，允许继续转发。 */
    AUTHENTICATED,

    /** 认证失败，应拒绝请求（通常 401）。 */
    UNAUTHENTICATED,

    /** 匿名访问，插件明确允许跳过（本阶段 JWT 插件不使用）。 */
    ANONYMOUS,

    /** 插件内部错误，网关可按策略 fail-close。 */
    ERROR
}
