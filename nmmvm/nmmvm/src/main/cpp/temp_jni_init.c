//
// Created by mao on 20-10-4.
//

#include <jni.h>
#include "GlobalCache.h"
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    cacheInitial(env);


    return JNI_VERSION_1_6;
}


#ifdef __cplusplus
}
#endif
