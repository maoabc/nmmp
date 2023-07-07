package com.nmmedit.apkprotect.aar;

import com.android.tools.r8.D8;
import com.android.zipflinger.*;
import com.google.common.collect.HashMultimap;
import com.nmmedit.apkprotect.BuildNativeLib;
import com.nmmedit.apkprotect.aar.asm.AsmMethod;
import com.nmmedit.apkprotect.aar.asm.AsmUtils;
import com.nmmedit.apkprotect.aar.asm.InjectStaticBlockVisitor;
import com.nmmedit.apkprotect.aar.asm.MethodToNativeVisitor;
import com.nmmedit.apkprotect.dex2c.Dex2c;
import com.nmmedit.apkprotect.dex2c.DexConfig;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.MyMethodUtil;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import com.nmmedit.apkprotect.util.CmakeUtils;
import com.nmmedit.apkprotect.util.FileUtils;
import org.jf.dexlib2.iface.Method;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.util.*;
import java.util.zip.Deflater;

public class AarProtect {
    private final AarFolders aarFolders;
    private final InstructionRewriter instructionRewriter;

    private final ClassAndMethodFilter filter;

    private final ClassAnalyzer classAnalyzer;

    private AarProtect(AarFolders aarFolders,
                       InstructionRewriter instructionRewriter,
                       ClassAndMethodFilter filter,
                       ClassAnalyzer classAnalyzer) {
        this.aarFolders = aarFolders;
        this.instructionRewriter = instructionRewriter;
        this.filter = filter;
        this.classAnalyzer = classAnalyzer;
    }

    public void run() throws IOException {
        final File aar = aarFolders.getAar();
        if (!aar.exists()) {
            throw new FileNotFoundException(aar.getAbsolutePath());
        }
        final File dexJar = classesJarToDexJar();

        final File zipExtractTempDir = aarFolders.apkFolders.getZipExtractTempDir();
        try {
            final File classesDex = getClassesDex(dexJar, zipExtractTempDir);

            CmakeUtils.generateCSources(aarFolders.apkFolders.getDex2cSrcDir(), instructionRewriter);


            //
            classAnalyzer.loadDexFile(classesDex);


            final DexConfig dexConfig = Dex2c.handleModuleDex(classesDex,
                    filter,
                    classAnalyzer,
                    instructionRewriter,
                    aarFolders.apkFolders.getCodeGeneratedDir());

            //根据处理过的dex信息修改转换前的class文件
            final File newClassesJar = modifyClassFiles(dexConfig);


            final Map<String, Map<File, File>> nativeLibs = BuildNativeLib.generateNativeLibs(aarFolders.apkFolders.getOutRootDir(),
                    getAbis());

            final ZipMap zipMap = ZipMap.from(aar.toPath());
            final ZipSource zipSource = new ZipSource(zipMap);

            final File outputAarFile = aarFolders.getOutputAar();
            if (outputAarFile.exists()) FileUtils.deleteFile(outputAarFile);

            try (
                    final ZipArchive outAar = new ZipArchive(outputAarFile.toPath());
            ) {

                for (Map.Entry<String, Entry> entry : zipMap.getEntries().entrySet()) {
                    final String name = entry.getKey();
                    if ("classes.jar".equals(name)) {//不需要原来aar中的classes.jar
                        continue;
                    }
                    final Entry v = entry.getValue();
                    if (v.isDirectory()) {
                        continue;
                    }
                    zipSource.select(name, name);
                }
                //添加没修改过的文件
                outAar.add(zipSource);

                //add classes.jar
                outAar.add(Sources.from(newClassesJar, "classes.jar", Deflater.DEFAULT_COMPRESSION));

                //todo 可能需要修改proguard规则文件，保留添加的新类及被修改过的class

                //add native libs
                for (Map.Entry<String, Map<File, File>> entry : nativeLibs.entrySet()) {
                    final String abi = entry.getKey();
                    //todo 不清楚是否应该使用未strip的.so文件
                    for (File file : entry.getValue().values()) {
                        final Source source = Sources.from(file, "jni/" + abi + "/" + file.getName(), Deflater.DEFAULT_COMPRESSION);
                        outAar.add(source);
                    }
                    //不清楚是否需要，安全起见还是加上目录项
                    outAar.add(Sources.dir(abi + "/"));
                }

            }


        } finally {
            FileUtils.deleteFile(dexJar);
            FileUtils.deleteFile(zipExtractTempDir);
        }

    }

    private File modifyClassFiles(DexConfig dexConfig) throws IOException {
        final HashMultimap<String, List<? extends Method>> modifiedMethods = dexConfig.getShellMethods();
        final File classJar = extractClassJar();

        final ZipMap zipMap = ZipMap.from(classJar.toPath());
        final ZipSource zipSource = new ZipSource(zipMap);
        final File outClassJar = getOutClassJar();
        if (outClassJar.exists()) FileUtils.deleteFile(outClassJar);

        try (
                final ZipRepo inZip = new ZipRepo(zipMap);
                final ZipArchive outZip = new ZipArchive(outClassJar.toPath());
        ) {
            for (Map.Entry<String, Entry> entry : inZip.getEntries().entrySet()) {
                if (entry.getValue().isDirectory()) {//不需要目录
                    continue;
                }

                final String name = entry.getKey();
                if (name.endsWith(".class")) {
                    String type = "L" + name.substring(0, name.length() - ".class".length()) + ";";
                    if (modifiedMethods.containsKey(type)) {
                        final InputStream inClass = inZip.getInputStream(name);
                        final Set<List<? extends Method>> methods = modifiedMethods.get(type);
                        //去除首尾L;
                        final int classIdx = dexConfig.getOffsetFromClassName(type.substring(1, type.length() - 1));
                        //生成新的class文件
                        final byte[] newClassBytes = modifyClass(inClass, methods, dexConfig.getRegisterNativesClassName(), dexConfig.getRegisterNativesMethodName(), classIdx);

                        //添加进jar中
                        outZip.add(new BytesSource(newClassBytes, name, Deflater.DEFAULT_COMPRESSION));
                        continue;
                    }
                }
                //不需要修改的文件
                zipSource.select(name, name);
            }
            outZip.add(zipSource);

            //生成NativeUtil,添加进jar
            final byte[] bytes = AsmUtils.genCfNativeUtil(dexConfig.getRegisterNativesClassName(),
                    BuildNativeLib.NMMP_NAME,
                    Collections.singletonList(dexConfig.getRegisterNativesMethodName()));
            outZip.add(new BytesSource(bytes, dexConfig.getRegisterNativesClassName() + ".class", Deflater.DEFAULT_COMPRESSION));
        }
        return outClassJar;
    }

    private static byte[] modifyClass(InputStream inClass,
                                      Set<List<? extends Method>> convertedMethods,
                                      String clsName,
                                      String methodName,
                                      int classIdx) throws IOException {
        Map<AsmMethod, List<AsmMethod>> myMethods = new HashMap<>();
        for (List<? extends Method> methods : convertedMethods) {
            if (methods.isEmpty()) {
                throw new IllegalStateException();
            }
            final Method method = methods.get(0);
            final String sig = MyMethodUtil.getMethodSignature(method.getParameterTypes(), method.getReturnType());
            final AsmMethod asmMethod = new AsmMethod(method.getAccessFlags(), method.getName(), sig);

            if (methods.size() == 1) {
                myMethods.put(asmMethod, Collections.singletonList(asmMethod));
            } else if (methods.size() == 2) {
                final Method method1 = methods.get(1);
                final String sig1 = MyMethodUtil.getMethodSignature(method1.getParameterTypes(), method1.getReturnType());
                final AsmMethod asmMethod1 = new AsmMethod(method1.getAccessFlags(), method1.getName(), sig1);

                myMethods.put(asmMethod, Arrays.asList(asmMethod, asmMethod1));
            }
        }
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final MethodToNativeVisitor methodToNativeVisitor = new MethodToNativeVisitor(Opcodes.ASM9, cw, myMethods);

        final InjectStaticBlockVisitor visitor = new InjectStaticBlockVisitor(Opcodes.ASM9, methodToNativeVisitor,
                clsName,
                methodName,
                classIdx);
        final ClassReader cr = new ClassReader(inClass);
        cr.accept(visitor, 0);
        return cw.toByteArray();
    }

    private static List<String> getAbis() throws IOException {
        return Arrays.asList(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64");
    }


    private File extractClassJar() throws IOException {
        final File file = new File(aarFolders.getTempDir(), "classes.jar");
        if (file.exists()) {
            return file;
        }

        try (
                ZipArchive zipArchive = new ZipArchive(aarFolders.getAar().toPath());
                final InputStream in = zipArchive.getInputStream("classes.jar");
                FileOutputStream out = new FileOutputStream(file);
        ) {
            if (in == null) {
                throw new IllegalStateException("No classes.jar");
            }
            FileUtils.copyStream(in, out);
        }
        return file;
    }

    private File getOutClassJar() throws IOException {
        return new File(aarFolders.getTempDir(), "new_classes.jar");
    }

    //d8 --release --output <file> <input-files>
    //可能还需要指定--lib或者--classpath参数用于desugaring
    private File classesJarToDexJar() throws IOException {
        final File classJar = extractClassJar();
        final File convertedDexJar = aarFolders.getConvertedDexJar();
        D8.main(new String[]{
                "--release",
//                "--lib",
//                libpath,
                "--output",
                convertedDexJar.getAbsolutePath(),
                classJar.getAbsolutePath()
        });
        return convertedDexJar;
    }

    private static File getClassesDex(File dexJar, File zipExtractDir) throws IOException {
        if (!zipExtractDir.exists()) zipExtractDir.mkdirs();
        final File file = new File(zipExtractDir, "classes.dex");
        try (
                final ZipArchive zipArchive = new ZipArchive(dexJar.toPath());
                final InputStream dex = zipArchive.getInputStream("classes.dex");
                final FileOutputStream out = new FileOutputStream(file);
        ) {
            if (dex == null) {
                throw new IllegalStateException("No classes.dex");
            }
            FileUtils.copyStream(dex, out);
        }
        return file;
    }


    public static class Builder {
        private final AarFolders aarFolders;
        private InstructionRewriter instructionRewriter;
        private ClassAndMethodFilter filter;
        private ClassAnalyzer classAnalyzer;


        public Builder(AarFolders aarFolders) {
            this.aarFolders = aarFolders;
        }

        public Builder setInstructionRewriter(InstructionRewriter instructionRewriter) {
            this.instructionRewriter = instructionRewriter;
            return this;
        }


        public Builder setFilter(ClassAndMethodFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setClassAnalyzer(ClassAnalyzer classAnalyzer) {
            this.classAnalyzer = classAnalyzer;
            return this;
        }

        public AarProtect build() {
            if (instructionRewriter == null) {
                throw new RuntimeException("instructionRewriter == null");
            }
            if (classAnalyzer == null) {
                throw new RuntimeException("classAnalyzer==null");
            }
            return new AarProtect(aarFolders, instructionRewriter, filter, classAnalyzer);
        }
    }
}
