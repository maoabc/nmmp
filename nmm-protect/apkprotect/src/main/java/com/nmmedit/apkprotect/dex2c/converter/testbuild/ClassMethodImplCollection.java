package com.nmmedit.apkprotect.dex2c.converter.testbuild;

import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import com.nmmedit.apkprotect.dex2c.converter.MyMethodUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 收集方法具体代码,把它放入全新的dex中
 * 当前用于测试,实际使用需要把它转换为c代码,
 */
public class ClassMethodImplCollection implements ClassDef {
    private final ClassDef classDef;

    private final StringBuilder codeContent;

    public ClassMethodImplCollection(ClassDef classDef, StringBuilder sb) {
        this.classDef = classDef;
        this.codeContent = sb;
    }

    @Nonnull
    @Override
    public String getType() {
        return classDef.getType();
    }

    @Override
    public int compareTo(@Nonnull CharSequence o) {
        return classDef.compareTo(o);
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
        return convertMethods(directMethods);
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        Iterable<? extends Method> virtualMethods = classDef.getVirtualMethods();
        return convertMethods(virtualMethods);
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        Iterable<? extends Method> methods = classDef.getMethods();
        return convertMethods(methods);
    }

    private Iterable<? extends Method> convertMethods(Iterable<? extends Method> methods) {
        ArrayList<Method> newMethods = new ArrayList<>();
        for (Method method : methods) {
            if (MyMethodUtil.isConstructorOrAbstract(method)) {
                continue;
            }
            newMethods.add(method);
            methodToC(method);
        }
        return newMethods;
    }

    private void methodToC(Method method) {
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null) {
            return;
        }
        int registerCount = implementation.getRegisterCount();
        int parameterRegisterCount = MethodUtil.getParameterRegisterCount(method);
        List<? extends CharSequence> parameterTypes = method.getParameterTypes();

        String code = JniTemp.genJniCode(getType(), method.getName(), parameterTypes, MethodUtil.isStatic(method), registerCount, parameterRegisterCount, method.getReturnType());
        codeContent.append(code);
        codeContent.append('\n');

    }

    @Override
    public int length() {
        return classDef.length();
    }

    @Override
    public char charAt(int index) {
        return classDef.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return classDef.subSequence(start, end);
    }

    @Override
    public void validateReference() throws InvalidReferenceException {
        classDef.validateReference();
    }

    @Nonnull
    @Override
    public String toString() {
        return classDef.toString();
    }
}