package com.nmmedit.apkprotect.dex2c;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class GlobalDexConfig {

    private final ArrayList<DexConfig> configs = new ArrayList<>();

    private final File outputDir;

    public GlobalDexConfig(File outputDir) {
        this.outputDir = outputDir;
    }

    public File getInitCodeFile() {
        return new File(outputDir, "jni_init.c");
    }

    public void addDexConfig(DexConfig config) {
        configs.add(config);
    }

    public List<DexConfig> getConfigs() {
        return configs;
    }

    public void generateJniInitCode() throws IOException {
        try (
                final FileWriter writer = new FileWriter(getInitCodeFile());
        ) {
            generateJniInitCode(writer);
        }
    }

    private void generateJniInitCode(Writer writer) throws IOException {
        final StringBuilder includeStaOrExternFunc = new StringBuilder();

        final StringBuilder initCallSta = new StringBuilder();

        for (DexConfig config : configs) {
            final DexConfig.HeaderFileAndSetupFuncName setupFunc = config.getHeaderFileAndSetupFunc();
            includeStaOrExternFunc.append(String.format("extern void %s(JNIEnv *env);\n", setupFunc.setupFunctionName));
            initCallSta.append(String.format("    %s(env);\n", setupFunc.setupFunctionName));
        }

        writer.write(String.format(
                "#include <jni.h>\n" +
                        "#include \"GlobalCache.h\"\n" +
                        "\n" +
                        "//auto generated\n" +
                        "%s" +
                        "\n" +
                        "\n" +
                        "JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {\n" +
                        "    JNIEnv *env;\n" +
                        "    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {\n" +
                        "        return -1;\n" +
                        "    }\n" +
                        "    cacheInitial(env);\n" +
                        "\n" +
                        "\n" +
                        "    //auto generated setup function\n" +
                        "%s" +
                        "\n" +
                        "\n" +
                        "    return JNI_VERSION_1_6;\n" +
                        "}\n\n\n",
                includeStaOrExternFunc.toString(), initCallSta.toString()));
    }
}