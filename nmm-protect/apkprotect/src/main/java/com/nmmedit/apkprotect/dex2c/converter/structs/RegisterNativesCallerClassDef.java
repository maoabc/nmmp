package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.google.common.collect.Iterables;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 给每个处理过的类添加静态块,注册本地方法
 * 1.如果静态初始化方法存在, 在指令开头插入调用指令
 * 2.如果静态初始化方法不存在,则需要添加静态初始化方法然后插入调用指令
 */
public class RegisterNativesCallerClassDef extends BaseTypeReference implements ClassDef {

    public static final String CLINIT_METHOD = "<clinit>";
    private final ClassDef classDef;
    private final int classIdx;
    private final String registerNativeDefiningClass;
    private final String registerNativeMethodName;


    public RegisterNativesCallerClassDef(ClassDef classDef, int classIdx,
                                         String registerNativeDefiningClass, String registerNativeMethodName) {
        this.classDef = classDef;
        this.classIdx = classIdx;
        this.registerNativeDefiningClass = registerNativeDefiningClass;
        this.registerNativeMethodName = registerNativeMethodName;
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
        return classDef.getSourceFile();
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
        final Iterable<? extends Method> directMethods = classDef.getDirectMethods();
        final List<Method> methods = new ArrayList<>();
        boolean handled = false;
        for (Method directMethod : directMethods) {
            if (directMethod.getName().equals(CLINIT_METHOD)
                    && directMethod.getParameters().isEmpty()
                    && "V".equals(directMethod.getReturnType())
            ) {//保证是static <clinit>()V方法. #82
                methods.add(new RegisterNativesStaticBlock(directMethod));
                handled = true;
            } else {
                methods.add(directMethod);
            }
        }
        //需要添加静态初始化方法
        if (!handled) {
            methods.add(new RegisterNativesStaticBlock(null));
        }


        return methods;
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return classDef.getVirtualMethods();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        return Iterables.concat(getDirectMethods(), getVirtualMethods());
    }


    @Override
    public void validateReference() throws InvalidReferenceException {
        classDef.validateReference();
    }

    private class RegisterNativesStaticBlock extends BaseMethodReference implements Method {

        private final Method method;

        public RegisterNativesStaticBlock(Method method) {
            this.method = method;
        }

        @Nonnull
        @Override
        public String getDefiningClass() {
            return method == null ? getType() : method.getDefiningClass();
        }

        @Nonnull
        @Override
        public String getName() {
            return CLINIT_METHOD;
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
            return method == null ? Collections.emptySet() : method.getAnnotations();
        }

        @Nonnull
        @Override
        public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
            return method == null ? Collections.emptySet() : method.getHiddenApiRestrictions();
        }

        @Override
        public MethodImplementation getImplementation() {
            if (method == null) {
                final MutableMethodImplementation newImpl = new MutableMethodImplementation(1);
                final List<BuilderInstruction> insns = getCallRegisterNativesMethod();
                for (BuilderInstruction insn : insns) {
                    newImpl.addInstruction(insn);
                }

                newImpl.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
                return newImpl;
            } else {
                final MethodImplementation implementation = method.getImplementation();
                if (implementation == null) {//无方法体static{},肯定出错不用考虑后续
                    throw new RuntimeException("static block methodImpl == null");
                }
                final MutableMethodImplementation newImpl = new MutableMethodImplementation(implementation) {
                    //因为<clinit>方法不会有参数,所以直接改寄存器数不会出问题
                    @Override
                    public int getRegisterCount() {
                        return Math.max(1, implementation.getRegisterCount());
                    }
                };
                final List<BuilderInstruction> insns = getCallRegisterNativesMethod();
                for (int i = insns.size() - 1; i >= 0; i--) {
                    newImpl.addInstruction(0, insns.get(i));
                }

                return newImpl;

            }
        }

        //classesInit0(classIdx);
        private List<BuilderInstruction> getCallRegisterNativesMethod() {
            final List<BuilderInstruction> insns = new ArrayList<>();
            insns.add(buildConstInst(0, classIdx));
            insns.add(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1,
                    0, 0, 0, 0, 0,
                    new BaseMethodReference() {
                        @Nonnull
                        @Override
                        public String getDefiningClass() {
                            return registerNativeDefiningClass;
                        }

                        @Nonnull
                        @Override
                        public String getName() {
                            return registerNativeMethodName;
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
            ));
            return insns;
        }

    }

    private static BuilderInstruction buildConstInst(int regA, int i) {
        if (i < 0) {
            throw new RuntimeException("invalid index " + i);
        }
        if (i <= 7) {
            return new BuilderInstruction11n(Opcode.CONST_4, regA, i);
        }
        if (i <= 0x7fff) {
            return new BuilderInstruction21s(Opcode.CONST_16, regA, i);
        }
        return new BuilderInstruction31i(Opcode.CONST, regA, i);
    }

}
