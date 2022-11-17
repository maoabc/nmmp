package com.nmmedit.apkprotect.aar;

import com.nmmedit.apkprotect.aar.asm.AsmUtils;
import com.nmmedit.apkprotect.util.FileUtils;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

public class ModuleProtectTest extends TestCase {

    private File getTestFile() throws IOException {
        final File tmp = File.createTempFile("tmp", "testAar.aar");
        try (
                final InputStream in = getClass().getResourceAsStream("/test-module.aar");
                final FileOutputStream out = new FileOutputStream(tmp);
        ) {
            FileUtils.copyStream(in, out);
        }
        return tmp;
    }


    public void testR8() throws IOException {
        final byte[] bytes = AsmUtils.genCfNativeUtil("com/nmmp/NativeUtil", "nmmp", Arrays.asList("classInit0", "classInit1"));
        final File file = new File("TestNative.class");

        Files.write(file.toPath(),bytes);
    }
}