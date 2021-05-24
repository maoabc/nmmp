package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.google.common.collect.Iterables;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.base.reference.BaseStringReference;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.*;

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
 */
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
            for (Method method : baseAppClassDef.getDirectMethods()) {
                if (method.getName().equals("<clinit>")) {
                    methods.add(new LoadLibStaticBlockMethod(method, type, libName));
                } else {
                    methods.add(method);
                }
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

    private static class LoadLibStaticBlockMethod extends BaseMethodReference implements Method {

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
            implementation.addInstruction(0, new BuilderInstruction21c(Opcode.CONST_STRING, 0, new BaseStringReference() {
                @Nonnull
                @Override
                public String getString() {
                    return libName;
                }
            }));
            implementation.addInstruction(1, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1,
                    0, 0, 0, 0, 0,
                    new BaseMethodReference() {
                        @Nonnull
                        @Override
                        public String getDefiningClass() {
                            return "Ljava/lang/System;";
                        }

                        @Nonnull
                        @Override
                        public String getName() {
                            return "loadLibrary";
                        }

                        @Nonnull
                        @Override
                        public List<? extends CharSequence> getParameterTypes() {
                            return Collections.singletonList("Ljava/lang/String;");
                        }

                        @Nonnull
                        @Override
                        public String getReturnType() {
                            return "V";
                        }
                    }
            ));
        }
    }

}
