package com.nmmedit.apkprotect.dex2c.filters;

import com.google.common.collect.HashMultimap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * class * extends android.app.Activity
 * class * implements java.io.Serializable
 * class my.package.AClass
 * class my.package.* { *; }
 * class * extends java.util.ArrayList {
 * if*;
 * }
 * class A {
 * }
 * class B extends A {
 * }
 * class C extends B {
 * }
 * The rule 'class * extends A' only match B
 */
public class SimpleRules {
    private final HashMultimap<ClassRule, MethodRule> convertRules = HashMultimap.create();


    public SimpleRules() {
    }

    public void parse(Reader ruleReader) throws IOException {
        try (BufferedReader reader = new BufferedReader(ruleReader)) {
            ClassRule classRule = null;
            final ArrayList<String> methodNameList = new ArrayList<>();
            boolean methodParsing = false;
            int lineNumb = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ("".equals(line)) {//empty line
                    lineNumb++;
                    continue;
                }
                if (line.startsWith("class")) {
                    final String[] split = line.split(" +");
                    final int length = split.length;
                    if (length < 2) {
                        throw new RemoteException("Error rule " + lineNumb + ": " + line);
                    }
                    String className = split[1];
                    String supperName = "";
                    String interfaceName = "";
                    if (length >= 4) {
                        if ("extends".equals(split[2])) {//class * extends A
                            supperName = split[3];
                        } else if ("implements".equals(split[2])) {//class * implements I
                            interfaceName = split[3];
                        }
                    }
                    classRule = new ClassRule(className, supperName, interfaceName);
                    int mstart;
                    if ((mstart = line.indexOf('{')) != -1) { // my.pkg.A { methodA;methodB;}
                        int mend;
                        if ((mend = line.indexOf('}')) != -1) {
                            final String[] methodNames = line.substring(mstart + 1, mend).trim().split(";");
                            if (methodNames.length == 0) {
                                throw new RemoteException("Error rule " + lineNumb + ": " + line);
                            }
                            for (String name : methodNames) {
                                convertRules.put(classRule, new MethodRule(name));
                            }
                        } else {
                            methodNameList.clear();
                            methodParsing = true;
                        }
                    } else {
                        //any methods
                        convertRules.put(classRule, new MethodRule("*"));
                    }
                } else if (methodParsing) {
                    // my.pkg.A {
                    //   methodA;
                    //   methodB;
                    // }
                    if (line.indexOf('}') != -1) {
                        if (methodNameList.isEmpty()) {
                            throw new RemoteException("Error rule " + lineNumb + ": " + line);
                        }
                        for (String methodName : methodNameList) {
                            if ("".equals(methodName)) {
                                continue;
                            }
                            convertRules.put(classRule, new MethodRule(methodName));
                        }
                        methodParsing = false;
                    } else {
                        methodNameList.add(line.replace(";", ""));
                    }
                } else {
                    throw new RemoteException("Error rule " + lineNumb + ": " + line);
                }

                lineNumb++;
            }
        }
    }

    private Set<MethodRule> methodRules;

    public boolean matchClass(@Nonnull String classType, @Nullable String supperType, @Nonnull List<String> ifacTypes) {
        for (ClassRule rule : convertRules.keySet()) {
            final String typeRegex = toRegex(classNameToType(rule.className));
            if (classType.matches(typeRegex)) {// match classType
                if (!"".equals(rule.supperName)) {//supper name not empty
                    if (supperType != null) {
                        final String type = classNameToType(rule.supperName);
                        if (supperType.equals(type)) {
                            methodRules = convertRules.get(rule);
                            return true;
                        }
                    }
                    continue;
                }
                if (!"".equals(rule.interfaceName)) {//interface name not empty
                    for (String iface : ifacTypes) {
                        if (iface.equals(classNameToType(rule.interfaceName))) {
                            methodRules = convertRules.get(rule);
                            return true;
                        }
                    }
                    continue;
                }
                methodRules = convertRules.get(rule);
                return true;
            }
        }
        methodRules = null;
        return false;
    }

    public boolean matchMethod(String methodName) {
        if (methodRules == null || methodName == null) {
            return false;
        }
        for (MethodRule methodRule : methodRules) {
            if (methodName.matches(toRegex(methodRule.methodName))) {
                return true;
            }
        }
        return false;
    }

    private static String classNameToType(String className) {
        return "L" + className.replace('.', '/') + ";";
    }

    @Nonnull
    private static String toRegex(String s) {
        final StringBuilder sb = new StringBuilder(s.length() + 3);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '*':
                    sb.append('.');
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static class ClassRule {
        @Nonnull
        private final String className;
        //supper class
        @Nonnull
        private final String supperName;
        //interface
        @Nonnull
        private final String interfaceName;

        public ClassRule(@Nonnull String className) {
            this(className, "", "");
        }

        public ClassRule(@Nonnull String className, @Nonnull String supperName, @Nonnull String interfaceName) {
            this.className = className;
            this.supperName = supperName;
            this.interfaceName = interfaceName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClassRule classRule = (ClassRule) o;

            if (!className.equals(classRule.className)) return false;
            if (!supperName.equals(classRule.supperName)) return false;
            return interfaceName.equals(classRule.interfaceName);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + supperName.hashCode();
            result = 31 * result + interfaceName.hashCode();
            return result;
        }
    }

    private static class MethodRule {
        @Nonnull
        private final String methodName;
        // args ?
        // private final List<String> args;

        public MethodRule(@Nonnull String methodName) {
            this.methodName = methodName;
        }
    }
}
