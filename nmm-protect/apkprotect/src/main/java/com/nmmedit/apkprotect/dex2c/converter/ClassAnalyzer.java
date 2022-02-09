package com.nmmedit.apkprotect.dex2c.converter;

import com.google.common.collect.Maps;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

//目前用于分析接口静态域，后面可能其他用途
//需要添加多个dex文件,以便完整分析class
public class ClassAnalyzer {
    private final Map<String, ClassDef> allClasses = Maps.newHashMap();

    private boolean hasJna;

    private int minSdk = 21;

    public ClassAnalyzer() {
    }

    public void setMinSdk(int minSdk) {
        this.minSdk = minSdk;
    }

    public void loadDexFile(@Nonnull File dexFile) throws IOException {
        loadDexFile(DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new BufferedInputStream(new FileInputStream(dexFile))));
    }

    public void loadDexFile(@Nonnull DexBackedDexFile dexFile) {
        for (DexBackedClassDef classDef : dexFile.getClasses()) {
            allClasses.put(classDef.getType(), classDef);
        }
        //jna这个类有不少本地方法,不会被混淆,所以通过它是否存在来判断是否使用了jna
        //没发现jna相关类时,每次加载外部dex时判断,如果已经找到则不用再判断
        if(!hasJna) {
            hasJna = allClasses.containsKey("Lcom/sun/jna/Native;");
        }
    }

    public boolean hasJnaLib() {
        return hasJna;
    }

    // android6下直接通过jni调用jna方法会直接崩溃,所以需要判断是否有调用jna方法的指令.issue #31
    // 遍历方法字节码,判断是否有直接调用jna方法的指令,如果有返回true
    public boolean hasCallJnaMethod(@Nonnull Method method) {
        if (!hasJnaLib()) {
            return false;
        }
        final MethodImplementation implementation = method.getImplementation();
        if (implementation == null) {
            return false;
        }
        for (Instruction instruction : implementation.getInstructions()) {
            switch (instruction.getOpcode()) {
                //jna调用用的是调用接口方法,所以只需要检测这两个指令
                case INVOKE_INTERFACE:
                case INVOKE_INTERFACE_RANGE:
                    MethodReference methodReference = (MethodReference) ((ReferenceInstruction) instruction).getReference();
                    //如果调用的方法所在class实现接口中包含jna的Library则表示有直接调用jna的指令
                    if (matchInterface(allClasses.get(methodReference.getDefiningClass()), "Lcom/sun/jna/Library;")) {
                        return true;
                    }
            }
        }
        return false;
    }

    //判断class是否实现某个接口
    private boolean matchInterface(ClassDef classDef, String interfaceType) {
        if (classDef == null) {
            return false;
        }
        for (String defInterface : classDef.getInterfaces()) {
            if (defInterface.equals(interfaceType)) {
                return true;
            }
            //再查找接口
            final ClassDef ifClass = allClasses.get(defInterface);
            return matchInterface(ifClass, interfaceType);
        }
        return false;
    }

    //处理接口中静态域无法通过子类获得值问题
    public FieldReference getDirectFieldRef(FieldReference reference) {
        final ClassDef classDef = allClasses.get(reference.getDefiningClass());
        if (classDef == null) {//不再当前dex中或者在系统库
            return null;
        }

        final String fieldName = reference.getName();
        final String fieldType = reference.getType();

        final ClassDef newClassDef = findFieldDefiningClass(classDef, fieldName, fieldType);
        if (newClassDef != null) {
            return new ImmutableFieldReference(newClassDef.getType(), fieldName, fieldType);
        }


        return null;
    }

    private ClassDef findFieldDefiningClass(ClassDef classDef, String fieldName, String fieldType) {
        if (classDef == null) {
            return null;
        }
        //查找当前类的静态域，如果名称和类型匹配，则返回对应的classDef
        for (Field field : classDef.getStaticFields()) {
            if (field.getName().equals(fieldName) && field.getType().equals(fieldType)) {
                return classDef;
            }
        }
        //查找接口对应的classDef
        for (String defInterface : classDef.getInterfaces()) {
            final ClassDef definingClass = findFieldDefiningClass(allClasses.get(defInterface), fieldName, fieldType);
            if (definingClass != null) {
                return definingClass;
            }
        }
        return null;
    }

    @Nullable
    public MethodReference findDirectMethod(@Nonnull MethodReference method) {
        if (minSdk < 23) {
            final ClassDef classDef = allClasses.get(method.getDefiningClass());
            if (classDef == null) {
                return null;
            }
            //从父类的direct method中查找名称和签名相同的方法
            final ClassDef superClass = allClasses.get(classDef.getSuperclass());
            return findDirectMethod(superClass,
                    method.getName(),
                    method.getParameterTypes(),
                    method.getReturnType());
        }
        return null;

    }

    @Nullable
    public ClassDef getClassDef(@Nonnull String className) {
        return allClasses.get(className);
    }

    //先查找当前类的direct method,找不到则查找超类的direct method
    @Nullable
    private MethodReference findDirectMethod(@Nonnull ClassDef thisClass,
                                             @Nonnull String name,
                                             @Nonnull List<? extends CharSequence> parameterTypes,
                                             @Nonnull String returnType
    ) {
        for (ClassDef classDef = thisClass; classDef != null; classDef = allClasses.get(classDef.getSuperclass())) {
            for (Method directMethod : classDef.getDirectMethods()) {
                //方法名及方法签名完全相等
                if (directMethod.getName().equals(name) &&
                        directMethod.getParameterTypes().equals(parameterTypes) &&
                        directMethod.getReturnType().equals(returnType)) {
                    return directMethod;
                }
            }
        }
        return null;
    }

    // 在当前类中查找方法,如果找不到则查找父类
    @Nullable
    public MethodReference findMethod(@Nonnull ClassDef thisClass,
                                      @Nonnull String name,
                                      @Nonnull List<? extends CharSequence> parameterTypes,
                                      @Nonnull String returnType
    ) {
        for (ClassDef classDef = thisClass; classDef != null; classDef = allClasses.get(classDef.getSuperclass())) {
            for (Method method : classDef.getMethods()) {
                //方法名及方法签名完全相等
                if (method.getName().equals(name) &&
                        method.getParameterTypes().equals(parameterTypes) &&
                        method.getReturnType().equals(returnType)) {
                    return method;
                }
            }
        }
        return null;
    }
}