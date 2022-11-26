package com.nmmedit.apkprotect.aar;

import com.nmmedit.apkprotect.util.FileUtils;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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


}