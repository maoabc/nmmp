package com.nmmedit.protect;

import com.nmmedit.apkprotect.aar.AarFolders;
import com.nmmedit.apkprotect.aar.AarProtect;
import com.nmmedit.apkprotect.deobfus.MappingReader;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter;
import com.nmmedit.apkprotect.dex2c.filters.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class AarMain {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("No Input aar.");
            System.err.println("<inAar> [<convertRuleFile> mapping.txt]");
            return;
        }
        final File aar = new File(args[0]);
        final File outDir = new File(aar.getParentFile(), "build");

        ClassAndMethodFilter filterConfig = new BasicKeepConfig();
        final SimpleRules simpleRules = new SimpleRules();
        if (args.length > 1) {
            simpleRules.parse(new InputStreamReader(new FileInputStream(args[1]), StandardCharsets.UTF_8));
        } else {
            //all classes
            simpleRules.parse(new StringReader("class *"));
        }

        if (args.length > 2) {
            final MappingReader mappingReader = new MappingReader(new File(args[2]));
            filterConfig = new ProguardMappingConfig(filterConfig, mappingReader, simpleRules);
        } else {
            filterConfig = new SimpleConvertConfig(new BasicKeepConfig(), simpleRules);
        }

        final ClassAnalyzer classAnalyzer = new ClassAnalyzer();
        //todo 可能需要加载某些厂商私有的sdk


        final AarFolders aarFolders = new AarFolders(aar, outDir);

        final AarProtect aarProtect = new AarProtect.Builder(aarFolders)
                .setInstructionRewriter(new RandomInstructionRewriter())
                .setFilter(filterConfig)
                .setClassAnalyzer(classAnalyzer)
                .build();
        aarProtect.run();
    }
}
