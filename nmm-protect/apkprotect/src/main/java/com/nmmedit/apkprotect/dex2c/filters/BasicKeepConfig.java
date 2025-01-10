package com.nmmedit.apkprotect.dex2c.filters;

import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.nmmedit.apkprotect.dex2c.converter.MyMethodUtil;

/**
 * 基本过滤规则,其他规则必须先通过它, 然后才能处理自定义规则
 */

public class BasicKeepConfig implements ClassAndMethodFilter {
    @Override
    public boolean acceptClass(ClassDef classDef) {
        if (classDef.getType().startsWith("Landroidx/core/app/CoreComponentFactory")
                || classDef.getType().startsWith("Landroid/support/v4/app/CoreComponentFactory")
        ) {
            return false;
        }
        return true;
    }

    @Override
    public boolean acceptMethod(Method method) {

        if (MyMethodUtil.isConstructorOrAbstract(method)
                || MyMethodUtil.isBridgeOrSynthetic(method)
        ) {
            return false;
        }
        return true;
    }
}