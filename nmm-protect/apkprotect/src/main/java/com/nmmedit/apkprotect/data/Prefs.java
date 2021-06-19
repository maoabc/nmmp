package com.nmmedit.apkprotect.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nmmedit.apkprotect.data.config.Config;
import com.nmmedit.apkprotect.data.config.Constants;
import com.nmmedit.apkprotect.util.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Prefs {
    public static Config config() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        try {
            String content = FileUtils.readFile(Constants.CONFIG_PATH, StandardCharsets.UTF_8);
            return gson.fromJson(content, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    public static String ndkToolchains() {
        return config().ndk.toolchains;
    }
}
