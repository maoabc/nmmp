package com.nmmedit.apkprotect.dex2c.filters;

import com.nmmedit.apkprotect.obfus.MappingProcessor;
import com.nmmedit.apkprotect.obfus.MappingReader;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 读取proguard的mapping.txt文件,根据它得到class和方法名混淆前后映射关系,然后再执行过滤规则
 */

public abstract class ProguardMappingConfig implements ClassAndMethodFilter, MappingProcessor {
    private final ClassAndMethodFilter filter;
    private final Map<String, String> newTypeOldTypeMap = new HashMap<>();

    public ProguardMappingConfig(ClassAndMethodFilter filter, MappingReader mappingReader) throws IOException {
        this.filter = filter;
        mappingReader.parse(this);
    }


    @Override
    public final boolean acceptClass(ClassDef classDef) {
        //先处理上游的过滤规则,如果上游不通过则直接返回不再处理,如果上游通过再处理当前的过滤规则
        if (filter != null && !filter.acceptClass(classDef)) {
            return false;
        }
        //得到混淆之前的className
        final String oldName = getOriginClassName(classDef.getType());
        if (oldName == null) {
            return false;
        }
        //需要保留的class返回false,
        return !keepClass(classDef);
    }

    protected abstract boolean keepClass(ClassDef classDef);

    protected String getOriginClassName(String type) {
        return newTypeOldTypeMap.get(type);
    }

    @Override
    public final boolean acceptMethod(Method method) {
        if (filter != null && !filter.acceptMethod(method)) {
            return false;
        }
        return !keepMethod(method);
    }

    protected abstract boolean keepMethod(Method method);

    private static String classNameToType(String className) {
        return "L" + className.replace('.', '/') + ";";
    }

    @Override
    public final void processClassMapping(String className, String newClassName) {
        newTypeOldTypeMap.put(classNameToType(newClassName), className);
    }

    @Override
    public final void processFieldMapping(String className, String fieldType, String fieldName, String newClassName, String newFieldName) {

    }

    @Override
    public final void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newClassName, int newFirstLineNumber, int newLastLineNumber, String newMethodName) {

    }
}
