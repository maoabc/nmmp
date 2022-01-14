package com.nmmedit.apkprotect.dex2c.converter;

import com.google.common.collect.Maps;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
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