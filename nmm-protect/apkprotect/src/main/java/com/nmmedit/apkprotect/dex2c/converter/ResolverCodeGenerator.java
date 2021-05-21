package com.nmmedit.apkprotect.dex2c.converter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.util.MethodUtil;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 根据dex生成符号解析代码,比如字符串常量池,类型常量池这些
 */

public class ResolverCodeGenerator {

    private final ArrayList<String> stringPool = new ArrayList<>();
    private final HashMap<String, Integer> stringPoolIndexMap = new HashMap<>();

    private final ArrayList<String> typePool = new ArrayList<>();
    private final HashMap<String, Integer> typePoolIndexMap = new HashMap<>();

    private final ArrayList<String> classNames = new ArrayList<>();
    private final HashMap<String, Integer> classNamePoolIndexMap = new HashMap<>();

    //方法签名及索引
    private final ArrayList<String> signatures = new ArrayList<>();
    private final HashMap<String, Integer> signaturePoolIndexMap = new HashMap<>();


    private final HashMultimap<String, MyFieldRef> fieldRefs = HashMultimap.create();
    private final HashMultimap<String, MyMethodRef> methodRefs = HashMultimap.create();
    private final HashSet<TypeReference> constantClasses = Sets.newHashSet();
    private final HashSet<StringReference> constantStrings = Sets.newHashSet();

    private final DexBackedDexFile dexFile;

    private final int maxTypeLen;

    public ResolverCodeGenerator(DexBackedDexFile dexFile) {
        this.dexFile = dexFile;

        for (ClassDef classDef : dexFile.getClasses()) {
            for (Method method : classDef.getMethods()) {
                MethodImplementation implementation = method.getImplementation();
                if (implementation == null) {
                    continue;
                }
                //解析所有引用指令,得到各种引用信息或者检测不支持指令
                collectReferences(implementation);
            }
        }
        //在原本字符串常量之上再添加新字符串,不再排序因此不会导致其他部分索引出问题
        ArrayList<String> stringPool = this.stringPool;

        stringPool.clear();
        stringPool.addAll(dexFile.getStringSection());

        //用于快速判断stringPool里是否有对应字符串,因此随着stringPool改变而改变
        HashSet<String> stringSet = new HashSet<>(dexFile.getStringSection());


        ArrayList<String> typePool = this.typePool;
        typePool.clear();
        typePool.addAll(dexFile.getTypeSection());
        int maxTypeLen = 0;
        for (int i = 0; i < typePool.size(); i++) {
            String type = typePool.get(i);
            typePoolIndexMap.put(type, i);

            maxTypeLen = Math.max(maxTypeLen, type.getBytes(StandardCharsets.UTF_8).length);
        }
        this.maxTypeLen = maxTypeLen;


        classNames.clear();
        //生成类名和位置索引
        for (int i = 0; i < typePool.size(); i++) {
            String type = typePool.get(i);
            if (type.charAt(0) == 'L') {
                type = type.substring(1, type.length() - 1);
            }
            if (!stringSet.contains(type)) {
                stringSet.add(type);
                stringPool.add(type);
            }
            classNames.add(type);
            classNamePoolIndexMap.put(type, i);
        }


        //方法签名
        List<DexBackedMethodReference> methodSection = dexFile.getMethodSection();
        TreeSet<String> signatureSet = new TreeSet<>(String::compareTo);
        for (MethodReference reference : methodSection) {
            String signature = MyMethodUtil.getMethodSignature(reference.getParameterTypes(), reference.getReturnType());
            signatureSet.add(signature);
        }
        signatures.clear();
        signatures.addAll(signatureSet);

        for (int i = 0; i < signatures.size(); i++) {
            String sig = signatures.get(i);
            if (!stringSet.contains(sig)) {
                stringSet.add(sig);
                stringPool.add(sig);
            }
            signaturePoolIndexMap.put(sig, i);
        }

        for (int i = 0; i < stringPool.size(); i++) {
            String str = stringPool.get(i);
            stringPoolIndexMap.put(str, i);
        }

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
        final int[] constIds = new int[constantStrings.size()];
        int idx = 0;
        for (StringReference strRef : constantStrings) {
            constIds[idx++] = stringPoolIndexMap.get(strRef.getString());
        }
        //排序, 运行时以二分法查找
        Arrays.sort(constIds);

        writer.write("static const u4 gStringConstantIds[] = {\n");
        for (int offset : constIds) {
            writer.write(String.format("    0x%04x,\n", offset));
        }
        writer.write("};\n");

        writer.write(String.format("static jstring gStringConstants[%d];\n\n", constIds.length));

    }

    private void generateResolver(Writer writer) throws IOException {
        writer.write("static void resolver_init(JNIEnv *env) {\n" +
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
                "\n" +
                "static jstring dvmConstantString(JNIEnv *env, u4 idx) {\n" +
                "    //先查找索引位置是否存在缓存,不用频繁创建string对象\n" +
                "    s4 i = binarySearch(gStringConstantIds, sizeof(gStringConstantIds) / sizeof(u4), idx);\n" +
                "    if (i >= 0) {\n" +
                "        if (gStringConstants[i] == NULL) {\n" +
                "            pthread_mutex_lock(&str_mutex);\n" +
                "            jstring str;\n" +
                "            if (gStringConstants[i] == NULL) {\n" +
                "                str = (*env)->NewStringUTF(env, STRING_BY_ID(idx));\n" +
                "                gStringConstants[i] = (*env)->NewGlobalRef(env, str);\n" +
                "            } else {\n" +
                "                str = (*env)->NewLocalRef(env, gStringConstants[i]);\n" +
                "            }\n" +
                "            pthread_mutex_unlock(&str_mutex);\n" +
                "\n" +
                "            return str;\n" +
                "        } else {\n" +
                "            return (*env)->NewLocalRef(env, gStringConstants[i]);\n" +
                "        }\n" +
                "    }\n" +
                "    return (*env)->NewStringUTF(env, STRING_BY_ID(idx));\n" +
                "}\n" +
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
                        "}\n\n", maxTypeLen));
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
        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u2 classIdx;\n" +
                        "    u4 nameIdx;\n" +
                        "    u4 shortyIdx;\n" +
                        "    u4 sigIdx;\n" +
                        "} MethodId;\n\n");
        writer.write("static const MethodId gMethodIds[] = {\n");

        List<DexBackedMethodReference> methodSection = dexFile.getMethodSection();
        for (MethodReference methodReference : methodSection) {
            String definingClass = methodReference.getDefiningClass();
            String className;
            if (definingClass.charAt(0) == 'L') {
                className = definingClass.substring(1, definingClass.length() - 1);
            } else {
                className = definingClass;
            }
            Integer classNameIdx = classNamePoolIndexMap.get(className);
            if (classNameIdx == null || classNameIdx < 0) {
                throw new RuntimeException("unknown class name" + definingClass);
            }
            String name = methodReference.getName();
            Integer nameIdx = stringPoolIndexMap.get(name);
            if (nameIdx == null || nameIdx < 0) {
                throw new RuntimeException("unknown method name");
            }
            Integer shortyIdx = stringPoolIndexMap.get(MethodUtil.getShorty(methodReference.getParameterTypes(), methodReference.getReturnType()));
            if (shortyIdx == null || shortyIdx < 0) {
                throw new RuntimeException("unknown method shorty");
            }
            String signature = MyMethodUtil.getMethodSignature(methodReference.getParameterTypes(), methodReference.getReturnType());
            Integer sigIdx = signaturePoolIndexMap.get(signature);
            if (sigIdx == null || sigIdx < 0) {
                throw new RuntimeException("unknown method signature");
            }

            writer.write(String.format(
                    "    {.classIdx=%d, .nameIdx=%d, .shortyIdx=%d, .sigIdx=%d},\n",
                    classNameIdx, nameIdx, shortyIdx, sigIdx));
        }
        writer.write("};\n");
        writer.write("//ends method data\n\n");
        writer.write(String.format("static vmMethod gMethods[%d];\n", methodSection.size()));
        writer.write("\n");
    }

    private void generateFieldPool(Writer writer) throws IOException {
        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u2 classIdx;\n" +
                        "    u4 nameIdx;\n" +
                        "    u2 typeIdx;\n" +
                        "} FieldId;\n\n");
        writer.write("static const FieldId gFieldIds[] = {\n");

        List<DexBackedFieldReference> fieldSection = dexFile.getFieldSection();
        for (FieldReference reference : fieldSection) {
            String definingClass = reference.getDefiningClass();
            String className;
            if (definingClass.charAt(0) == 'L') {
                className = definingClass.substring(1, definingClass.length() - 1);
            } else {
                className = definingClass;
            }
            Integer classNameIdx = classNamePoolIndexMap.get(className);
            if (classNameIdx == null || classNameIdx < 0) {
                throw new RuntimeException("unknown class name");
            }
            String name = reference.getName();
            Integer nameIdx = stringPoolIndexMap.get(name);
            if (nameIdx == null || nameIdx < 0) {
                throw new RuntimeException("unknown field name");
            }
            String type = reference.getType();
            Integer typeIdx = typePoolIndexMap.get(type);
            if (typeIdx == null || typeIdx < 0) {
                throw new RuntimeException("unknown field type");
            }

            writer.write(String.format(
                    "    {.classIdx=%d, .nameIdx=%d, .typeIdx=%d},\n",
                    classNameIdx, nameIdx, typeIdx));
        }
        writer.write("};\n");
        writer.write("//ends field id\n\n");
        writer.write(String.format("static vmField gFields[%d];\n", fieldSection.size()));
    }


    private void generateStringPool(Writer writer) throws IOException {
        writer.write("static const u1 gBaseStrPtr[]={\n");

        ArrayList<Long> strOffsets = new ArrayList<>();
        long strOffset = 0;
        for (String string : stringPool) {
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

            writer.write("    ");
            ;
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

//        writer.write("static const char *gStringPool[] = {\n");
//        for (String string : stringPool) {
//            writer.write(String.format("    \"%s\",\n", stringEsc(string)));
//        }
//        writer.write("};\n");
        writer.flush();
    }

    static String stringEsc(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(4 * bytes.length);
        for (byte b : bytes) {
            sb.append(String.format("\\x%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private void generateTypePool(Writer writer) throws IOException {
        HashMap<String, Integer> stringPoolIndexMap = this.stringPoolIndexMap;

        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} TypeId;\n");

        writer.write("static const TypeId gTypeIds[] = {\n");
        for (String type : typePool) {
            writer.write(String.format("    {.idx=%d},\n", stringPoolIndexMap.get(type)));
        }
        writer.write("};\n");
        writer.write("//ends type ids\n\n");
        writer.flush();
    }

    //根据类型池,去掉L开头和;得到class name,其他则不变
    private void generateClassNamePool(Writer writer) throws IOException {
        HashMap<String, Integer> stringPoolIndexMap = this.stringPoolIndexMap;

        writer.write(
                "\n" +
                        "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} ClassId;\n");


        writer.write("static const ClassId gClassIds[] = {\n");

        List<String> classNames = this.classNames;
        for (String className : classNames) {
            Integer classNameIdx = stringPoolIndexMap.get(className);
            if (classNameIdx == null || classNameIdx < 0) {
                throw new RuntimeException("string not contain");
            }
            writer.write(String.format("    {.idx=%d},\n", classNameIdx));

        }
        writer.write("};\n");
        writer.write("//ends class name ids\n\n");
    }

    private void generateSignaturePool(Writer writer) throws IOException {
        HashMap<String, Integer> stringPoolIndexMap = this.stringPoolIndexMap;

        writer.write(
                "typedef struct {\n" +
                        "    u4 idx;\n" +
                        "} SignatureId;\n");

        writer.write("static const SignatureId gSignatureIds[] = {\n");

        List<String> signatures = this.signatures;
        for (String sig : signatures) {
            Integer sigIdx = stringPoolIndexMap.get(sig);
            if (sigIdx == null || sigIdx < 0) {
                throw new RuntimeException("string not contain");
            }
            writer.write(String.format("    {.idx=%d},\n", sigIdx));
        }
        writer.write("};\n");
        writer.write("//ends method signature pool\n\n");
    }

    public int getIndexByClassName(String className) {
        return classNamePoolIndexMap.get(className);
    }

    public int getIndexByString(String str) {
        return stringPoolIndexMap.get(str);
    }

    private void collectReferences(MethodImplementation implementation) {
        for (Instruction instruction : implementation.getInstructions()) {
            switch (instruction.getOpcode()) {
                //iget_x
                case IGET_BYTE:
                case IGET_BOOLEAN:
                case IGET_CHAR:
                case IGET_SHORT:
                case IGET:
                case IGET_WIDE:
                case IGET_OBJECT:
                    //iput_x
                case IPUT_BYTE:
                case IPUT_BOOLEAN:
                case IPUT_CHAR:
                case IPUT_SHORT:
                case IPUT:
                case IPUT_WIDE:
                case IPUT_OBJECT: {
                    FieldReference reference = (FieldReference) ((ReferenceInstruction) instruction).getReference();
                    fieldRefs.put(reference.getDefiningClass(), new MyFieldRef(false, reference));
                    break;
                }
                //sget_x
                case SGET_BYTE:
                case SGET_BOOLEAN:
                case SGET_CHAR:
                case SGET_SHORT:
                case SGET:
                case SGET_WIDE:
                case SGET_OBJECT:
                    //sput_x
                case SPUT_BYTE:
                case SPUT_BOOLEAN:
                case SPUT_CHAR:
                case SPUT_SHORT:
                case SPUT:
                case SPUT_WIDE:
                case SPUT_OBJECT: {
                    FieldReference reference = (FieldReference) ((ReferenceInstruction) instruction).getReference();
                    fieldRefs.put(reference.getDefiningClass(), new MyFieldRef(true, reference));
                    break;
                }
                case CONST_STRING:
                case CONST_STRING_JUMBO: {
                    StringReference reference = (StringReference) ((ReferenceInstruction) instruction).getReference();
                    constantStrings.add(reference);
                    break;
                }
                case CONST_CLASS: {
                    TypeReference reference = (TypeReference) ((ReferenceInstruction) instruction).getReference();
                    constantClasses.add(reference);
                    break;
                }
                case INVOKE_STATIC:
                case INVOKE_STATIC_RANGE: {
                    MethodReference reference = (MethodReference) ((ReferenceInstruction) instruction).getReference();
                    methodRefs.put(reference.getDefiningClass(), new MyMethodRef(true, reference));
                    break;
                }
                case INVOKE_DIRECT:
                case INVOKE_DIRECT_RANGE:
                case INVOKE_SUPER:
                case INVOKE_SUPER_RANGE:
                case INVOKE_INTERFACE:
                case INVOKE_INTERFACE_RANGE:
                case INVOKE_VIRTUAL:
                case INVOKE_VIRTUAL_RANGE: {
                    MethodReference reference = (MethodReference) ((ReferenceInstruction) instruction).getReference();
                    methodRefs.put(reference.getDefiningClass(), new MyMethodRef(false, reference));
                    break;
                }
                case INVOKE_CUSTOM:
                case INVOKE_CUSTOM_RANGE:
                case INVOKE_POLYMORPHIC:
                case INVOKE_POLYMORPHIC_RANGE: {
                    //todo 暂时没实现这些指令
                    throw new RuntimeException("Don't support");
                }
            }
        }
    }

    public static class MyFieldRef {
        public final boolean isStatic;
        public final FieldReference reference;

        public MyFieldRef(boolean isStatic, FieldReference reference) {
            this.isStatic = isStatic;
            this.reference = reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyFieldRef that = (MyFieldRef) o;

            return reference.equals(that.reference);
        }

        @Override
        public int hashCode() {
            return reference.hashCode();
        }
    }

    public static class MyMethodRef {
        public final boolean isStatic;
        public final MethodReference reference;

        public MyMethodRef(boolean isStatic, MethodReference reference) {
            this.isStatic = isStatic;
            this.reference = reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyMethodRef that = (MyMethodRef) o;

            return reference.equals(that.reference);
        }

        @Override
        public int hashCode() {
            return reference.hashCode();
        }
    }

}
