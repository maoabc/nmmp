package com.nmmedit.apkprotect.dex2c.converter.testbuild;

import java.util.List;

/**
 * 生成开发测试用的jni代码,依赖libdex从符号dex里提取各种信息
 */
public class JniTemp {
    public static final String CODE_TEMP_INSNS_TRIES = "\n\n    const DexCode *dexCode = findDexCode(\"%s\", \"%s\");\n" +
            "    const u2 *insns = dexCode->insns;\n" +
            "    u4 insnsSize = dexCode->insnsSize;\n" +
            "\n" +
            "    size_t size = dexGetTryHandlerSize(dexCode);\n" +
            "    const DexTry *tries = dexGetTries(dexCode);\n" +
            "\n" +
            "    u1 *tryHandler = (u1 *) malloc(size + 4);\n" +
            "    u2 *ts = (u2 *) tryHandler;\n" +
            "    ts[0] = dexCode->triesSize;\n" +
            "    ts[1] = 0;\n" +
            "\n" +
            "    memcpy(tryHandler + 4, tries, size);\n" +
            "\n" +
            "    jvalue value = dvmInterpret(env, insns, insnsSize, regs, (const u1 *) tryHandler, &dvmResolver);\n" +
            "    free(tryHandler);\n" +
            "\n";

    public static String genJniCode(String classType, String methodName, List<? extends CharSequence> parameterTypes, boolean isStatic, int registerCount, int parameterRegisterCount, String returnType) {
        StringBuilder params = new StringBuilder();

        //寄存器初始化
        StringBuilder regsAss = new StringBuilder(String.format("    u8 regs[%d];\n    memset(regs, 0, sizeof(regs));\n", registerCount));


        String clazzName = classType.substring(1, classType.length() - 1);
        String jniCode = String.format("JNIEXPORT %s Java_%s_%s(JNIEnv *env, %s ",
                getJNIType(returnType),
                clazzName.replace('/', '_'),
                methodName,
                isStatic ? "jclass jcls" : "jobject thiz"
        );

        int paramRegStart = registerCount - parameterRegisterCount;
        if (!isStatic) {
            regsAss.append(String.format("    regs[%d] = (u8) thiz;\n", paramRegStart++));
        }

        int size = parameterTypes.size();
        for (int i = 0; i < size; i++) {
            String type = parameterTypes.get(i).toString();
            String jniType = getJNIType(type);
            int argNum = isStatic ? i : i + 1;
            params.append(jniType)
                    .append(" p")
                    .append(argNum);
            if (type.startsWith("[") || type.startsWith("L")) {//对象类型,转为u8
                regsAss.append(String.format("    regs[%d] = (u8) p%d;\n", paramRegStart++, argNum));
            } else if (type.equals("F")) {//先转为u4指针,然后再取值,把float存进4字节里,double同理
                regsAss.append(String.format("    regs[%d] = *(u4 *) &p%d;\n", paramRegStart++, argNum));
            } else if (type.equals("D")) {
                regsAss.append(String.format("    regs[%d] = *(u8 *) &p%d;\n", paramRegStart++, argNum));
            } else {
                regsAss.append(String.format("    regs[%d] = p%d;\n", paramRegStart++, argNum));
            }
            if (type.equals("J") || type.equals("D")) {//long和double需要两个寄存器
                paramRegStart++;
            }
            if (i < size - 1) {//最后不用加,
                params.append(",  ");
            }

        }
        if (params.length() > 0) {
            jniCode += ", " + params.toString();
        }
        jniCode += ") {\n";
        jniCode += regsAss;
        jniCode += String.format(CODE_TEMP_INSNS_TRIES, classType, methodName);


        if (!returnType.equals("V")) {
            char typeCh = returnType.charAt(0);
            jniCode += String.format("    return value.%s;\n", Character.toLowerCase(typeCh == '[' ? 'L' : typeCh));
        }
        jniCode += "}\n";

        return jniCode;
    }

    public static String getJNIType(String type) {
        switch (type) {
            case "Z":
                return "jboolean";
            case "B":
                return "jbyte";
            case "S":
                return "jshort";
            case "C":
                return "jchar";
            case "I":
                return "jint";
            case "F":
                return "jfloat";
            case "J":
                return "jlong";
            case "D":
                return "jdouble";
            case "Ljava/lang/String;":
                return "jstring";
            case "V":
                return "void";
            default:
                return "jobject";
        }
    }
}
