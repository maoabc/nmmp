//
// Created by mao on 20-10-7.
//
#include <stdlib.h>
#include <string.h>
#include "vm.h"
#include "Common.h"

#include "temp_resolver.c"

#ifdef __cplusplus
extern "C" {
#endif

#define SET_REGISTER_FLOAT(_idx, _val)      (*((float*) &regs[(_idx)]) = (_val))

#ifdef _LP64

#define SET_REGISTER_WIDE(_idx, _val)       (regs[(_idx)] =(s8) (_val));

#define SET_REGISTER_DOUBLE(_idx, _val)     (*((double*) &regs[(_idx)]) = (_val));

#else

#define SET_REGISTER_WIDE(_idx, _val)      putLongToArray(regs,_idx,_val);

#define SET_REGISTER_DOUBLE(_idx, _val)    putDoubleToArray(regs,_idx,_val);

#endif

static jobject
Java_retrofit2_b_a__Ljava_lang_Object_2Ljava_lang_String_2_Ljava_lang_Object_2(JNIEnv *env,
                                                                               jclass jcls,
                                                                               jobject p0,
                                                                               jobject p1) {
    int reg_count = 79;
    if (reg_count < 6) {
        regptr_t regs[2];
        regs[0] = 0;
        regs[1] = 0;
        regs[0] = (u8) p0;
        regs[1] = (u8) p1;


        u1 reg_flags[2];
        reg_flags[0] = 0;
        reg_flags[1] = 0;
        reg_flags[0] = 1;
        reg_flags[1] = 1;
    } else {
        const u2 flags_count =
                reg_count / sizeof(regptr_t) + (reg_count % sizeof(reg_count)) ? 1 : 0;
        regptr_t *regs = calloc(reg_count, sizeof(regptr_t) + flags_count);
        u1 *reg_flags = ((u1 *) regs) + reg_count * sizeof(regptr_t);
    }

    static const u2 insns[] = {
            0x0038, 0x0003, 0x0011, 0x0022, 0x1890, 0x2070, 0x78e9, 0x0010, 0x0027,
    };
    const u1 *tries = NULL;

    const vmCode code = {
            .insns=insns,
            .insnsSize=9,
//            .regs=regs,
//            .reg_flags=reg_flags,
            .triesHandlers=tries
    };

    jvalue value = vmInterpret(env,
                               &code,
                               &dvmResolver);
    return value.l;
}

typedef struct {
    u4 nameIdx;
    u4 sigIdx;
    void *fnPtr;
} MyNativeMethod;
static const MyNativeMethod gNativeMethods[] = {
        {27199, 47354, (void *) Java_retrofit2_b_a__Ljava_lang_Object_2Ljava_lang_String_2_Ljava_lang_Object_2},
};


typedef struct {
    u4 classIdx;
    u4 offset;
    u4 count;
} NativeMethodData;
static const NativeMethodData gNativeRegisterData[] = {
        {.classIdx = 4017, .offset = 14389, .count = 1},
};


static void
Java_com_nmmedit_protect_NativeUtil_classesInit0__I(JNIEnv *env, jclass jcls, jint dataIdx) {
#define MAX_METHOD 8
    JNINativeMethod methodBuf[MAX_METHOD];

    JNINativeMethod *methods;
    const NativeMethodData data = gNativeRegisterData[(u4) dataIdx];
    if (data.count > MAX_METHOD) {
        methods = (JNINativeMethod *) malloc(sizeof(JNINativeMethod) * data.count);
    } else {
        //方法数比较小直接使用栈内存,减少内存分配和释放
        methods = methodBuf;
    }

    jclass clazz = (*env)->FindClass(env, STRING_BY_CLASS_ID(data.classIdx));
    if (clazz == NULL) {
        return;
    }
    for (int midx = 0; midx < data.count; ++midx) {
        MyNativeMethod myNativeMethod = gNativeMethods[data.offset + midx];

        JNINativeMethod *method = methods + midx;
        method->name = STRING_BY_ID(myNativeMethod.nameIdx);
        method->signature = STRING_BY_ID(myNativeMethod.sigIdx);
        method->fnPtr = myNativeMethod.fnPtr;
    }

    (*env)->RegisterNatives(env, clazz, methods, data.count);

    (*env)->DeleteLocalRef(env, clazz);

    //不相等表示使用malloc申请的内存需要释放
    if (methods != methodBuf)free(methods);
}

#ifdef __cplusplus
}
#endif
