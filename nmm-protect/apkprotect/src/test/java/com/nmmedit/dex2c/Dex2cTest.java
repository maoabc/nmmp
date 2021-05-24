package com.nmmedit.dex2c;

import com.nmmedit.apkprotect.dex2c.Dex2c;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.NoneInstructionRewriter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Dex2cTest {

    @Test
    public void testParseDex() throws IOException {
        Dex2c.parseDex(this.getClass().getResourceAsStream("/classes2.dex"));
    }

    @Test
    public void testDexSplit() throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/classes2.dex");
        File outdir = new File("/tmp","outdir");
        if (!outdir.exists()) outdir.mkdirs();
        final InstructionRewriter instructionRewriter = new NoneInstructionRewriter();
        Dex2c.handleDex(resourceAsStream,
                "classes.dex",
                Dex2c.testFilter,
                instructionRewriter,
                outdir);
    }

    @Test
    public void testDexConvert() throws IOException {
//        File dexdir = new File("/home/mao/estest/");
//        File outdir = new File(dexdir, "dex2c");
//        if (!outdir.exists()) outdir.mkdirs();
//        ArrayList<File> dexes = new ArrayList<>();
//        for (File file : dexdir.listFiles()) {
//            if (file.getName().endsWith(".dex")) {
//                dexes.add(file);
//            }
//        }
//
//        final InstructionRewriter instructionRewriter = new NoneInstructionRewriter();
//        final GlobalDexConfig globalConfig = Dex2c.handleDexes(dexes, new ClassAndMethodFilter() {
//                    @Override
//                    public boolean acceptClass(ClassDef classDef) {
//                        if (
//                                classDef.getType().startsWith("Landroid/") ||
//                                        classDef.getType().startsWith("Landroidx/")
//                        ) {
//                            return false;
//                        }
//                        return true;
//                    }
//
//                    @Override
//                    public boolean acceptMethod(Method method) {
//                        return !MyMethodUtil.isConstructorOrAbstract(method) &&
//                                !MyMethodUtil.isBridgeOrSynthetic(method);
//                    }
//                },
//                instructionRewriter,
//                outdir);
//
    }
}