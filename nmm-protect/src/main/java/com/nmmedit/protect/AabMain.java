package com.nmmedit.protect;

import com.nmmedit.apkprotect.aab.AabFolders;
import com.nmmedit.apkprotect.aab.AabProtect;
import com.nmmedit.apkprotect.deobfus.MappingReader;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter;
import com.nmmedit.apkprotect.dex2c.filters.BasicKeepConfig;
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import com.nmmedit.apkprotect.dex2c.filters.ProguardMappingConfig;
import com.nmmedit.apkprotect.dex2c.filters.SimpleRules;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class AabMain {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("No Input aab.");
            System.err.println("<inAab> [convertRuleFile]");
            return;
        }
        final File aab = new File(args[0]);
        final File outDir = new File(aab.getParentFile(), "bundleOut");

        ClassAndMethodFilter filterConfig = new BasicKeepConfig();
        final SimpleRules simpleRules = new SimpleRules();
        if (args.length > 1) {
            simpleRules.parse(new InputStreamReader(new FileInputStream(args[1]), StandardCharsets.UTF_8));
        } else {
            //all classes
            simpleRules.parse(new StringReader("class *"));
        }

        final MappingReader mappingReader = new MappingReader(AabProtect.getAabProguardMapping(aab));
        filterConfig = new ProguardMappingConfig(filterConfig, mappingReader, simpleRules);


        final ClassAnalyzer classAnalyzer = new ClassAnalyzer();
        //todo 可能需要加载某些厂商私有的sdk


        final AabFolders aabFolders = new AabFolders(aab, outDir);

        final AabProtect aarProtect = new AabProtect.Builder(aabFolders)
                .setInstructionRewriter(new RandomInstructionRewriter())
                .setFilter(filterConfig)
                .setClassAnalyzer(classAnalyzer)
                .build();
        aarProtect.run();
    }
}
