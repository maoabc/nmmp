package com.nmmedit.apkprotect.dex2c.filters;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

public class SimpleConvertConfig implements ClassAndMethodFilter {
    private final ClassAndMethodFilter filter;
    private final SimpleRule simpleRule;

    public SimpleConvertConfig(ClassAndMethodFilter filter, SimpleRule simpleRule) {
        this.filter = filter;
        this.simpleRule = simpleRule;
    }

    @Override
    public boolean acceptClass(ClassDef classDef) {
        if (filter != null && !filter.acceptClass(classDef)) {
            return false;
        }
        return simpleRule != null && simpleRule.matchClass(
                classDef.getType(),
                classDef.getSuperclass(),
                classDef.getInterfaces());
    }

    @Override
    public boolean acceptMethod(Method method) {
        if (filter != null && !filter.acceptMethod(method)) {
            return false;
        }
        return simpleRule != null && simpleRule.matchMethod(method.getName());
    }
}
