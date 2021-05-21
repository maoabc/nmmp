package com.nmmedit.apkprotect.dex2c.filters;

import com.nmmedit.apkprotect.obfus.MappingReader;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

import java.io.IOException;

/**
 * 自定义规则文件用于过滤需要处理的class和方法
 */
public class KeepConfig extends ProguardMappingConfig {

    public KeepConfig(ClassAndMethodFilter filter, MappingReader mappingReader) throws IOException {
        super(filter, mappingReader);
    }

    @Override
    protected boolean keepClass(ClassDef classDef) {
        //需要保留的class
        return true;
    }

    @Override
    protected boolean keepMethod(Method method) {
        return false;
    }
}
