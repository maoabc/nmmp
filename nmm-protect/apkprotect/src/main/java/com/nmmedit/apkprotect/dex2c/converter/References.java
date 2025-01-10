package com.nmmedit.apkprotect.dex2c.converter;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.iface.reference.TypeReference;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import com.google.common.collect.Sets;
import com.nmmedit.apkprotect.util.ModifiedUtf8;

import javax.annotation.Nonnull;
import java.io.UTFDataFormatException;
import java.util.*;


//用于收集引用，生成c结构体同时用于指令重写时提供引用索引

public class References {

    private final ClassAnalyzer analyzer;
    //计算type名最大长度，方便生成c代码时直接使用栈上内存
    int maxTypeLen = 0;

    //基本字符串，其他引用全指向它
    private final HashSet<String> stringRefs = Sets.newHashSet();

    //所有类型引用
    private final HashSet<String> typeRefs = Sets.newHashSet();

    //域引用
    private final HashSet<MyFieldRef> fieldRefs = Sets.newHashSet();
    //方法引用
    private final HashSet<MyMethodRef> methodRefs = Sets.newHashSet();


    //方法参数加上返回类型，例如：(II)V
    private final HashSet<String> signatureRefs = Sets.newHashSet();

    //const-string两个指令需要的字符串
    private final HashSet<String> constantStrings = Sets.newHashSet();

    //扩展字符串池，原本dex没有，提前做些解析提升性能，为了不影响一些指令的引用索引防止超过65533,把它追加在字符串常量池后面
    private final HashSet<String> extStringRefs = Sets.newHashSet();

    References(@Nonnull DexBackedDexFile dexFile,
               @Nonnull ClassAnalyzer analyzer) {
        this.analyzer = analyzer;
        parseReferences(dexFile);
    }

    private void addStringRef(String type) {
        stringRefs.add(type);
    }

    private void addTypeRef(String type) {
        if (type == null) {
            return;
        }
        stringRefs.add(type);

//        添加一个去除首尾L和;的字符串
        extStringRefs.add(typeToClassName(type));

        typeRefs.add(type);
    }

    private void addConstStringRef(String type) {
        stringRefs.add(type);
        constantStrings.add(type);
    }

    private void addSignatureRef(String type) {
        extStringRefs.add(type);
        signatureRefs.add(type);
    }

    private void addExtStringRef(String type) {
        stringRefs.add(type);
    }

    @Nonnull
    private static String typeToClassName(@Nonnull String type) {
        if (type.charAt(0) == 'L') {
            return type.substring(1, type.length() - 1);
        }
        return type;
    }


    private void addMethodSignature(MethodReference reference) {
        final List<? extends CharSequence> parameterTypes = reference.getParameterTypes();
        StringBuilder sig = new StringBuilder();
        sig.append("(");
        for (CharSequence parameterType : parameterTypes) {
            sig.append(parameterType);
            addTypeRef(parameterType.toString());
        }
        sig.append(")");
        final String returnType = reference.getReturnType();
        sig.append(returnType);

        //shorty,用于确定方法调用参数类型
        addExtStringRef(MethodUtil.getShorty(parameterTypes, returnType));

        addTypeRef(returnType);

        //查找方法时需要，提前计算
        addSignatureRef(sig.toString());
    }

    private void addFieldRef(FieldReference reference, boolean isStatic) {
        addTypeRef(reference.getDefiningClass());

        addStringRef(reference.getName());

        addTypeRef(reference.getType());

        fieldRefs.add(new MyFieldRef(isStatic, reference));
    }

    private void addMethodRef(MethodReference reference, boolean isStatic) {
        addTypeRef(reference.getDefiningClass());

        addStringRef(reference.getName());

        addMethodSignature(reference);

        methodRefs.add(new MyMethodRef(isStatic, reference));
    }

    private void parseReferences(DexBackedDexFile dexFile) {
        //遍历所有classDef
        for (ClassDef classDef : dexFile.getClasses()) {
            //添加classDef自身相关
            addTypeRef(classDef.getType());
            addTypeRef(classDef.getSuperclass());
            for (String iface : classDef.getInterfaces()) {
                addTypeRef(iface);
            }

            //method，field

            for (Field field : classDef.getStaticFields()) {
                addFieldRef(field, true);
            }
            for (Field field : classDef.getInstanceFields()) {
                addFieldRef(field, false);
            }
            for (Method method : classDef.getDirectMethods()) {
                addMethodRef(method, AccessFlags.STATIC.isSet(method.getAccessFlags()));
            }
            for (Method method : classDef.getVirtualMethods()) {
                addMethodRef(method, false);
            }

            //收集方法指令内的引用
            for (Method method : classDef.getMethods()) {
                MethodImplementation implementation = method.getImplementation();
                if (implementation == null) {
                    continue;
                }
                collectReferences(implementation);
            }
        }

        //创建常量池

        makeStringPool();
        makeTypePool();
        makeFieldPool();
        makeMethodPool();

        makeSignaturePool();
        makeClassNamePool();

        //合并两个字符串常量池，不包含重复内容
        final ArrayList<String> stringPool = this.stringPool;
        final HashMap<String, Integer> stringPoolIndexMap = this.stringPoolIndexMap;
        for (String ref : extStringRefs) {
            if (!stringPoolIndexMap.containsKey(ref)) {
                stringPool.add(ref);
                stringPoolIndexMap.put(ref, stringPool.size() - 1);
            }
        }
        makeConstStringPool();
    }

    //解析所有引用指令,得到各种引用信息或者检测不支持指令
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

                    addFieldRef(reference, false);
                    break;
                }
                //sget_x
                case SGET_BYTE:
                case SGET_BOOLEAN:
                case SGET_CHAR:
                case SGET_SHORT:
                case SGET:
                case SGET_WIDE:
                case SGET_OBJECT: {
                    FieldReference reference = (FieldReference) ((ReferenceInstruction) instruction).getReference();

                    final FieldReference directFieldRef = analyzer.getDirectFieldRef(reference);
                    if (directFieldRef != null) {//处理接口静态域访问问题
                        addFieldRef(directFieldRef, true);
                    } else {
                        addFieldRef(reference, true);
                    }
                    break;
                }
                //sput_x
                case SPUT_BYTE:
                case SPUT_BOOLEAN:
                case SPUT_CHAR:
                case SPUT_SHORT:
                case SPUT:
                case SPUT_WIDE:
                case SPUT_OBJECT: {
                    FieldReference reference = (FieldReference) ((ReferenceInstruction) instruction).getReference();

                    addFieldRef(reference, true);
                    break;
                }
                case CONST_STRING:
                case CONST_STRING_JUMBO: {
                    StringReference reference = (StringReference) ((ReferenceInstruction) instruction).getReference();

                    addConstStringRef(reference.getString());
                    break;
                }

                case CONST_CLASS:
                case CHECK_CAST:
                case INSTANCE_OF:
                case NEW_INSTANCE:
                case NEW_ARRAY:
                case FILLED_NEW_ARRAY:
                case FILLED_NEW_ARRAY_RANGE: {
                    final TypeReference reference = (TypeReference) ((ReferenceInstruction) instruction).getReference();

                    addTypeRef(reference.getType());

                    break;
                }
                case INVOKE_STATIC:
                case INVOKE_STATIC_RANGE: {
                    MethodReference reference = (MethodReference) ((ReferenceInstruction) instruction).getReference();

                    addMethodRef(reference, true);
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

                    addMethodRef(reference, false);
                    break;
                }
                case INVOKE_CUSTOM:
                case INVOKE_CUSTOM_RANGE:
                case INVOKE_POLYMORPHIC:
                case INVOKE_POLYMORPHIC_RANGE: {
                    //todo 暂时没实现这些指令
                    throw new RuntimeException("Unsupported opcode： " + instruction.getOpcode());
                }
            }

            //收集异常type
            for (TryBlock<? extends ExceptionHandler> tryBlock : implementation.getTryBlocks()) {
                for (ExceptionHandler handler : tryBlock.getExceptionHandlers()) {
                    addTypeRef(handler.getExceptionType());
                }
            }
        }
    }

    final ArrayList<String> stringPool = new ArrayList<>();
    private final HashMap<String, Integer> stringPoolIndexMap = new HashMap<>();

    private void makeStringPool() {
        //添加到有序列表里，同时缓存索引给其他引用使用
        final ArrayList<String> stringPool = this.stringPool;
        stringPool.addAll(stringRefs);
        Collections.sort(stringPool);

        for (int i = 0; i < stringPool.size(); i++) {
            final String str = stringPool.get(i);
            stringPoolIndexMap.put(str, i);
        }
    }

    public int getStringItemIndex(String str) {
        final Integer integer = stringPoolIndexMap.get(str);
        if (integer == null) {
            throw new RuntimeException("unknown string ref: " + str);
        }
        return integer;
    }

    public List<String> getStringPool() {
        return stringPool;
    }


    //用于重写const-string指令索引
    final ArrayList<String> constStringPool = new ArrayList<>();
    private final HashMap<String, Integer> constStringPoolIndexMap = new HashMap<>();


    private void makeConstStringPool() {
        //常量字符串
        constStringPool.addAll(constantStrings);
        Collections.sort(constStringPool);
        for (int i = 0; i < constStringPool.size(); i++) {
            constStringPoolIndexMap.put(constStringPool.get(i), i);
        }
    }


    public List<String> getConstantStringPool() {
        return constStringPool;
    }

    public int getConstStringItemIndex(String constString) {
        final Integer integer = constStringPoolIndexMap.get(constString);
        if (integer == null) {
            throw new RuntimeException("unknown constString ref: " + constString);
        }
        return integer;
    }


    private final ArrayList<String> typePool = new ArrayList<>();
    private final HashMap<String, Integer> typePoolIndexMap = new HashMap<>();

    private void makeTypePool() {
        final ArrayList<String> typePool = this.typePool;
        typePool.addAll(typeRefs);
        Collections.sort(typePool);

        for (int i = 0; i < typePool.size(); i++) {
            typePoolIndexMap.put(typePool.get(i), i);
        }
    }

    public int getTypeItemIndex(String type) {
        final Integer integer = typePoolIndexMap.get(type);
        if (integer == null) {
            throw new RuntimeException("unknown type ref: " + type);
        }
        return integer;
    }

    public List<String> getTypePool() {
        return typePool;
    }

    public int getMaxTypeLen() {
        if (maxTypeLen == 0) {
            for (String type : typePool) {
                //计算 类型的最大长度
                final int countBytes;
                try {
                    countBytes = (int) ModifiedUtf8.countBytes(type, true);
                } catch (UTFDataFormatException e) {
                    throw new RuntimeException(e);
                }
                maxTypeLen = Math.max(maxTypeLen, countBytes);
            }
        }
        return maxTypeLen;
    }

    //保持跟type pool一致(顺序和大小)，className只是去掉L;的类型字符串，如果基本类型则不处理比如I,B
    private final ArrayList<String> classNamePool = new ArrayList<>();
    private final HashMap<String, Integer> classNamePoolIndexMap = new HashMap<>();

    private void makeClassNamePool() {
        for (String type : typePool) {
            classNamePool.add(typeToClassName(type));
        }

        for (int i = 0; i < classNamePool.size(); i++) {
            final String key = classNamePool.get(i);
            classNamePoolIndexMap.put(key, i);
        }
    }

    public int getClassNameItemIndex(String className) {
        final Integer integer = classNamePoolIndexMap.get(className);
        if (integer == null) {
            throw new RuntimeException("unknown class name ref: " + className);
        }
        return integer;
    }

    public List<String> getClassNamePool() {
        return classNamePool;
    }

    //方法签名及索引
    private final ArrayList<String> signaturePool = new ArrayList<>();
    private final HashMap<String, Integer> signaturePoolIndexMap = new HashMap<>();

    private void makeSignaturePool() {
        final ArrayList<String> classNamePool = this.signaturePool;
        classNamePool.addAll(signatureRefs);
        Collections.sort(classNamePool);

        for (int i = 0; i < classNamePool.size(); i++) {
            signaturePoolIndexMap.put(classNamePool.get(i), i);
        }
    }

    public int getSignatureItemIndex(String sig) {
        final Integer integer = signaturePoolIndexMap.get(sig);
        if (integer == null) {
            throw new RuntimeException("unknown signature ref: " + sig);
        }
        return integer;
    }

    public List<String> getSignaturePool() {
        return signaturePool;
    }

    private final ArrayList<FieldReference> fieldPool = new ArrayList<>();
    private final HashMap<FieldReference, Integer> fieldPoolIndexMap = new HashMap<>();

    private void makeFieldPool() {
        final ArrayList<FieldReference> fieldPool = this.fieldPool;
        for (MyFieldRef fieldRef : fieldRefs) {
            fieldPool.add(fieldRef.reference);
        }
        Collections.sort(fieldPool);
        for (int i = 0; i < fieldPool.size(); i++) {
            final FieldReference reference = fieldPool.get(i);
            fieldPoolIndexMap.put(reference, i);
        }
    }

    public int getFieldItemIndex(FieldReference reference) {
        final Integer integer = fieldPoolIndexMap.get(reference);
        if (integer == null) {
            throw new RuntimeException("unknown field ref: " + reference);
        }
        return integer;
    }

    public List<FieldReference> getFieldPool() {
        return fieldPool;
    }

    private final ArrayList<MethodReference> methodPool = new ArrayList<>();
    private final HashMap<MethodReference, Integer> methodPoolIndexMap = new HashMap<>();

    private void makeMethodPool() {
        final ArrayList<MethodReference> methodPool = this.methodPool;
        for (MyMethodRef methodRef : methodRefs) {
            methodPool.add(methodRef.reference);
        }
        Collections.sort(methodPool);
        for (int i = 0; i < methodPool.size(); i++) {
            final MethodReference reference = methodPool.get(i);
            methodPoolIndexMap.put(reference, i);
        }
    }

    public int getMethodItemIndex(MethodReference reference) {
        final Integer integer = methodPoolIndexMap.get(reference);
        if (integer == null) {
            throw new RuntimeException("unknown method ref: " + reference);
        }
        return integer;
    }

    public List<MethodReference> getMethodPool() {
        return methodPool;
    }


    //目前使用时没区分是否为静态，下面方法引用也一样
    static class MyFieldRef {
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

    static class MyMethodRef {
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