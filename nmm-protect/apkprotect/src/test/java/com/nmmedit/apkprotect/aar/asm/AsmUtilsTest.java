package com.nmmedit.apkprotect.aar.asm;

import com.nmmedit.apkprotect.util.FileUtils;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.*;

public class AsmUtilsTest {


    @Test
    public void testInjectStaticBlock() throws IOException {
        final InputStream t1 = getClass().getResourceAsStream("/test2_class");
        final ClassReader cls1 = new ClassReader(t1);
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        InjectStaticBlockVisitor cv = new InjectStaticBlockVisitor(Opcodes.ASM9, cw,
                "com/nmmp/NativeUtils", "classInit0", 5);

        cls1.accept(cv, ClassReader.SKIP_DEBUG);

        final File file = new File("/home/mao/adbi/t2.class");

        try (
                final FileOutputStream out = new FileOutputStream(file);
        ) {
            FileUtils.copyStream(new ByteArrayInputStream(cw.toByteArray()), out);
        }

    }
}