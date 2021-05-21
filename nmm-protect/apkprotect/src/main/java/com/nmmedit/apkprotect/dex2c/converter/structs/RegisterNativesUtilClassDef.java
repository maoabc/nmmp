package com.nmmedit.apkprotect.dex2c.converter.structs;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 每个类调用的静态初始化方法,一般情况一个classes.dex对应一个注册方法
 * 需要把它放在主classes.dex里
 */
public class RegisterNativesUtilClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull
    private final String type;
    private final List<String> nativeMethodNames;

    public RegisterNativesUtilClassDef(@Nonnull String type, List<String> nativeMethodNames) {
        this.type = type;
        this.nativeMethodNames = nativeMethodNames;
    }

    @Nonnull
    @Override
    public String getType() {
        return type;
    }


    @Override
    public int getAccessFlags() {
        return AccessFlags.PUBLIC.getValue();
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return "Ljava/lang/Object;";
    }

    @Nonnull
    @Override
    public List<String> getInterfaces() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return null;
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getFields() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        final ArrayList<Method> methods = new ArrayList<>();
        methods.add(new EmptyConstructorMethod(type,"Ljava/lang/Object;"));
        for (String methodName : nativeMethodNames) {
            methods.add(new NativeMethod(type, methodName));
        }
        return methods;
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        //virtualMethods为空,总方法只需要返回directMethods就行
        return getDirectMethods();
    }



    private static class NativeMethod extends BaseMethodReference implements Method {

        @Nonnull
        private final String type;

        private final String methodName;

        public NativeMethod(@Nonnull String type, String methodName) {
            this.type = type;
            this.methodName = methodName;
        }

        @Nonnull
        @Override
        public List<? extends MethodParameter> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public int getAccessFlags() {
            return AccessFlags.NATIVE.getValue()
                    | AccessFlags.STATIC.getValue()
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
            return null;
        }

        @Nonnull
        @Override
        public String getDefiningClass() {
            return type;
        }

        @Nonnull
        @Override
        public String getName() {
            return methodName;
        }

        @Nonnull
        @Override
        public List<? extends CharSequence> getParameterTypes() {
            return Collections.singletonList("I");
        }

        @Nonnull
        @Override
        public String getReturnType() {
            return "V";
        }
    }

}
