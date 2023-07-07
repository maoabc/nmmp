package com.nmmedit.dex2c;

import com.nmmedit.apkprotect.dex2c.Dex2c;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.MyMethodUtil;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.NoneInstructionRewriter;
import com.nmmedit.apkprotect.dex2c.converter.testbuild.ClassMethodImplCollection;
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.writer.pool.DexPool;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Dex2cTest {

    @Test
    public void testParseDex() throws IOException {
        parseDex(this.getClass().getResourceAsStream("/classes2.dex"));
    }

    @Test
    public void testDexSplit() throws IOException {
        File outdir = new File("/tmp", "outdir");
        if (!outdir.exists()) outdir.mkdirs();
        final InstructionRewriter instructionRewriter = new NoneInstructionRewriter();
        final ClassAnalyzer classAnalyzer = new ClassAnalyzer();
        final DexBackedDexFile dexFile = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new BufferedInputStream(this.getClass().getResourceAsStream("/classes2.dex")));
        classAnalyzer.loadDexFile(dexFile);

        Dex2c.handleDex(this.getClass().getResourceAsStream("/classes2.dex"),
                "classes.dex",
                testFilter,
                classAnalyzer,
                instructionRewriter,
                outdir);
    }

    public static ClassAndMethodFilter testFilter = new ClassAndMethodFilter() {

        @Override
        public boolean acceptClass(ClassDef classDef) {
            return classDef.getType().startsWith("Ltests/");
        }

        @Override
        public boolean acceptMethod(Method method) {
            return !MyMethodUtil.isConstructorOrAbstract(method) && !AccessFlags.BRIDGE.isSet(method.getAccessFlags());
        }
    };

    public static void parseDex(InputStream dexStream) throws IOException {
        DexBackedDexFile dexFile = DexBackedDexFile.fromInputStream(Opcodes.forApi(21), dexStream);
        DexPool dexPool = new DexPool(Opcodes.forApi(21));
        DexPool dexPoolMethodIml = new DexPool(Opcodes.forApi(21));


        StringBuilder sb = new StringBuilder();

        for (final ClassDef classDef : dexFile.getClasses()) {
            if (testFilter.acceptClass(classDef)) {
                dexPool.internClass(new ClassMethodToNative(classDef, testFilter));
                dexPoolMethodIml.internClass(new ClassMethodImplCollection(classDef, sb));
            } else {
                dexPool.internClass(classDef);
            }
        }

        //需要看输出文件可以自己制定目录
//        File outdir = File.createTempFile("mytest", "dex2c-dir");
//        if(!outdir.exists()) outdir.mkdirs();
//        dexPool.writeTo(new FileDataStore(new File(outdir,"classes2.dex")));
//        dexPoolMethodIml.writeTo(new FileDataStore(new File(outdir,"sym.dat")));

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
