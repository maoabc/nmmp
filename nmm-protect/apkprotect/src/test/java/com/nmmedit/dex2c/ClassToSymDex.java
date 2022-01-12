package com.nmmedit.dex2c;

import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 收集方法代码,把它们放入全新的dex中,改变class及其他各种索引让人难以简单恢复原本代码，
 * 就算逆向出指令流也需要重新合并多个dex
 */
public class ClassToSymDex extends BaseTypeReference implements ClassDef {
    private final ClassDef classDef;

    private final ClassAndMethodFilter filter;

    public ClassToSymDex(ClassDef classDef, ClassAndMethodFilter filter) {
        this.classDef = classDef;
        this.filter = filter;
    }

    @Nonnull
    @Override
    public String getType() {
        return classDef.getType();
    }

    @Override
    public int getAccessFlags() {
        return classDef.getAccessFlags();
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return classDef.getSuperclass();
    }

    @Nonnull
    @Override
    public List<String> getInterfaces() {
        return classDef.getInterfaces();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        //忽略
        return null;
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return classDef.getAnnotations();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return classDef.getStaticFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return classDef.getInstanceFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getFields() {
        return classDef.getFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        Iterable<? extends Method> directMethods = classDef.getDirectMethods();
        return filterMethods(directMethods);
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        Iterable<? extends Method> virtualMethods = classDef.getVirtualMethods();
        return filterMethods(virtualMethods);
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        Iterable<? extends Method> methods = classDef.getMethods();
        return filterMethods(methods);
    }

    //过滤掉不转换的方法
    private Iterable<? extends Method> filterMethods(Iterable<? extends Method> methods) {
        ArrayList<Method> newMethods = new ArrayList<>();
        for (Method method : methods) {
            if (filter.acceptMethod(method)) {
                newMethods.add(method);
            }
        }
        return newMethods;
    }
}