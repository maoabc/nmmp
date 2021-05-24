package com.nmmedit.protect;

import com.nmmedit.apkprotect.ApkFolders;
import com.nmmedit.apkprotect.ApkProtect;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter;
import com.nmmedit.apkprotect.dex2c.filters.BasicKeepConfig;
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import com.nmmedit.apkprotect.dex2c.filters.ProguardMappingConfig;
import com.nmmedit.apkprotect.obfus.MappingReader;
import com.nmmedit.apkprotect.sign.ApkVerifyCodeGenerator;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("No Input apk.");
            System.err.println("<inApk> ");
            System.exit(-1);
        }
        final File apk = new File(args[0]);
        final File outDir = new File(apk.getParentFile(), "build");


        ClassAndMethodFilter filterConfig = new BasicKeepConfig();
        if (args.length == 2) {
            final MappingReader mappingReader = new MappingReader(new File(args[1]));
            filterConfig = new ProguardMappingConfig(new BasicKeepConfig(), mappingReader) {
                @Override
                protected boolean keepClass(ClassDef classDef) {
                    final String className = getOriginClassName(classDef.getType());
                    //todo 自定义规则
                    return false;
                }


                @Override
                protected boolean keepMethod(Method method) {
                    return false;
                }
            };
        }

        final ApkFolders apkFolders = new ApkFolders(apk, outDir);

        //apk签名验证相关，不使用
        final ApkVerifyCodeGenerator apkVerifyCodeGenerator = null;

        final ApkProtect apkProtect = new ApkProtect.Builder(apkFolders)
                .setInstructionRewriter(new RandomInstructionRewriter())
                .setApkVerifyCodeGenerator(apkVerifyCodeGenerator)
                .setFilter(filterConfig)
                .build();
        apkProtect.run();
    }
}
