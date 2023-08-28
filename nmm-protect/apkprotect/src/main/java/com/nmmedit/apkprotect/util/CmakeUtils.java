package com.nmmedit.apkprotect.util;

import com.nmmedit.apkprotect.BuildNativeLib;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.sign.ApkVerifyCodeGenerator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CmakeUtils {

    //根据指令重写规则,重新生成新的opcode
    public static void writeOpcodeHeaderFile(File source, InstructionRewriter instructionRewriter) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(source), StandardCharsets.UTF_8));

        final String collect = bufferedReader.lines().collect(Collectors.joining("\n"));
        final Pattern opcodePattern = Pattern.compile(
                "enum Opcode \\{.*?};",
                Pattern.MULTILINE | Pattern.DOTALL);
        final StringWriter opcodeContent = new StringWriter();
        final StringWriter gotoTableContent = new StringWriter();
        instructionRewriter.generateConfig(opcodeContent, gotoTableContent);
        String headerContent = opcodePattern
                .matcher(collect)
                .replaceAll(String.format("enum Opcode {\n%s};\n", opcodeContent.toString()));

        //根据opcode生成goto表
        final Pattern patternGotoTable = Pattern.compile(
                "_name\\[kNumPackedOpcodes\\] = \\{.*?};",
                Pattern.MULTILINE | Pattern.DOTALL);
        headerContent = patternGotoTable
                .matcher(headerContent)
                .replaceAll(String.format("_name[kNumPackedOpcodes] = {        \\\\\n%s};\n", gotoTableContent));

        try (FileWriter fileWriter = new FileWriter(source)) {
            fileWriter.write(headerContent);
        }
    }

    //读取证书信息,并把公钥写入签名验证文件里,运行时对apk进行签名校验
    private static void writeApkVerifierFile(String packageName, File source, ApkVerifyCodeGenerator apkVerifyCodeGenerator) throws IOException {
        if (apkVerifyCodeGenerator == null) {
            return;
        }
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(source), StandardCharsets.UTF_8));

        final String lines = bufferedReader.lines().collect(Collectors.joining("\n"));
        String dataPlaceHolder = "#define publicKeyPlaceHolder";

        String content = lines.replaceAll(dataPlaceHolder, dataPlaceHolder + apkVerifyCodeGenerator.generate());
        content = content.replaceAll("(#define PACKAGE_NAME) .*\n", "$1 \"" + packageName + "\"\n");

        try (FileWriter fileWriter = new FileWriter(source)) {
            fileWriter.write(content);
        }
    }

    public static void writeCmakeFile(File cmakeTemp, String libName) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(cmakeTemp), StandardCharsets.UTF_8));

        String lines = bufferedReader.lines().collect(Collectors.joining("\n"));
        //定位cmake里的语句,防止替换错误
        String libNameFormat = "set\\(LIBNAME_PLACEHOLDER \"%s\"\\)";

        //替换原本libname
        lines = lines.replaceAll(String.format(libNameFormat, "nmmp"), String.format(libNameFormat, libName));

        try (FileWriter fileWriter = new FileWriter(cmakeTemp)) {
            fileWriter.write(lines);
        }
    }


    public static void generateCSources(File srcDir, InstructionRewriter instructionRewriter) throws IOException {
        final File vmsrcFile = new File(FileUtils.getHomePath(), "tools/vmsrc.zip");
        if (!vmsrcFile.exists()) {
            //警告：如果外部源码存在不会复制内部vmsrc.zip出去，需要删除外部源码文件才能保证vmsrc.zip正确更新
            vmsrcFile.getParentFile().mkdirs();
            //copy vmsrc.zip to external directory
            try (
                    InputStream inputStream = CmakeUtils.class.getResourceAsStream("/vmsrc.zip");
                    final FileOutputStream outputStream = new FileOutputStream(vmsrcFile);
            ) {
                FileUtils.copyStream(inputStream, outputStream);
            }
        }
        final List<File> cSources = ApkUtils.extractFiles(vmsrcFile, ".*", srcDir);

        //处理指令及apk验证,生成新的c文件
        for (File source : cSources) {
            if (source.getName().endsWith("DexOpcodes.h")) {
                //根据指令重写规则重新生成DexOpcodes.h文件
                writeOpcodeHeaderFile(source, instructionRewriter);
            } else if (source.getName().equals("CMakeLists.txt")) {
                //处理cmake里配置的本地库名
                writeCmakeFile(source, BuildNativeLib.NMMP_NAME);
            } else if (source.getName().endsWith("vm.h")) {
                writeRandomResolver(source);
            } else if (source.getName().endsWith("JNIWrapper.h")) {
                writeRandomJNIWrapper(source);
            }
        }
    }

    private static void writeRandomResolver(File source) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(source), StandardCharsets.UTF_8));
        String lines = bufferedReader.lines().collect(Collectors.joining("\n"));
        final Pattern p = Pattern.compile("typedef struct \\{([^}]*?)} vmResolver;", Pattern.MULTILINE | Pattern.DOTALL);
        final Matcher matcherResolver = p.matcher(lines);
        if (matcherResolver.find()) {
            final String body = matcherResolver.group(1);
            //match function pointer
            final Pattern funcPattern = Pattern.compile("([^();]* \\**\\(\\*[a-zA-z0-9]*\\)\\([^();]*\\);)", Pattern.MULTILINE | Pattern.DOTALL);
            final ArrayList<String> funcs = new ArrayList<>();
            final Matcher matcher = funcPattern.matcher(body);
            while (matcher.find()) {
                funcs.add(matcher.group(1));
            }


            try (FileWriter fileWriter = new FileWriter(source)) {
                final String doc = matcherResolver.replaceAll("typedef struct {\n" +
                        randomList(funcs) +
                        "} vmResolver;");
                fileWriter.write(doc);
            }
        }
    }

    private static void writeRandomJNIWrapper(File file) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8));
        String lines = bufferedReader.lines().collect(Collectors.joining("\n"));
        final Pattern p = Pattern.compile("typedef struct \\{([^}]*?)} JNIWrapper;", Pattern.MULTILINE | Pattern.DOTALL);
        final Matcher matcherWrapper = p.matcher(lines);
        if (matcherWrapper.find()) {
            final String body = matcherWrapper.group(1);
            //match function pointer
            final Pattern funcPattern = Pattern.compile("([^();]* \\**\\(\\*[a-zA-z0-9]*\\)\\([^();]*\\);)", Pattern.MULTILINE | Pattern.DOTALL);
            final ArrayList<String> funcs = new ArrayList<>();
            final Matcher matcher = funcPattern.matcher(body);
            while (matcher.find()) {
                funcs.add(matcher.group(1));
            }


            try (FileWriter fileWriter = new FileWriter(file)) {
                final String doc = matcherWrapper.replaceAll("typedef struct {\n" +
                        randomList(funcs) +
                        "} JNIWrapper;");
                fileWriter.write(doc);
            }
        }
    }

    private static String randomList(List<String> list) {
        final StringBuilder sb = new StringBuilder();
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            final Random random = new Random();
            final int idx = random.nextInt(list.size());
            sb.append(list.get(idx));
            sb.append('\n');
            list.remove(idx);
        }
        return sb.toString();
    }
}
