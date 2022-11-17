package com.nmmedit.apkprotect.aar.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nonnull;

/**
 * 在原class插入初始化代码
 */
public class InjectStaticBlockVisitor extends ClassVisitor {
    private final String typeName;
    private final String methodName;
    private final int classIdx;

    private boolean hasStaticBlock;

    public InjectStaticBlockVisitor(int api,
                                    ClassVisitor classVisitor,
                                    String typeName,
                                    String methodName,
                                    int classIdx) {
        super(api, classVisitor);
        this.typeName = typeName;
        this.methodName = methodName;
        this.classIdx = classIdx;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        if ("<clinit>".equals(name)) {
            hasStaticBlock = true;
            final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(api, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();
                    //原class存在静态块，只需要在最前面插入调用初始化方法代码
                    genCallClassesInit(this, typeName, methodName, classIdx);
                }
            };
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (!hasStaticBlock) {//原class没有静态块，需要添加
            if (cv != null) {
                final MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                if (mv != null) {
                    mv.visitCode();

                    genCallClassesInit(mv, typeName, methodName, classIdx);

                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitEnd();
                }
            }
        }
    }

    // NativeUtils.classesInit(idx);
    // 方法签名固定为(I)V,就类名跟方法名可变
    private static void genCallClassesInit(@Nonnull MethodVisitor mv,
                                           String clsName,
                                           String initMethodName,
                                           int idx) {
        //选择更适合的索引加载指令
        if (idx < 0) {//unsigned int
            mv.visitLdcInsn(idx);
        } else if (idx <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, idx);
        } else if (idx <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, idx);
        } else {
            mv.visitLdcInsn(idx);
        }
        //调用classesInit方法
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, clsName, initMethodName, "(I)V", false);
    }
}
