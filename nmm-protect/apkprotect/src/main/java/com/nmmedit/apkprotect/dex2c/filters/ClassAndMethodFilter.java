package com.nmmedit.apkprotect.dex2c.filters;


import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;

public interface ClassAndMethodFilter {
    boolean acceptClass(ClassDef classDef);

    boolean acceptMethod(Method method);
}
