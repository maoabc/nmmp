package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.google.common.collect.Iterables;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * android.app.Application直接或者间接子类, 该类只包含一个静态块用于加载so
 * public class MyApplication extends android.app.Application{
 * static {
 * System.loadLibrary(libname);
 * }
 * }
 * 如果原来自定义了Application,则该类成为原来Application的父类,原来Application的父类成为该类的父类：
 * BaseApp <-- android.app.Application 变为 BaseApp <-- MyApplication <-- android.app.Application
 * 如果原来没自定义Application,则需要修改AndroidManifest.xml把该类设置为当前应用的Application
 * <p>
 * <p>
 * 没必要在application处理本地库加载,直接在NativeUtil里就可以
 */

@Deprecated
public class LoadLibClassDef extends BaseTypeReference implements ClassDef {
    private ClassDef baseAppClassDef;
    @Nonnull
    private final String type;
    @Nonnull
    private final String libName;


    public LoadLibClassDef(@Nullable ClassDef baseAppClassDef, @Nonnull String type, @Nonnull String libName) {
        this.baseAppClassDef = baseAppClassDef;
        this.type = type;
        this.libName = libName;
    }

    @Nonnull
    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getAccessFlags() {
        return baseAppClassDef != null ?
                baseAppClassDef.getAccessFlags()
                : AccessFlags.PUBLIC.getValue();
    }

    @Override
    public String getSuperclass() {
        return baseAppClassDef != null ?
                baseAppClassDef.getSuperclass()
                : "Landroid/app/Application;";
    }

    @Nonnull
    @Override
    public List<String> getInterfaces() {
        return baseAppClassDef != null ?
                baseAppClassDef.getInterfaces()
                : Collections.emptyList();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return null;
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return baseAppClassDef != null ?
                baseAppClassDef.getAnnotations()
                : Collections.emptySet();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return baseAppClassDef != null ?
                baseAppClassDef.getStaticFields()
                : Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return baseAppClassDef != null ?
                baseAppClassDef.getInstanceFields()
                : Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getFields() {
        return baseAppClassDef != null ?
                baseAppClassDef.getFields()
                : Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        if (baseAppClassDef != null) {//原来方法上添加指令
            final ArrayList<Method> methods = new ArrayList<>();
            boolean hasStaticBlock = false;
            for (Method method : baseAppClassDef.getDirectMethods()) {
                if (method.getName().equals("<clinit>")) {
                    methods.add(new LoadLibStaticBlockMethod(method, type, libName));
                    hasStaticBlock = true;
                } else {
                    methods.add(method);
                }
            }
            //自定义application没有静态初始化方法，给它添加静态初始化方法
            if (!hasStaticBlock) {
                methods.add(new LoadLibStaticBlockMethod(null, type, libName));
            }

            return methods;
        }

        return Arrays.asList(new LoadLibStaticBlockMethod(null, type, libName)
                , new EmptyConstructorMethod(type, getSuperclass())
        );
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return baseAppClassDef != null ?
                baseAppClassDef.getVirtualMethods()
                : Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        return Iterables.concat(getDirectMethods(), getVirtualMethods());
    }


}
