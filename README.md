# nmmp
基于dex-vm运行dalvik字节码从而对dex进行保护，增加反编译难度。
项目分为两部分nmm-protect是纯java项目，对dex进行转换，把dex里方法及各种数据转为c结构体，处理apk生成c项目，编译生成so，输出处理后的apk。nmmvm是一个安卓项目，包含dex-vm实现及各种dalvik指令的测试等。
# nmm-protect

+ 简单使用

当前只支持linux环境，先安装好JDK及android sdk和ndk。

下载[nmm-protect.jar](https://github.com/maoabc/nmmp/releases/download/1.0/nmm-protect-1.0-SNAPSHOT.jar),配置好环境变量ANDROID_SDK_HOME、ANDROID_NDK_HOME:
``` bash
export ANDROID_SDK_HOME=/opt/android-sdk
export ANDROID_NDK_HOME=/opt/android-sdk/ndk/22.1.7171670
export CMAKE_PATH=/opt/android-sdk/cmake/3.18.1/   #可选，不配置的话直接使用/bin/cmake
```
然后运行jar：
``` bash
java -jar nmm-protect-xxx.jar input.apk
```
执行完毕会在input.apk所在的目录下生成一个build目录，里面包含最后输出的apk(build/input-protect.apk)，完整的c项目dex2c(基于cmake)及处理过程中生成的.dex等

生成的apk需要使用zipalign对齐及apksigner签名才能安装使用
``` bash
zipalign 4 build/input-protect.apk build/input-protect-align.apk
apksigner sign --ks ~/.myapp.jks build/input-protect-align.apk
```
+ 下载及编译项目
``` bash
git clone git@github.com:maoabc/nmmp.git
cd nmmp/nmm-protect
./gradlew arsc:build
./gradlew build
```
成功后会在build/libs生成可直接执行的fatjar。
+ 需要保护的类及规则处理

这个目前没在简单测试的jar上实现，需要自己改源码实现，内部有对应接口。

# nmmvm
nmmvm是dex虚拟机具体实现，入口就一个函数:
``` c
jvalue vmInterpret(
        JNIEnv *env,
        const vmCode *code,
        const vmResolver *dvmResolver
);

typedef struct {
    const u2 *insns;             //指令
    const u4 insnsSize;          //指令大小
    regptr_t *regs;                    //寄存器
    u1 *reg_flags;               //寄存器数据类型标记,主要标记是否为对象
    const u1 *triesHandlers;     //异常表
} vmCode;


typedef struct {

    const vmField *(*dvmResolveField)(JNIEnv *env, u4 idx, bool isStatic);

    const vmMethod *(*dvmResolveMethod)(JNIEnv *env, u4 idx, bool isStatic);

    //从类型常量池取得类型名
    const char *(*dvmResolveTypeUtf)(JNIEnv *env, u4 idx);

    //直接返回jclass对象,本地引用需要释放引用
    jclass (*dvmResolveClass)(JNIEnv *env, u4 idx);

    //根据类型名得到class
    jclass (*dvmFindClass)(JNIEnv *env, const char *type);

    //const_string指令加载的字符串对象
    jstring (*dvmConstantString)(JNIEnv *env, u4 idx);

} vmResolver;

```
vmCode提供执行所需要的指令、异常表及寄存器空间，vmResolver它包含一组函数指针，提供运行时的符号，比如field，method等。通过自定义这两个参数来实现不同的加固强度，比如项目里的test.cpp有一个简单的基于libdex实现的vmResolver，它主要用于开发测试。而nmm-protect实现的是把.dex相关数据转换为c结构体，还包含了opcode随机化等，基本可实际使用。
