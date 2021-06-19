package com.nmmedit.apkprotect.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nmmedit.apkprotect.data.config.Config;
import com.nmmedit.apkprotect.util.FileUtils;
import com.nmmedit.apkprotect.util.OsDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Prefs {
    public static final String CONFIG_PATH = new File(FileUtils.getHomePath(), "tools/config.json").getAbsolutePath();

    public static Config config() {
        final File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try (
                    final InputStream inputStream = Prefs.class.getResourceAsStream("/" + (OsDetector.isWindows() ? "config-windows.json" : "config.json"));
                    final FileOutputStream outputStream = new FileOutputStream(configFile);
            ) {
                FileUtils.copyStream(inputStream, outputStream);
            } catch (IOException e) {
            }
        }
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        try {
            String content = FileUtils.readFile(CONFIG_PATH, StandardCharsets.UTF_8);
            return gson.fromJson(content, Config.class);
        } catch (IOException e) {
            throw new RuntimeException("Load config failed", e);
        }
    }

    public static boolean isArm() {
        return config().abi.arm;
    }

    public static boolean isArm64() {
        return config().abi.arm64;
    }

    public static boolean isX86() {
        return config().abi.x86;
    }

    public static boolean isX64() {
        return config().abi.x64;
    }

    public static String sdkPath() {
        return config().path.sdk;
    }

    public static String cmakePath() {
        return config().path.cmake;
    }

    public static String ndkPath() {
        return config().path.ndk;
    }

    public static String osName() {
        return config().ndk.osName;
    }
}
