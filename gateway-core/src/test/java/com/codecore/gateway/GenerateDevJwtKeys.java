package com.codecore.gateway;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 开发用 JWT RSA 密钥对生成入口。
 * <p>
 * 将密钥写入项目约定路径：
 * <ul>
 *   <li>私钥：{@code src/test/resources/jwt/dev-private.pem}</li>
 *   <li>公钥：{@code src/main/resources/jwt/dev-public.pem}</li>
 * </ul>
 * 在 IDE 中运行 {@link #main(String[])}，或在 {@code gateway-core} 模块下执行本类即可。
 * </p>
 */
public final class GenerateDevJwtKeys {

    private static final String PRIVATE_RELATIVE = "src/test/resources/jwt/dev-private.pem";
    private static final String PUBLIC_RELATIVE = "src/main/resources/jwt/dev-public.pem";

    private GenerateDevJwtKeys() {
    }

    /**
     * 生成密钥对并写入默认路径。
     * <p>
     * 内部先定位 gateway-core 模块根目录，再调用 {@link RsaKeyPairGenerator} 生成 2048 位 RSA 密钥，
     * 最后分别写入测试私钥与主资源公钥文件。
     * </p>
     *
     * @param args 可选：{@code [私钥输出路径] [公钥输出路径]}，不传则使用默认相对路径。
     */
    public static void main(String[] args) throws Exception {
        Path moduleRoot = resolveModuleRoot();
        Path privateKeyPath = args.length > 0 ? Path.of(args[0]) : moduleRoot.resolve(PRIVATE_RELATIVE);
        Path publicKeyPath = args.length > 1 ? Path.of(args[1]) : moduleRoot.resolve(PUBLIC_RELATIVE);

        RsaKeyPairGenerator.RsaKeyPairPem keyPair = RsaKeyPairGenerator.generate();

        Files.createDirectories(privateKeyPath.getParent());
        Files.createDirectories(publicKeyPath.getParent());
        Files.writeString(privateKeyPath, keyPair.privateKeyPem());
        Files.writeString(publicKeyPath, keyPair.publicKeyPem());

        System.out.println("RSA 密钥对已生成：");
        System.out.println("  私钥: " + privateKeyPath.toAbsolutePath().normalize());
        System.out.println("  公钥: " + publicKeyPath.toAbsolutePath().normalize());
        System.out.println();
        System.out.println("说明：私钥仅用于测试签发 JWT；公钥供网关验签加载。");
    }

    /**
     * 解析 gateway-core 模块根目录。
     * <p>
     * 优先使用当前工作目录；若不在模块内，则尝试 {@code gateway-core} 子目录。
     * </p>
     *
     * @return 模块根路径。
     */
    static Path resolveModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(cwd.resolve("src/test/resources"))) {
            return cwd;
        }
        Path nested = cwd.resolve("gateway-core");
        if (Files.isDirectory(nested.resolve("src/test/resources"))) {
            return nested;
        }
        throw new IllegalStateException(
                "无法定位 gateway-core 模块根目录，请在 gateway-core 目录或项目根目录下运行。");
    }
}
