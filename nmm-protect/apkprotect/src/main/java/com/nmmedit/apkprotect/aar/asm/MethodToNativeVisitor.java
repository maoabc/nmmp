package com.nmmedit.apkprotect.aar.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Map;

/**
 * 把需要转换的方法标识为native，去掉字节码部分
 */
public class MethodToNativeVisitor extends ClassVisitor {
    private final Map<AsmMethod, List<AsmMethod>> convertedMethods;

    public MethodToNativeVisitor(int api,
                                 ClassVisitor classVisitor,
                                 Map<AsmMethod, List<AsmMethod>> convertedMethods) {
        super(api, classVisitor);
        this.convertedMethods = convertedMethods;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        final List<AsmMethod> myMethods = convertedMethods.get(new AsmMethod(access, name, descriptor));
        if (myMethods != null && !myMethods.isEmpty()) {
            if (myMethods.size() == 1) {
                final MethodVisitor mv = super.visitMethod(access | Opcodes.ACC_NATIVE, name, descriptor, signature, exceptions);
                //不需要code等部分
                return null;
            }
            // myMethods.size()==2
            // todo 生成第一个方法调用第二个方法代码，解决一些native方法无法正常初始化问题。
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    //生成第一个方法调用第二个的字节码
    private static void genCall(ClassVisitor cv, AsmMethod method1, AsmMethod method2) {
        final String sig1 = method1.descriptor;
        final String sig2 = method2.descriptor;
        if (!sig1.equals(sig2)) {
            throw new IllegalStateException();
        }
        final MethodVisitor mv = cv.visitMethod(method1.access, method1.name, sig1, null, null);

    }

    private static void genLoadInsn(MethodVisitor mv, int slotIdx, String methodName, String sig) {
       
    }
}
