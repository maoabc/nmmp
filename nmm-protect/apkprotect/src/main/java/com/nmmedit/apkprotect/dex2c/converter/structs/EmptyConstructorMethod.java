package com.nmmedit.apkprotect.dex2c.converter.structs;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

//默认无参数构造方法

/**
 * .method public constructor <init>()V
 *     .locals 0
 *     invoke-direct {p0}, Landroid/app/Application;-><init>()V
 *     return-void
 * .end method
 */

public class EmptyConstructorMethod extends BaseMethodReference implements Method {

    @Nonnull
    private final String definingClass;

    @Nonnull
    private final String superClass;

    public EmptyConstructorMethod(@Nonnull String definingClass, @Nonnull String superClass) {
        this.definingClass = definingClass;
        this.superClass = superClass;
    }

    @Nonnull
    @Override
    public List<? extends MethodParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public int getAccessFlags() {
        return AccessFlags.CONSTRUCTOR.getValue()
                | AccessFlags.PUBLIC.getValue();
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
        final MutableMethodImplementation implementation = new MutableMethodImplementation(1);
        implementation.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1,
                0, 0, 0, 0, 0, new BaseMethodReference() {
            @Nonnull
            @Override
            public String getDefiningClass() {
                return superClass;
            }

            @Nonnull
            @Override
            public String getName() {
                return "<init>";
            }

            @Nonnull
            @Override
            public List<? extends CharSequence> getParameterTypes() {
                return Collections.emptyList();
            }

            @Nonnull
            @Override
            public String getReturnType() {
                return "V";
            }
        }
        ));
        implementation.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return implementation;
    }

    @Nonnull
    @Override
    public String getDefiningClass() {
        return definingClass;
    }

    @Nonnull
    @Override
    public String getName() {
        return "<init>";
    }

    @Nonnull
    @Override
    public List<? extends CharSequence> getParameterTypes() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String getReturnType() {
        return "V";
    }
}
