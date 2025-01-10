package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.HiddenApiRestriction;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

//静态初始化方法
public class LoadLibStaticBlockMethod extends BaseMethodReference implements Method {

    private final Method method;

    @Nonnull
    private final String definingClass;

    @Nonnull
    private final String libName;


    public LoadLibStaticBlockMethod(@Nullable Method method, @Nonnull String definingClass, @Nonnull String libName) {
        this.method = method;
        this.definingClass = definingClass;
        this.libName = libName;
    }

    @Nonnull
    @Override
    public String getDefiningClass() {
        return definingClass;
    }

    @Nonnull
    @Override
    public String getName() {
        return "<clinit>";
    }

    @Nonnull
    @Override
    public List<? extends CharSequence> getParameterTypes() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<? extends MethodParameter> getParameters() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String getReturnType() {
        return "V";
    }


    @Override
    public int getAccessFlags() {
        return AccessFlags.CONSTRUCTOR.getValue()
                | AccessFlags.STATIC.getValue();
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
        return Collections.emptySet();
    }

    @Override
    public MethodImplementation getImplementation() {
        final MutableMethodImplementation implementation;
        if (method != null && method.getImplementation() != null) {
            implementation = new MutableMethodImplementation(method.getImplementation()) {
                @Override
                public int getRegisterCount() {//起码需要一个寄存器
                    return Math.max(1, super.getRegisterCount());
                }
            };
            injectCallLoadLibInsns(implementation);
        } else {//原来不存在,则需要添加返回指令
            implementation = new MutableMethodImplementation(1);
            injectCallLoadLibInsns(implementation);
            implementation.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        }
        return implementation;
    }

    private void injectCallLoadLibInsns(MutableMethodImplementation implementation) {
        implementation.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, 0,
                new ImmutableStringReference(libName)));
        implementation.addInstruction(1, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1,
                0, 0, 0, 0, 0,
                new ImmutableMethodReference(
                        "Ljava/lang/System;",
                        "loadLibrary",
                        Collections.singletonList("Ljava/lang/String;"),
                        "V"
                )
        ));
    }
}
