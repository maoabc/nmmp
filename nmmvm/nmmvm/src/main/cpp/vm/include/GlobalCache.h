//
// Created by mao on 20-9-14.
//

#ifndef DEX_EDITOR_GLOBALCACHE_H
#define DEX_EDITOR_GLOBALCACHE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif


typedef struct {
    jclass exNoClassDefFoundError;
    jclass exNoSuchFieldError;
    jclass exNoSuchFieldException;
    jclass exNoSuchMethodError;
    jclass exNullPointerException;
    jclass exArithmeticException;
    jclass exInternalError;
    jclass exNegativeArraySizeException;
    jclass exArrayIndexOutOfBoundsException;
    jclass exClassCastException;
    jclass exClassNotFoundException;
    jclass exRuntimeException;
} vmGlobals;

//需要在加载库时初始化对应的异常类
extern vmGlobals gVm;


//缓存一些基本类型class,加载库的时候调用
void cacheInitial(JNIEnv *env);

//根据类型名取得jclass
jclass getCacheClass(JNIEnv *env, const char *type);


#ifdef __cplusplus
}
#endif

#endif //DEX_EDITOR_GLOBALCACHE_H
