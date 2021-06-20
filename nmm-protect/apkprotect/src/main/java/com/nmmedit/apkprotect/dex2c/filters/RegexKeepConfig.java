package com.nmmedit.apkprotect.dex2c.filters;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

import java.io.File;

/**
 *   class * extends android.app.Activity
 *   class * implements java.io.Serializable
 */
public class RegexKeepConfig implements ClassAndMethodFilter {
    private ClassAndMethodFilter filter;

    public RegexKeepConfig(ClassAndMethodFilter filter, File ruleFile) {
        this.filter = filter;
    }

    @Override
    public boolean acceptClass(ClassDef classDef) {
        if (filter != null && !filter.acceptClass(classDef)) {
            return false;
        }
        //todo
        return true;
    }

    @Override
    public boolean acceptMethod(Method method) {
        if (filter != null && !filter.acceptMethod(method)) {
            return false;
        }
        //todo
        return false;
    }
}
