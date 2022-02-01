package com.nmmedit.apkprotect;

import com.nmmedit.apkprotect.data.Prefs;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class BuildNativeLib {
    //库名称
    public static final String NMMP_NAME = "nmmp";

    //
    //虚拟机库名称,如果cmake里配置为静态库,这个可以忽略
    public static final String VM_NAME = "nmmvm";


    //编译出native lib，同时返回最后的so文件
    public static List<File> build(@NotNull CMakeOptions options) throws IOException {

        final List<String> cmakeArguments = options.getCmakeArguments();
        //cmake
        execCmd(cmakeArguments);

        //cmake --build <dir>
        execCmd(Arrays.asList(
                options.getCmakeBinaryPath(),
                "--build",
                options.getBuildPath()
        ));
        //strip
        final List<File> sharedObjectPath = options.getSharedObjectFile();
        for (File file : sharedObjectPath) {
            execCmd(Arrays.asList(
                            options.getStripBinaryPath(),
                            "--strip-unneeded",
                            file.getAbsolutePath()
                    )
            );
        }

        //编译成功的so
        return sharedObjectPath;
    }

    private static void execCmd(List<String> cmds) throws IOException {
        System.out.println(cmds);
        final ProcessBuilder builder = new ProcessBuilder()
                .command(cmds);

        final Process process = builder.start();

        printOutput(process.getInputStream());
        printOutput(process.getErrorStream());

        try {
            final int exitStatus = process.waitFor();
            if (exitStatus != 0) {
                throw new IOException(String.format("Cmd '%s' exec failed", cmds.toString()));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        private final String cmakePath;

        private final String sdkHome;

        private final String ndkHome;
        private final int apiLevel;
        private final String projectHome;

        private final BuildType buildType;

        private final String abi;

        public CMakeOptions(String cmakePath,
                            String sdkHome,
                            String ndkHome,
                            int apiLevel,
                            String projectHome,
                            BuildType buildType,
                            String abi) {
            this.cmakePath = cmakePath;
            this.sdkHome = sdkHome;
            this.ndkHome = ndkHome;
            this.apiLevel = apiLevel;
            this.projectHome = projectHome;
            this.buildType = buildType;
            this.abi = abi;
        }

        public String getCmakePath() {
            return cmakePath;
        }

        public String getSdkHome() {
            return sdkHome;
        }

        public String getNdkHome() {
            return ndkHome;
        }

        public int getApiLevel() {
            return apiLevel;
        }

        public String getProjectHome() {
            return projectHome;
        }

        public BuildType getBuildType() {
            return buildType;
        }

        public String getAbi() {
            return abi;
        }

        public String getLibOutputDir() {
            return new File(new File(getProjectHome(), "obj"), abi).getAbsolutePath();
        }


        //camke --build <BuildPath>
        public String getBuildPath() {
            return new File(getProjectHome(),
                    String.format(".cxx/cmake/%s/%s", getBuildType().getBuildTypeName(), getAbi())).getAbsolutePath();
        }

        public String getStripBinaryPath() {
            return new File(getNdkHome(), Prefs.ndkToolchains() + "/" +
                    Prefs.ndkAbi() + "/" + Prefs.ndkStrip()).getAbsolutePath();
        }

        public String getCmakeBinaryPath() {
            return new File(getCmakePath(), "/bin/cmake").getAbsolutePath();
        }

        public String getNinjaBinaryPath() {
            return new File(getCmakePath(), "/bin/ninja").getAbsolutePath();
        }

        public List<String> getCmakeArguments() {
            return Arrays.asList(
                    getCmakeBinaryPath(),
                    String.format("-H%s", new File(getProjectHome(), "dex2c").getAbsoluteFile()),
                    String.format("-DCMAKE_TOOLCHAIN_FILE=%s", new File(getNdkHome(), "/build/cmake/android.toolchain.cmake").getAbsoluteFile()),
                    String.format("-DCMAKE_BUILD_TYPE=%s", getBuildType().getBuildTypeName()),
                    String.format("-DANDROID_ABI=%s", getAbi()),
                    String.format("-DANDROID_NDK=%s", getNdkHome()),
                    String.format("-DANDROID_PLATFORM=android-%d", getApiLevel()),
                    String.format("-DCMAKE_ANDROID_ARCH_ABI=%s", getAbi()),
                    String.format("-DCMAKE_ANDROID_NDK=%s", getNdkHome()),
                    "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                    String.format("-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=%s", getLibOutputDir()),
                    String.format("-DCMAKE_MAKE_PROGRAM=%s", getNinjaBinaryPath()),
                    "-DCMAKE_SYSTEM_NAME=Android",
                    String.format("-DCMAKE_SYSTEM_VERSION=%d", getApiLevel()),
                    String.format("-B%s", getBuildPath()),
                    "-GNinja");
        }

        //最后输出的so文件
        public List<File> getSharedObjectFile() {
            //linux,etc.
            final String vmLibName = "lib" + VM_NAME + ".so";

            final String nmmpLibName = "lib" + NMMP_NAME + ".so";

            File vmSo = new File(getLibOutputDir(), vmLibName);
            File mpSo = new File(getLibOutputDir(), nmmpLibName);

            if (!vmSo.exists()) {
                //windows
                vmSo = new File(getBuildPath(), "vm/" + vmLibName);
            }
            if (!vmSo.exists()) {
                throw new RuntimeException("Not Found so: " + vmSo.getAbsolutePath());
            }

            if (!mpSo.exists()) {
                mpSo = new File(getBuildPath(), nmmpLibName);
            }
            if (!mpSo.exists()) {
                throw new RuntimeException("Not Found so: " + mpSo.getAbsolutePath());
            }

            return Arrays.asList(vmSo, mpSo);
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
