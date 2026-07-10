package com.codecore.gateway.plugin.jwt;

/**
 * JWT 校验结果。
 *
 * @param valid   是否通过校验。
 * @param message 失败时的原因说明；通过时为 null。
 */
public record JwtValidationResult(boolean valid, String message) {

    /**
     * 构造校验通过结果。
     *
     * @return 通过结果。
     */
    public static JwtValidationResult success() {
        return new JwtValidationResult(true, null);
    }

    /**
     * 构造校验失败结果。
     *
     * @param message 失败原因。
     * @return 失败结果。
     */
    public static JwtValidationResult failure(String message) {
        return new JwtValidationResult(false, message);
    }
}
