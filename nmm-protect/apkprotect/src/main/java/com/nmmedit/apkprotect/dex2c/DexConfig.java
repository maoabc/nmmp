package com.nmmedit.apkprotect.dex2c;

import com.nmmedit.apkprotect.dex2c.converter.JniCodeGenerator;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * dex处理后生成新dex及c代码,文件名结构配置
 */
public class DexConfig {
    private final File outputDir;
    private final String dexName;


    //jnicodegenerator 处理完成后,缓存已处理的类及方法
    private Set<String> nativeClasses;
    private Map<String, Integer> nativeMethodOffsets;

    public DexConfig(File outputDir, String dexFileName) {
        this.outputDir = outputDir;
        int i = dexFileName.lastIndexOf('.');
        if (i != -1) {
            this.dexName = dexFileName.substring(0, i);
        } else {
            this.dexName = dexFileName;
        }
    }

    public File getOutputDir() {
        return outputDir;
    }

    public String getDexName() {
        return dexName;
    }

    //每个处理过的class,需要调用这个类里的注册函数,注册函数名和classes.dex相关
    public String getRegisterNativesClassName() {
        return "com/nmmedit/protect/NativeUtil";
    }

    public String getRegisterNativesMethodName() {
        return getDexName() + "Init0";
    }

    @Nonnull
    public Set<String> getNativeClasses() {
        return nativeClasses;
    }

    public int getOffsetFromClassName(String className) {
        return nativeMethodOffsets.get(className);
    }

    public void setResult(JniCodeGenerator codeGenerator) {
        nativeClasses = codeGenerator.getNativeClasses();
        nativeMethodOffsets = codeGenerator.getNativeMethodOffsets();
    }


    /**
     * 方法被标识为native的dex,用于替换原dex
     */
    public File getNativeDexFile() {
        return new File(outputDir, dexName + "_native.dex");
    }

    /**
     * 符号dex文件,用于生成c代码
     */
    public File getSymbolDexFile() {
        return new File(outputDir, dexName + "_sym.dex");
    }

    /**
     * 本地方法实现
     */
    public File getNativeFunctionsFile() {
        return new File(outputDir, dexName + "_native_functions.c");
    }

    /**
     * 初始化代码头文件及初始化函数名,提供函数给外部调用
     */
    public HeaderFileAndSetupFuncName getHeaderFileAndSetupFunc() {
        return new HeaderFileAndSetupFuncName(
                new File(outputDir, dexName + "_native_functions.h"),
                dexName + "_setup");

    }


    /**
     * 符号解析器代码文件
     */
    public File getResolverFile() {
        return new File(outputDir, dexName + "_resolver.c");
    }

    public static class HeaderFileAndSetupFuncName {
        public final File headerFile;
        public final String setupFunctionName;

        private HeaderFileAndSetupFuncName(File headerFile, String setupFunctionName) {
            this.headerFile = headerFile;
            this.setupFunctionName = setupFunctionName;
        }
    }
}
