package com.nmmedit.apkprotect.dex2c.filters;

import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.nmmedit.apkprotect.deobfus.MappingProcessor;
import com.nmmedit.apkprotect.deobfus.MappingReader;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 读取proguard的mapping.txt文件,根据它得到class和方法名混淆前后映射关系,然后再执行过滤规则
 */

public class ProguardMappingConfig implements ClassAndMethodFilter, MappingProcessor {
    private final ClassAndMethodFilter filter;
    private final Map<String, String> newTypeOldTypeMap = Maps.newHashMap();
    private final Map<String, String> oldTypeNewTypeMap = Maps.newHashMap();
    private final Set<MethodMapping> methodSet = Sets.newHashSet();
    private final HashMultimap<MethodReference, MethodReference> newMethodRefMap = HashMultimap.create();
    private final SimpleRules simpleRules;

    public ProguardMappingConfig(ClassAndMethodFilter filter,
                                 MappingReader mappingReader,
                                 SimpleRules simpleRules) throws IOException {
        this.filter = filter;
        this.simpleRules = simpleRules;
        mappingReader.parse(this);

        for (MethodMapping methodMapping : methodSet) {
            final List<String> args = parseArgs(methodMapping.args);
            final ImmutableMethodReference oldMethodRef = new ImmutableMethodReference(
                    javaType2jvm(methodMapping.className),
                    methodMapping.methodName, args,
                    javaType2jvm(methodMapping.returnType));

            final List<String> newArgs = getNewArgs(args);
            String newRetType = oldTypeNewTypeMap.get(methodMapping.returnType);
            if (newRetType == null) {
                newRetType = methodMapping.returnType;
            }
            final ImmutableMethodReference newMethodRef = new ImmutableMethodReference(javaType2jvm(
                    methodMapping.newClassName),
                    methodMapping.newMethodName, newArgs,
                    javaType2jvm(newRetType));
            newMethodRefMap.put(newMethodRef, oldMethodRef);
        }
    }

    private List<String> getNewArgs(List<String> args) {
        final ArrayList<String> newArgs = new ArrayList<>();
        for (String arg : args) {
            final String newType = oldTypeNewTypeMap.get(arg);
            newArgs.add(newType == null ? arg : newType);
        }
        return newArgs;
    }


    @Override
    public final boolean acceptClass(ClassDef classDef) {
        //先处理上游的过滤规则,如果上游不通过则直接返回不再处理,如果上游通过再处理当前的过滤规则
        if (filter != null && !filter.acceptClass(classDef)) {
            return false;
        }
        //得到混淆之前的className
        final String oldType = getOriginClassType(classDef.getType());
        if (oldType == null) {
            return false;
        }
        final ArrayList<String> ifacs = new ArrayList<>();
        for (String ifac : classDef.getInterfaces()) {
            ifacs.add(getOriginClassType(ifac));
        }

        return simpleRules != null && simpleRules.matchClass(
                oldType,
                getOriginClassType(classDef.getSuperclass()),
                ifacs);
    }


    private String getOriginClassType(String type) {
        final String oldType = newTypeOldTypeMap.get(type);
        if (oldType == null) {
            return type;
        }
        return oldType;
    }

    @Override
    public final boolean acceptMethod(Method method) {
        if (filter != null && !filter.acceptMethod(method)) {
            return false;
        }
        final String oldType = getOriginClassType(method.getDefiningClass());
        if (oldType == null) {
            return false;
        }
        final Set<MethodReference> oldMethodRefSet = newMethodRefMap.get(method);


        for (MethodReference reference : oldMethodRefSet) {
            if (oldType.equals(reference.getDefiningClass())) {
                if (simpleRules != null && simpleRules.matchMethod(reference.getName())) {
                    return true;
                }
            }
        }

        return simpleRules != null && simpleRules.matchMethod(method.getName());
    }

    private static String classNameToType(String className) {
        return "L" + className.replace('.', '/') + ";";
    }

    @Override
    public final void processClassMapping(String className, String newClassName) {
        newTypeOldTypeMap.put(classNameToType(newClassName), classNameToType(className));
        oldTypeNewTypeMap.put(classNameToType(className), classNameToType(newClassName));
    }

    @Override
    public final void processFieldMapping(String className, String fieldType, String fieldName, String newClassName, String newFieldName) {

    }

    @Nonnull
    private static String javaType2jvm(@Nonnull String type) {
        switch (type.trim()) {
            case "boolean":
                return "Z";
            case "byte":
                return "B";
            case "char":
                return "C";
            case "short":
                return "S";
            case "int":
                return "I";
            case "float":
                return "F";
            case "long":
                return "J";
            case "double":
                return "D";
            case "void":
                return "V";
            default:
                int i = type.indexOf('[');
                if (i != -1) {
                    String t = type.substring(0, i);
                    StringBuilder arr = new StringBuilder("[");
                    while ((i = type.indexOf('[', i + 1)) != -1) {
                        arr.append('[');
                    }
                    arr.append(javaType2jvm(t));
                    return arr.toString();
                } else {
                    return classNameToType(type);
                }

        }
    }

    @Nonnull
    private List<String> parseArgs(String methodArgs) {
        final ArrayList<String> args = new ArrayList<>();
        if ("".equals(methodArgs)) {
            return args;
        }
        final String[] split = methodArgs.split(",");
        for (String type : split) {
            args.add(javaType2jvm(type));
        }
        return args;
    }

    @Override
    public final void processMethodMapping(String className,
                                           int firstLineNumber, int lastLineNumber,
                                           String methodReturnType,
                                           String methodName,
                                           String methodArguments,
                                           String newClassName,
                                           int newFirstLineNumber, int newLastLineNumber,
                                           String newMethodName) {
        final MethodMapping mapping = new MethodMapping(className, methodName, newClassName, newMethodName, methodArguments, methodReturnType);
        methodSet.add(mapping);
    }

    private static class MethodMapping {
        private final String className;
        private final String methodName;

        private final String newClassName;
        private final String newMethodName;
        private final String args;
        private final String returnType;

        public MethodMapping(String className, String methodName,
                             String newClassName, String newMethodName,
                             String args, String returnType) {
            this.className = className;
            this.methodName = methodName;
            this.newClassName = newClassName;
            this.newMethodName = newMethodName;
            this.args = args;
            this.returnType = returnType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodMapping that = (MethodMapping) o;

            if (!className.equals(that.className)) return false;
            if (!methodName.equals(that.methodName)) return false;
            if (!newClassName.equals(that.newClassName)) return false;
            if (!newMethodName.equals(that.newMethodName)) return false;
            if (!args.equals(that.args)) return false;
            return returnType.equals(that.returnType);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + newClassName.hashCode();
            result = 31 * result + newMethodName.hashCode();
            result = 31 * result + args.hashCode();
            result = 31 * result + returnType.hashCode();
            return result;
        }
    }
}