package com.nmmedit.apkprotect.sign;

/**
 * 验证公钥,通过公钥生成c代码, 运行时读取apk签名块里公钥, 进行对比从而验证apk是否被重新签名
 */
public class ApkVerifyCodeGenerator {
    private final String keyStorePath;
    private final String alias;
    private final String keyStorePassword;

    public ApkVerifyCodeGenerator(String keyStorePath, String alias, String keyStorePassword) {
        this.keyStorePath = keyStorePath;
        this.alias = alias;
        this.keyStorePassword = keyStorePassword;
    }

    public String generate() {
        final byte[] publicKey = PublicKeyUtils.getPublicKey(keyStorePath, alias, keyStorePassword);
        if (publicKey == null) {
            throw new RuntimeException("publicKey == null");
        }
        final StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < publicKey.length; i++) {
            if (i % 10 == 0) {
                sb.append("    \\\\\n    ");
            }
            sb.append(String.format("0x%02x, ", publicKey[i] & 0xFF));
        }
        return sb.toString();
    }
}
