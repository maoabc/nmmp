package com.nmmedit.apkprotect.dex2c.converter.structs;


import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.google.common.collect.Iterators;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class MyClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull
    private final ClassDef classDef;
    @Nonnull
    private final List<? extends Method> directMethods;
    @Nonnull
    private final List<? extends Method> virtualMethods;


    public MyClassDef(@Nonnull ClassDef classDef,
                      @Nonnull List<? extends Method> directMethods,
                      @Nonnull List<? extends Method> virtualMethods) {
        this.classDef = classDef;
        this.directMethods = directMethods;
        this.virtualMethods = virtualMethods;
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
        return directMethods;
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return virtualMethods;
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
//        return Iterables.concat(directMethods, virtualMethods);
        return new AbstractCollection<Method>() {
            @Nonnull
            @Override
            public Iterator<Method> iterator() {
                return Iterators.concat(directMethods.iterator(), virtualMethods.iterator());
            }

            @Override
            public int size() {
                return directMethods.size() + virtualMethods.size();
            }
        };
    }
}