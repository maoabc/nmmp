package com.nmmedit.apkprotect.dex2c.converter;

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import com.nmmedit.apkprotect.util.ModifiedUtf8;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * 根据dex生成符号解析代码,比如字符串常量池,类型常量池这些
 */

public class ResolverCodeGenerator {


    private final References references;

    public ResolverCodeGenerator(DexBackedDexFile dexFile,
                                 @Nonnull ClassAnalyzer analyzer
    ) {

        references = new References(dexFile, analyzer);
    }

    public References getReferences() {
        return references;
    }

    public void generate(Writer writer) throws IOException {
        writer.write("#include \"GlobalCache.h\"\n");
        writer.write("#include \"ConstantPool.h\"\n\n");
        writer.write("#include <pthread.h>\n\n\n");

        generateStringPool(writer);
        generateTypePool(writer);

        //额外添加的,方便生成结构体
        generateClassNamePool(writer);
        generateSignaturePool(writer);

        generateFieldPool(writer);

        generateMethodPool(writer);

        generateStringConstants(writer);

        //生成初始化函数及符号解析器结构体
        generateResolver(writer);
    }

    //产生const-string*指令对应的缓存
    private void generateStringConstants(Writer writer) throws IOException {
        final References references = this.references;
        final List<String> constantStringPool = references.getConstantStringPool();

        final int[] constStringIds = new int[constantStringPool.size()];
        for (int i = 0; i < constantStringPool.size(); i++) {
            //把得到字符串索引
            constStringIds[i] = references.getStringItemIndex(constantStringPool.get(i));
        }

        //
        writer.write(
                "\n//字符串常量索引缓存,const-string指令索引被重写，直接根据索引得到字符串索引，然后创建jstring\n" +
                        "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} ConstStringId;\n"
        );

        writer.write("static const ConstStringId gStringConstantIds[] = {\n");
        for (int offset : constStringIds) {
            writer.write(String.format("    {.idx=0x%04x},\n", offset));
        }
        writer.write("};\n");

        writer.write(String.format("static jstring gStringConstants[%d];\n\n", constStringIds.length));
    }

    private void generateResolver(Writer writer) throws IOException {
        writer.write("static void resolver_init(JNIEnv *env) {\n" +
                "    if(sizeof(gFields) == 0) return;\n" +
                "    if(sizeof(gMethods) == 0) return;\n" +
                "    if(sizeof(gStringConstants) == 0) return;\n" +
                "    memset(gFields, 0, sizeof(gFields));\n" +
                "    memset(gMethods, 0, sizeof(gMethods));\n" +
                "    memset(gStringConstants, 0, sizeof(gStringConstants));\n" +
                "}\n" +
                "\n" +
                "#define STRING_BY_ID(_idx) ((const char *) (gBaseStrPtr + gStringIds[_idx].off))\n" +
                "\n" +
                "#define STRING_BY_TYPE_ID(_idx) (STRING_BY_ID(gTypeIds[_idx].idx))\n" +
                "\n" +
                "#define STRING_BY_CLASS_ID(_idx) (STRING_BY_ID(gClassIds[_idx].idx))\n" +
                "\n" +
                "#define STRING_BY_SIGNATURE_ID(_idx) (STRING_BY_ID(gSignatureIds[_idx].idx))\n" +
                "\n" +
                "#define FIND_CLASS_BY_NAME(_className)                          \\\n" +
                "    clazz = (*env)->FindClass(env, _className);                 \\\n" +
                "    if (clazz == NULL) {                                        \\\n" +
                "        /*转换异常类型,保持和正常java抛一样异常*/                   \\\n" +
                "        (*env)->ExceptionClear(env);                            \\\n" +
                "        vmThrowNoClassDefFoundError(env, _className);           \\\n" +
                "        return NULL;                                            \\\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "static void vmThrowNoClassDefFoundError(JNIEnv *env, const char *msg) {\n" +
                "    (*env)->ThrowNew(env, gVm.exNoClassDefFoundError, msg);\n" +
                "}\n" +
                "\n" +
                "static void vmThrowNoSuchFieldError(JNIEnv *env, const char *msg) {\n" +
                "    (*env)->ThrowNew(env, gVm.exNoSuchFieldError, msg);\n" +
                "}\n" +
                "\n" +
                "static void vmThrowNoSuchMethodError(JNIEnv *env, const char *msg) {\n" +
                "    (*env)->ThrowNew(env, gVm.exNoSuchMethodError, msg);\n" +
                "}\n" +
                "\n" +
                "static const vmField *dvmResolveField(JNIEnv *env, u4 idx, bool isStatic) {\n" +
                "    vmField *field = &gFields[idx];\n" +
                "    if (field->fieldId == NULL) {\n" +
                "        FieldId fieldId = gFieldIds[idx];\n" +
                "\n" +
                "        jclass clazz;\n" +
                "        FIND_CLASS_BY_NAME(STRING_BY_CLASS_ID(fieldId.classIdx));\n" +
                "\n" +
                "        const char *type = STRING_BY_TYPE_ID(fieldId.typeIdx);\n" +
                "        const char *name = STRING_BY_ID(fieldId.nameIdx);\n" +
                "\n" +
                "        field->classIdx = fieldId.classIdx;\n" +
                "        field->type = (*type == '[') ? 'L' : *type;\n" +
                "\n" +
                "        //和方法解析同理,最后赋值fieldId\n" +
                "        jfieldID fid;\n" +
                "        if (isStatic) {\n" +
                "            fid = (*env)->GetStaticFieldID(env, clazz, name, type);\n" +
                "        } else {\n" +
                "            fid = (*env)->GetFieldID(env, clazz, name, type);\n" +
                "        }\n" +
                "        if (fid == NULL) {\n" +
                "            (*env)->DeleteLocalRef(env, clazz);\n" +
                "\n" +
                "            (*env)->ExceptionClear(env);\n" +
                "            vmThrowNoSuchFieldError(env, name);\n" +
                "            return NULL;\n" +
                "        }\n" +
                "        (*env)->DeleteLocalRef(env, clazz);\n" +
                "\n" +
                "\n" +
                "        field->fieldId = fid;\n" +
                "\n" +
                "    }\n" +
                "    return field;\n" +
                "}\n" +
                "\n" +
                "static const vmMethod *dvmResolveMethod(JNIEnv *env, u4 idx, bool isStatic) {\n" +
                "    vmMethod *method = &gMethods[idx];\n" +
                "    if (method->methodId == NULL) {\n" +
                "        MethodId methodId = gMethodIds[idx];\n" +
                "\n" +
                "        jclass clazz;\n" +
                "        FIND_CLASS_BY_NAME(STRING_BY_CLASS_ID(methodId.classIdx));\n" +
                "\n" +
                "        method->shorty = STRING_BY_ID(methodId.shortyIdx);\n" +
                "\n" +
                "        method->classIdx = methodId.classIdx;\n" +
                "\n" +
                "        const char *name = STRING_BY_ID(methodId.nameIdx);\n" +
                "        const char *sig = STRING_BY_SIGNATURE_ID(methodId.sigIdx);\n" +
                "\n" +
                "        jmethodID mid;\n" +
                "        if (isStatic) {\n" +
                "            mid = (*env)->GetStaticMethodID(env, clazz, name, sig);\n" +
                "        } else {\n" +
                "            mid = (*env)->GetMethodID(env, clazz, name, sig);\n" +
                "        }\n" +
                "        if (mid == NULL) {\n" +
                "            (*env)->DeleteLocalRef(env, clazz);\n" +
                "\n" +
                "            (*env)->ExceptionClear(env);\n" +
                "            vmThrowNoSuchMethodError(env, name);\n" +
                "            return NULL;\n" +
                "        }\n" +
                "        (*env)->DeleteLocalRef(env, clazz);\n" +
                "\n" +
                "        //只根据method->methodId判断是否需要解析,最后赋值为了防止结构体解析一半被其他线程使用从而导致错误\n" +
                "        //todo 赋值需为原子操作\n" +
                "\n" +
                "        method->methodId = mid;\n" +
                "\n" +
                "    }\n" +
                "    return method;\n" +
                "}\n" +
                "\n" +
                "static pthread_mutex_t str_mutex = PTHREAD_MUTEX_INITIALIZER;\n" +

                "static jstring dvmConstantString(JNIEnv *env, u4 idx) {\n" +
                "    //先查找索引位置是否存在缓存,不用频繁创建string对象\n" +
                "    if (gStringConstants[idx] == NULL) {\n" +
                "        pthread_mutex_lock(&str_mutex);\n" +
                "        jstring str;\n" +
                "        if (gStringConstants[idx] == NULL) {\n" +
                "            str = (*env)->NewStringUTF(env, STRING_BY_ID(gStringConstantIds[idx].idx));\n" +
                "            gStringConstants[idx] = (*env)->NewGlobalRef(env, str);\n" +
                "        } else {\n" +
                "            str = (*env)->NewLocalRef(env, gStringConstants[idx]);\n" +
                "        }\n" +
                "        pthread_mutex_unlock(&str_mutex);\n" +
                "\n" +
                "        return str;\n" +
                "    } else {\n" +
                "        return (*env)->NewLocalRef(env, gStringConstants[idx]);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "static const char *dvmResolveTypeUtf(JNIEnv *env, u4 idx) {\n" +
                "    return STRING_BY_TYPE_ID(idx);\n" +
                "}\n" +
                "\n" +
                "static jclass dvmResolveClass(JNIEnv *env, u4 idx) {\n" +
                "    jclass clazz = getCacheClass(env, STRING_BY_TYPE_ID(idx));\n" +
                "    if (clazz != NULL) {\n" +
                "        return (jclass) (*env)->NewLocalRef(env, clazz);\n" +
                "    }\n" +
                "\n" +
                "    FIND_CLASS_BY_NAME(STRING_BY_CLASS_ID(idx));\n" +
                "\n" +
                "    return clazz;\n" +
                "}\n\n");

        //因为类型需要去掉开头的'L'和结尾的';',所以最大最大class名不需要再加1表示字符串结尾
        writer.write(String.format(
                "static jclass dvmFindClass(JNIEnv *env, const char *type) {\n" +
                        "    jclass clazz = getCacheClass(env, type);\n" +
                        "    if (clazz != NULL) {\n" +
                        "        return (jclass) (*env)->NewLocalRef(env, clazz);\n" +
                        "    }\n" +
                        "    if (*type == 'L') {\n" +
                        "        char clazzName[%d];\n" +
                        "        size_t len = strlen(type);\n" +
                        "        strncpy(clazzName, type + 1, len - 2);\n" +
                        "        clazzName[len - 2] = 0;\n" +
                        "\n" +
                        "        FIND_CLASS_BY_NAME(clazzName);\n" +
                        "\n" +
                        "        return clazz;\n" +
                        "    }\n" +
                        "\n" +
                        "    FIND_CLASS_BY_NAME(type);\n" +
                        "\n" +
                        "    return clazz;\n" +
                        "}\n\n", references.getMaxTypeLen()));
        writer.write(
                "static const vmResolver dvmResolver = {\n" +
                        "        .dvmResolveField = dvmResolveField,\n" +
                        "        .dvmResolveMethod = dvmResolveMethod,\n" +
                        "        .dvmResolveTypeUtf = dvmResolveTypeUtf,\n" +
                        "        .dvmResolveClass = dvmResolveClass,\n" +
                        "        .dvmFindClass = dvmFindClass,\n" +
                        "        .dvmConstantString = dvmConstantString,\n" +
                        "};\n" +
                        "\n");
    }

    private void generateMethodPool(Writer writer) throws IOException {
        final References references = this.references;
        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u2 classIdx;\n" +
                        "    u4 nameIdx;\n" +
                        "    u4 shortyIdx;\n" +
                        "    u4 sigIdx;\n" +
                        "} MethodId;\n\n");
        writer.write("static const MethodId gMethodIds[] = {\n");

        final List<MethodReference> methodPool = references.getMethodPool();
        for (MethodReference methodReference : methodPool) {
            String definingClass = methodReference.getDefiningClass();
            String className;
            if (definingClass.charAt(0) == 'L') {
                className = definingClass.substring(1, definingClass.length() - 1);
            } else {
                className = definingClass;
            }
            int classNameIdx = references.getClassNameItemIndex(className);
            if (classNameIdx < 0) {
                throw new RuntimeException("unknown class name" + definingClass);
            }
            String name = methodReference.getName();
            int nameIdx = references.getStringItemIndex(name);
            if (nameIdx < 0) {
                throw new RuntimeException("unknown method name");
            }
            int shortyIdx = references.getStringItemIndex(MethodUtil.getShorty(methodReference.getParameterTypes(), methodReference.getReturnType()));
            if (shortyIdx < 0) {
                throw new RuntimeException("unknown method shorty");
            }
            String signature = MyMethodUtil.getMethodSignature(methodReference.getParameterTypes(), methodReference.getReturnType());
            int sigIdx = references.getSignatureItemIndex(signature);
            if (sigIdx < 0) {
                throw new RuntimeException("unknown method signature");
            }

            writer.write(String.format(
                    "    {.classIdx=%d, .nameIdx=%d, .shortyIdx=%d, .sigIdx=%d},\n",
                    classNameIdx, nameIdx, shortyIdx, sigIdx));
        }
        writer.write("};\n");
        writer.write("//ends method data\n\n");
        writer.write(String.format("static vmMethod gMethods[%d];\n", methodPool.size()));
        writer.write("\n");
    }

    private void generateFieldPool(Writer writer) throws IOException {
        final References references = this.references;
        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u2 classIdx;\n" +
                        "    u4 nameIdx;\n" +
                        "    u2 typeIdx;\n" +
                        "} FieldId;\n\n");
        writer.write("static const FieldId gFieldIds[] = {\n");

        final List<FieldReference> fieldPool = references.getFieldPool();
        for (FieldReference reference : fieldPool) {
            String definingClass = reference.getDefiningClass();
            String className;
            if (definingClass.charAt(0) == 'L') {
                className = definingClass.substring(1, definingClass.length() - 1);
            } else {
                className = definingClass;
            }
            int classNameIdx = references.getClassNameItemIndex(className);
            if (classNameIdx < 0) {
                throw new RuntimeException("unknown class name");
            }
            int nameIdx = references.getStringItemIndex(reference.getName());
            if (nameIdx < 0) {
                throw new RuntimeException("unknown field name");
            }
            int typeIdx = references.getTypeItemIndex(reference.getType());
            if (typeIdx < 0) {
                throw new RuntimeException("unknown field type");
            }

            writer.write(String.format(
                    "    {.classIdx=%d, .nameIdx=%d, .typeIdx=%d},\n",
                    classNameIdx, nameIdx, typeIdx));
        }
        writer.write("};\n");
        writer.write("//ends field id\n\n");
        writer.write(String.format("static vmField gFields[%d];\n", fieldPool.size()));
    }


    private void generateStringPool(Writer writer) throws IOException {
        writer.write("static const u1 gBaseStrPtr[]={\n");

        ArrayList<Long> strOffsets = new ArrayList<>();
        long strOffset = 0;

        final List<String> stringPool = references.getStringPool();
        for (String string : stringPool) {

            //必须使用modified utf8，不然jni的NewStringUtf函数可能出问题.issue #3
            byte[] bytes = ModifiedUtf8.encode(string);

            writer.write("    ");
            for (byte aByte : bytes) {
                writer.write(String.format("0x%02x,", aByte & 0xFF));
            }
            writer.write("0x00,\n");

            strOffsets.add(strOffset);
            strOffset += bytes.length + 1;
        }
        writer.write("};\n\n");

        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u4 off;\n" +
                        "} StringId;\n");

        writer.write("static const StringId gStringIds[] = {\n");
        for (Long offset : strOffsets) {
            if (offset > 0xFFFFFFFFL) {
                throw new RuntimeException("string offset too long");
            }
            writer.write(String.format("    {.off=0x%04x},\n", offset));
        }
        writer.write("};\n");
        writer.write("//ends string ids\n\n");

        writer.flush();
    }

    static String stringEsc(String str) throws UTFDataFormatException {
        byte[] bytes = ModifiedUtf8.encode(str);
        StringBuilder sb = new StringBuilder(4 * bytes.length);
        for (byte b : bytes) {
            sb.append(String.format("\\x%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private void generateTypePool(Writer writer) throws IOException {

        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} TypeId;\n");

        writer.write("static const TypeId gTypeIds[] = {\n");
        final References references = this.references;
        for (String type : references.getTypePool()) {
            writer.write(String.format("    {.idx=%d},\n", references.getStringItemIndex(type)));
        }
        writer.write("};\n");
        writer.write("//ends type ids\n\n");
        writer.flush();
    }

    //根据类型池,去掉L开头和;得到class name,其他则不变
    private void generateClassNamePool(Writer writer) throws IOException {
        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} ClassId;\n");


        writer.write("static const ClassId gClassIds[] = {\n");

        final References references = this.references;
        for (String className : references.getClassNamePool()) {
            int classNameIdx = references.getStringItemIndex(className);
            if (classNameIdx < 0) {
                throw new RuntimeException("string not contain");
            }
            writer.write(String.format("    {.idx=%d},\n", classNameIdx));

        }
        writer.write("};\n");
        writer.write("//ends class name ids\n\n");
    }

    private void generateSignaturePool(Writer writer) throws IOException {
        writer.write(
                "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} SignatureId;\n");

        writer.write("static const SignatureId gSignatureIds[] = {\n");

        final References references = this.references;
        for (String sig : references.getSignaturePool()) {
            int sigIdx = references.getStringItemIndex(sig);
            if (sigIdx < 0) {
                throw new RuntimeException("string not contain");
            }
            writer.write(String.format("    {.idx=%d},\n", sigIdx));
        }
        writer.write("};\n");
        writer.write("//ends method signature pool\n\n");
    }
}