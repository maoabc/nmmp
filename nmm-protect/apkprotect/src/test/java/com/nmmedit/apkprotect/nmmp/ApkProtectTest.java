package com.nmmedit.apkprotect.nmmp;

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
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ApkProtectTest {


    @Test
    public void testConvert() throws IOException {
        final File apkBuildDir = new File("/home/mao/nmmp/nmm/outputs182/");
        final File apk = new File(apkBuildDir, "apk/release/app_release_arm64-v8a_1.8.2_20210516.apk");

        final File outDir = new File("/home/mao/nmmp/nmm");

        final MappingReader mappingReader = new MappingReader(new File(apkBuildDir, "mapping/release/mapping.txt"));
        final ClassAndMethodFilter keepConfig = new ProguardMappingConfig(new BasicKeepConfig(), mappingReader) {
            @Override
            protected boolean keepClass(ClassDef classDef) {
                final String className = getOriginClassName(classDef.getType());


                if (className.startsWith("com.nmmedit")) {//只处理自己包名下的class
                    return false;
                }
                return true;
            }

            private boolean keepClassOrChild(ClassDef classDef, String prefixPageRecord) {

                final String className = getOriginClassName(classDef.getType());
                if (className.startsWith(prefixPageRecord)) {
                    return true;
                }
                final String oldClassName = getOriginClassName(classDef.getSuperclass());
                if (oldClassName != null && oldClassName.startsWith(prefixPageRecord)) {
                    return true;
                }
                return false;
            }

            @Override
            protected boolean keepMethod(Method method) {
                return false;
            }
        };

        final ApkFolders apkFolders = new ApkFolders(apk, outDir);

        final ApkVerifyCodeGenerator apkVerifyCodeGenerator = null;

        final ApkProtect apkProtect = new ApkProtect.Builder(apkFolders)
                .setInstructionRewriter(new RandomInstructionRewriter())
                .setApkVerifyCodeGenerator(apkVerifyCodeGenerator)
                .setFilter(keepConfig)
                .build();
        apkProtect.run();
    }

}
