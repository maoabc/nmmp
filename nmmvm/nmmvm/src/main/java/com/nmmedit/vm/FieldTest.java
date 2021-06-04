package com.nmmedit.vm;

// 域相关测试


public class FieldTest {

    // jni的GetStaticXXXField无法通过自身得到接口里的静态域,但java可以
    // 需要在生成jni代码时修复这种有问题的代码
    static interface A {
        public static Object obj = new Object();

        public static int INT = 3456;
    }

    static class B implements A {
    }

    public static Object getObj() {
        return B.obj;
    }

    public static native Object getObj0();

    public static int getInt() {
        return B.INT;
    }

    public static native int getInt0();
}
