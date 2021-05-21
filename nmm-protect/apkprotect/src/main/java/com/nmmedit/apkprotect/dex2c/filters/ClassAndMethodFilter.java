package com.nmmedit.apkprotect.dex2c.filters;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

public interface ClassAndMethodFilter {
    boolean acceptClass(ClassDef classDef);

    boolean acceptMethod(Method method);
}
