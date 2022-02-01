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
    public static final String CONFIG_PATH = new File(FileUtils.getHomePath(), "tools/" + (OsDetector.isWindows() ? "config-windows.json" : "config.json")).getAbsolutePath();

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
                e.printStackTrace();
            }
        }
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        try {
            String content = FileUtils.readFile(CONFIG_PATH, StandardCharsets.UTF_8);
            final Config config = gson.fromJson(content, Config.class);
            //compact old config
            if (config.environment == null) {
                //remove old config
                final File file = new File(CONFIG_PATH);
                file.delete();
                //load new config
                return config();
            }
            return config;
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
        return config().environment.sdk_path;
    }

    public static String cmakePath() {
        return config().environment.cmake_path;
    }


    public static String ndkPath() {
        return config().environment.ndk_path;
    }

    public static String ndkToolchains() {
        return config().environment.ndk_toolchains;
    }

    public static String ndkAbi() {
        return config().environment.ndk_abi;
    }

    public static String ndkStrip() {
        return config().environment.ndk_strip;
    }
}
