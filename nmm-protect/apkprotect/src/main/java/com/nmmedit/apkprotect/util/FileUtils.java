package com.nmmedit.apkprotect.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;

public class FileUtils {
    public static String getWorkPath() {
        return System.getProperty("user.dir");
    }

    public static String getHomePath() {
        CodeSource codeSource = FileUtils.class.getProtectionDomain().getCodeSource();
        try {
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            return jarFile.getParentFile().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return getHomePath();
        }
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
