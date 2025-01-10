package com.nmmedit.apkprotect.dex2c;

import com.android.tools.smali.dexlib2.iface.Method;
import com.google.common.collect.HashMultimap;
import com.nmmedit.apkprotect.dex2c.converter.JniCodeGenerator;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * dex处理后生成新dex及c代码,文件名结构配置
 */
public class DexConfig {
    private final File outputDir;
    private final String dexName;

    //记录壳dex被处理过的类及处理后的方法, 因为可能新增方法所以壳dex方法跟需要转换的dex并不相等
    //单独记录下来,主要后面交给java-asm处理转换前的class文件
    private HashMultimap<String, List<? extends Method>> shellMethods;

    //jnicodegenerator 处理完成后,缓存已处理的类及方法
    private Set<String> handledNativeClasses;
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
    public Set<String> getHandledNativeClasses() {
        return handledNativeClasses;
    }

    public int getOffsetFromClassName(String className) {
        return nativeMethodOffsets.get(className);
    }

    public void setResult(JniCodeGenerator codeGenerator) {
        handledNativeClasses = codeGenerator.getHandledNativeClasses();
        nativeMethodOffsets = codeGenerator.getNativeMethodOffsets();
    }


    public void setShellMethods(HashMultimap<String, List<? extends Method>> shellMethods) {
        this.shellMethods = shellMethods;
    }

    public HashMultimap<String, List<? extends Method>> getShellMethods() {
        return shellMethods;
    }

    /**
     * 方法被标识为native的dex,用于替换原dex
     */
    public File getShellDexFile() {
        return new File(outputDir, dexName + "_shell.dex");
    }

    /**
     * 符号及方法实现dex文件,用于生成c代码
     */
    public File getImplDexFile() {
        return new File(outputDir, dexName + "_impl.dex");
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