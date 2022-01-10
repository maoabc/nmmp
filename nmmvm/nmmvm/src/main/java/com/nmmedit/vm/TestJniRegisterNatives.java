package com.nmmedit.vm;


/**
 * android5(api 21和22) 注册jni方法(RegisterNatives)方法查找逻辑:
 * direct method  ->  parent direct method  ->  virtual method -> parent virtual method
 * 也就是 如果当前类一个virtual method 被native化,而父类刚好又有一个跟它同名的direct method,
 * 会导致找到错误的方法,然后报错
 *
 */
public class TestJniRegisterNatives extends TestJniRegisterNativesParent {

    //模拟加固后的类初始化
    static {
        System.loadLibrary("nmmp");
        initClass();
    }


    // 这个方法以标准jni函数命名来实现, 里面用RegisterNatives注册getRealOwner方法
    static native void initClass();

    //测试android5 jni注册问题
    // 属于 virtual method
    native Object getRealOwner();
}
