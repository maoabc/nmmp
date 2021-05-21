package com.nmmedit.apkprotect.sign;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Enumeration;

public class PublicKeyUtils {
    public static byte[] getPublicKey(String keyStorePath, String alias, String password) {
        try {
            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(keyStorePath), password.toCharArray());
            if (alias == null || "".equals(alias)) {
                final Enumeration<String> aliases = ks.aliases();
                if (aliases == null) {
                    return null;
                }
                if (aliases.hasMoreElements()) {
                    alias = aliases.nextElement();//取第一个证书别名
                }
            }
            final Certificate[] chain = ks.getCertificateChain(alias);
            if (chain == null || chain.length == 0) {
                throw new RuntimeException(
                        keyStorePath + " entry \"" + alias + "\" does not contain certificates");
            }

            final PublicKey publicKey = chain[0].getPublicKey();
            return publicKey.getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
