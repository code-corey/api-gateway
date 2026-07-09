package com.codecore.gateway.auth;

/**
 * 本地联调 JWT 打印工具。
 * <p>
 * 在 IDE 中运行 main 方法，或使用 {@code mvn -pl gateway-core exec:java} 获取测试用 JWT。
 * </p>
 */
public final class JwtDevTokenPrinter {

    private JwtDevTokenPrinter() {
    }

    /**
     * 打印一张可用于 curl 联调的有效 JWT。
     *
     * @param args 命令行参数（未使用）。
     */
    public static void main(String[] args) throws Exception {
        String token = JwtTestTokenHelper.validToken();
        System.out.println("Bearer " + token);
        System.out.println();
        System.out.println("curl.exe -H \"Authorization: Bearer " + token + "\" http://localhost:8080/api/hello");
    }
}
