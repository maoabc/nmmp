package com.nmmedit.apkprotect.dex2c.filters;

import junit.framework.TestCase;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedMethod;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringReader;

public class SimpleRulesTest extends TestCase {

    public void testParse() throws IOException {
        final SimpleRules ruleReader = new SimpleRules();
        final DexBackedDexFile dexFile = DexBackedDexFile.fromInputStream(Opcodes.getDefault(),
                new BufferedInputStream(SimpleRulesTest.class.getResourceAsStream("/classes2.dex")));
        ruleReader.parse(new StringReader(
                "class * extends android.app.Activity\n" +
                        "class * implements java.io.Serializable\n" +
                        "class *\n" +
                        "class * extends java.util.ArrayList {\n" +
                        "if*;\n" +
                        "}"));
        for (DexBackedClassDef classDef : dexFile.getClasses()) {
            if (ruleReader.matchClass(classDef.getType(), classDef.getSuperclass(), classDef.getInterfaces())) {
                System.out.println(classDef);
                for (DexBackedMethod method : classDef.getMethods()) {
                    if (ruleReader.matchMethod(method.getName())) {
                        System.out.println(classDef + "   " + method);
                    }
                }

            }
        }

    }
}