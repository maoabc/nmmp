package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.android.tools.smali.dexlib2.base.BaseMethodParameter;
import com.android.tools.smali.dexlib2.iface.Annotation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class EmptyAnnotationMethodParameter extends BaseMethodParameter {
    //不用记录参数名称
    @Nullable
    private final String name = null;
    @Nonnull
    private final String type;

    public EmptyAnnotationMethodParameter(@Nonnull String type) {
        this.type = type;
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        //参数annotation总是为空
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public String getType() {
        return type;
    }
}
