package com.nmmedit.apkprotect;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BuildNativeLib {

    //编译出native lib，同时返回最后的so文件
    public static List<File> build(@Nonnull String scriptPath, CMakeOptions options) throws IOException {
        final ProcessBuilder builder = new ProcessBuilder("sh", scriptPath)
                .directory(new File(options.getProjectHome()));
        //通过环境变量给脚本传递参数
        final Map<String, String> environment = builder.environment();
        environment.put("PROJECT_ROOT", options.getProjectHome());
        environment.put("ABI", options.getAbi());
        environment.put("API_LEVEL", options.getApiLevel() + "");
        environment.put("BUILD_TYPE", options.getBuildType().getBuildTypeName());
        environment.put("ANDROID_SDK_HOME", options.getSdkHome());
        environment.put("ANDROID_NDK_HOME", options.getNdkHome());
        environment.put("CMAKE_PATH", options.getCmakePath());
        environment.put("LIBRARY_OUTPUT_DIRECTORY", options.getLibOutputDir());

        final Process process = builder.start();

        printOutput(process.getInputStream());
        printOutput(process.getErrorStream());

        try {
            final int exitStatus = process.waitFor();
            if (exitStatus != 0) {
                throw new IOException("Build failed");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //编译成功的so
        return Arrays.asList(
                new File(options.getLibOutputDir(), "libnmmvm.so"),
                new File(options.getLibOutputDir(), "libnmmp.so")
        );
    }

    private static void printOutput(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    /**
     * cmake相关配置
     */
    public static class CMakeOptions {
        @Nonnull
        private final String cmakePath;

        @Nonnull
        private final String sdkHome;

        @Nonnull
        private final String ndkHome;
        private final int apiLevel;
        @Nonnull
        private final String projectHome;

        @Nonnull
        private final BuildType buildType;

        @Nonnull
        private final String abi;

        public CMakeOptions(@Nonnull String cmakePath, @Nonnull String sdkHome, @Nonnull String ndkHome,
                            int apiLevel, @Nonnull String projectHome,
                            @Nonnull BuildType buildType, @Nonnull String abi) {
            this.cmakePath = cmakePath;
            this.sdkHome = sdkHome;
            this.ndkHome = ndkHome;
            this.apiLevel = apiLevel;
            this.projectHome = projectHome;
            this.buildType = buildType;
            this.abi = abi;
        }

        @Nonnull
        public String getCmakePath() {
            return cmakePath;
        }

        @Nonnull
        public String getSdkHome() {
            return sdkHome;
        }

        @Nonnull
        public String getNdkHome() {
            return ndkHome;
        }

        public int getApiLevel() {
            return apiLevel;
        }

        @Nonnull
        public String getProjectHome() {
            return projectHome;
        }

        @Nonnull
        public BuildType getBuildType() {
            return buildType;
        }

        @Nonnull
        public String getAbi() {
            return abi;
        }

        @Nonnull
        public String getLibOutputDir() {
            return projectHome + "/obj/" + abi;
        }

        public enum BuildType {
            DEBUG("Debug"),
            RELEASE("Release");


            private final String buildTypeName;

            BuildType(String buildTypeName) {
                this.buildTypeName = buildTypeName;
            }

            public String getBuildTypeName() {
                return buildTypeName;
            }
        }
    }
}
