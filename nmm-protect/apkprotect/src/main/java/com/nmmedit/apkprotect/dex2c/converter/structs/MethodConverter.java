package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodConverter {

    @Nonnull
    private final ClassAnalyzer classAnalyzer;

    public MethodConverter(@Nonnull ClassAnalyzer classAnalyzer) {
        this.classAnalyzer = classAnalyzer;
    }

    //转换方法为native及具体实现
    @Nonnull
    public Pair<List<? extends Method>, Method> convert(@Nonnull Method method) {
        // android6及以上不用特别处理
        if (MethodUtil.isDirect(method)) {
            return splitMethod(method);
        } else {
            // virtual method
            // 模仿api21和22的jni方法查找, 如果找到表示直接把方法标识为native会出错
            final MethodReference directMethod = classAnalyzer.findDirectMethod(method);
            if (directMethod == null) {
                // 不用特殊处理
                return splitMethod(method);
            }

            return generateMethod(method);

        }
    }


    // 把方法分离为native方法和一个正常方法
    private Pair<List<? extends Method>, Method> splitMethod(@Nonnull Method method) {
        return new Pair<>(Collections.singletonList(methodToNative(method)), method);
    }

    //方法转换为native方法
    private static Method methodToNative(final Method method) {
        return new ImmutableMethod(
                method.getDefiningClass(),
                method.getName(),
                // 因为方法实现被去掉,不删除参数annotation之类会导致生成的dex有问题
                removeAnnotation(method.getParameters()),
                method.getReturnType(),
                //添加native标识
                method.getAccessFlags() | AccessFlags.NATIVE.getValue(),
                method.getAnnotations(),
                method.getHiddenApiRestrictions(),
                null);
    }


    private String getNewMethodName(ClassDef classDef,
                                    String name,
                                    List<? extends CharSequence> parameterTypes,
                                    String returnType) {
        // 找到一个不冲突的方法名
        // 新方法名规则, 直接在原方法名后面加数字
        for (int idx = 0; idx < 0xFFFF; idx++) {
            final String newMethodName = name + idx;
            final MethodReference method = classAnalyzer.findMethod(classDef, newMethodName, parameterTypes, returnType);
            if (method == null) {
                return newMethodName;
            }
        }
        throw new RuntimeException("unknown");
    }

    private static int getResultRegisterWidth(String type) {
        int firstChar = type.charAt(0);
        if (firstChar == 'J' || firstChar == 'D') {
            return 2;
        } else if (firstChar == 'V') {
            return 0;
        } else {
            return 1;
        }
    }

    private Pair<List<? extends Method>, Method> generateMethod(@Nonnull Method method) {
        // 得到方法所在的class
        final ClassDef definingClass = classAnalyzer.getClassDef(method.getDefiningClass());
        if (definingClass == null) {
            // 可能没加载到对应dex, 直接简单处理
            return splitMethod(method);
        }

        final MethodImplementation implementation = method.getImplementation();
        if (implementation == null) {
            throw new RuntimeException("method error");
        }

        // 生成一个私有的native方法, 原方法调用这个native方法
        final String newMethodName = getNewMethodName(
                definingClass,
                method.getName(),
                method.getParameterTypes(),
                method.getReturnType());

        int accessFlags = AccessFlags.PRIVATE.getValue();
        if (MethodUtil.isStatic(method)) {
            accessFlags |= AccessFlags.STATIC.getValue();
        }

        final ImmutableMethod shellNativeMethod = new ImmutableMethod(
                method.getDefiningClass(),
                newMethodName,
                //去掉参数annotation,不然dex格式有问题
                removeAnnotation(method.getParameters()),
                method.getReturnType(),
                //私有的native方法
                accessFlags | AccessFlags.NATIVE.getValue(),
                method.getAnnotations(),
                method.getHiddenApiRestrictions(),
                null);

        // 方法指令部分被写入改名后的新方法
        final ImmutableMethod implNativeMethod = new ImmutableMethod(
                method.getDefiningClass(),
                newMethodName,
                method.getParameters(),
                method.getReturnType(),
                accessFlags,
                method.getAnnotations(),
                method.getHiddenApiRestrictions(),
                implementation);


        final MethodImplementation methodImpl = buildCallShellNativeMethodImpl(method, shellNativeMethod);


        final ImmutableMethod method1 = new ImmutableMethod(
                method.getDefiningClass(),
                method.getName(),
                method.getParameters(),
                method.getReturnType(),
                method.getAccessFlags(),
                method.getAnnotations(),
                method.getHiddenApiRestrictions(),
                methodImpl);

        return new Pair<>(Arrays.asList(method1, shellNativeMethod), implNativeMethod);
    }

    // 删除参数的annotation
    private static List<? extends MethodParameter> removeAnnotation(List<? extends MethodParameter> parameters) {
        final ArrayList<EmptyAnnotationMethodParameter> newParameters = new ArrayList<>();
        for (MethodParameter parameter : parameters) {
            newParameters.add(new EmptyAnnotationMethodParameter(parameter.getType()));
        }
        return newParameters;
    }

    private static MethodImplementation buildCallShellNativeMethodImpl(Method method, MethodReference shellNativeMethod) {
        // 生成方法调用代码
        final int parameterRegisterCount = MethodUtil.getParameterRegisterCount(method);

        final String returnType = method.getReturnType();

        // 得到接收方法返回值所需要的寄存器数量
        final int resultRegisterWidth = getResultRegisterWidth(returnType);

        // 保证方法有足够的寄存器保存方法调用后的返回值,
        // 比如签名为(I)J的静态方法,如果直接用参数寄存器接收返回结果,会导致单个寄存器无法保存long
        final int registerCount = Math.max(resultRegisterWidth, parameterRegisterCount);


        final int startReg = registerCount - parameterRegisterCount;

        final MutableMethodImplementation methodImpl = new MutableMethodImplementation(registerCount);
        if (parameterRegisterCount > 5) {//寄存器
            final BuilderInstruction3rc instruction = new BuilderInstruction3rc(Opcode.INVOKE_DIRECT_RANGE, startReg, parameterRegisterCount, shellNativeMethod);
            methodImpl.addInstruction(instruction);
        } else {
            //参数寄存器
            int regC = 0;
            int regD = 0;
            int regE = 0;
            int regF = 0;
            int regG = 0;
            switch (parameterRegisterCount) {
                case 5:
                    regG = startReg + 4;
                case 4:
                    regF = startReg + 3;
                case 3:
                    regE = startReg + 2;
                case 2:
                    regD = startReg + 1;
                case 1:
                    regC = startReg + 0;
            }
            final BuilderInstruction35c instruction = new BuilderInstruction35c(Opcode.INVOKE_DIRECT, parameterRegisterCount, regC, regD, regE, regF, regG, shellNativeMethod);
            methodImpl.addInstruction(instruction);
        }
        //返回类型不同,生成不同的返回指令
        int returnReg = 0;
        final char firstChar = returnType.charAt(0);
        if (firstChar == 'J' || firstChar == 'D') {
            final BuilderInstruction11x moveResult = new BuilderInstruction11x(Opcode.MOVE_RESULT_WIDE, returnReg);
            methodImpl.addInstruction(moveResult);

            final BuilderInstruction11x instruction = new BuilderInstruction11x(Opcode.RETURN_WIDE, returnReg);
            methodImpl.addInstruction(instruction);

        } else if (firstChar == 'L' || firstChar == '[') {
            final BuilderInstruction11x moveResult = new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, returnReg);
            methodImpl.addInstruction(moveResult);

            final BuilderInstruction11x instruction = new BuilderInstruction11x(Opcode.RETURN_OBJECT, returnReg);
            methodImpl.addInstruction(instruction);
        } else if (firstChar == 'V') {
            final BuilderInstruction10x instruction = new BuilderInstruction10x(Opcode.RETURN_VOID);
            methodImpl.addInstruction(instruction);
        } else {
            final BuilderInstruction11x moveResult = new BuilderInstruction11x(Opcode.MOVE_RESULT, returnReg);
            methodImpl.addInstruction(moveResult);

            final BuilderInstruction11x instruction = new BuilderInstruction11x(Opcode.RETURN, returnReg);
            methodImpl.addInstruction(instruction);
        }
        return methodImpl;
    }
}
