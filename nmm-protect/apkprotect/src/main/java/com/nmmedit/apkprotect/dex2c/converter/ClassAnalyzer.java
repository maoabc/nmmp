package com.nmmedit.apkprotect.dex2c.converter;

import com.google.common.collect.Maps;
import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.FieldReference;

import javax.annotation.Nonnull;
import java.util.Map;

//目前用于分析接口静态域，后面可能其他用途
public class ClassAnalyzer {
    private final Map<String, ClassDef> originClasses = Maps.newHashMap();

    public ClassAnalyzer(@Nonnull DexBackedDexFile dexFile) {

        for (DexBackedClassDef classDef : dexFile.getClasses()) {
            originClasses.put(classDef.getType(), classDef);
        }
    }

    //处理接口中静态域无法通过子类获得值问题
    public FieldReference getDirectFieldRef(FieldReference reference) {
        final ClassDef classDef = originClasses.get(reference.getDefiningClass());
        if (classDef == null) {//不再当前dex中或者在系统库
            return null;
        }

        final String fieldName = reference.getName();
        final String fieldType = reference.getType();

        final ClassDef newClassDef = findFieldDefiningClass(classDef, fieldName, fieldType);
        if (newClassDef != null) {
            return new BaseFieldReference() {
                @Nonnull
                @Override
                public String getDefiningClass() {
                    return newClassDef.getType();
                }

                @Nonnull
                @Override
                public String getName() {
                    return fieldName;
                }

                @Nonnull
                @Override
                public String getType() {
                    return fieldType;
                }
            };
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
            final ClassDef definingClass = findFieldDefiningClass(originClasses.get(defInterface), fieldName, fieldType);
            if (definingClass != null) {
                return definingClass;
            }
        }
        return null;
    }
}