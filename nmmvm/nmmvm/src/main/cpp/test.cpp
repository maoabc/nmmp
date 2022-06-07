
#include <cstdio>
#include <cstring>
#include <malloc.h>
#include <jni.h>
#include <libdex/DexClass.h>
#include <libdex/DexCatch.h>
#include <fcntl.h>
#include "vm.h"
#include "ScopedLocalRef.h"
#include "GlobalCache.h"

#ifdef __cplusplus
extern "C" {
#endif

#define SET_REGISTER_FLOAT(_idx, _val)      (*((float*) &regs[(_idx)]) = (_val))

//#ifdef _LP64

#define SET_REGISTER_WIDE(_idx, _val)       (regs[(_idx)] =(s8) (_val));

#define SET_REGISTER_DOUBLE(_idx, _val)     (*((double*) &regs[(_idx)]) = (_val));

//#else
//
//#define SET_REGISTER_WIDE(_idx, _val)      putLongToArray(regs,_idx,_val);
//
//#define SET_REGISTER_DOUBLE(_idx, _val)    putDoubleToArray(regs,_idx,_val);
//
//#endif

const vmField *dvmResolveField(JNIEnv *env, u4 idx, bool isStatic);

jclass dvmResolveClass(JNIEnv *env, u4 idx);

jstring dvmResolveString(JNIEnv *env, u4 idx);

const char *dvmResolveTypeUtf(JNIEnv *env, u4 idx);

const vmMethod *dvmResolveMethod(JNIEnv *env, u4 idx, bool isStatic);

jclass dvmFindClass(JNIEnv *env, const char *type);
//todo 实现解析虚拟机所需要的各种符号
static vmResolver dvmResolver = {
        .dvmResolveField = dvmResolveField,
        .dvmResolveMethod = dvmResolveMethod,
        .dvmResolveClass = dvmResolveClass,
        .dvmFindClass=dvmFindClass,
        .dvmResolveTypeUtf = dvmResolveTypeUtf,
        .dvmConstantString = dvmResolveString,
};
DexFile *pDex;
JNIEXPORT void Java_com_nmmedit_vm_VmTest_loadDex0
        (JNIEnv *env, jclass jcls, jbyteArray dex) {

    jbyte *dexbuf = env->GetByteArrayElements(dex, NULL);
    jsize length = env->GetArrayLength(dex);
    pDex = dexFileParse((const u1 *) dexbuf, length, 0);
    //用于测试,不释放数组空间
}

//根据类名方法名查找方法代码,不处理重载方法
const DexCode *findDexCode(const char *className, const char *methodName) {
    for (int i = 0; i < pDex->pHeader->classDefsSize; ++i) {
        const DexClassDef *pDef = dexGetClassDef(pDex, i);
        if (strcmp(dexGetClassDescriptor(pDex, pDef), className) == 0) {
            const u1 *classData = dexGetClassData(pDex, pDef);
            DexClassData *pClassData = dexReadAndVerifyClassData(&classData, NULL);
#define MATCH_METHOD(_methods, _methodSize, _methodName)                                  \
            for (int midx = 0; midx < (_methodSize); ++midx) {                            \
                DexMethod &dexMethod = (_methods)[midx];                                  \
                const DexMethodId *pMethodId = dexGetMethodId(pDex,                       \
                                                              dexMethod.methodIdx);       \
                if (strcmp(dexStringById(pDex, pMethodId->nameIdx), _methodName) == 0) {  \
                    return dexGetCode(pDex, &dexMethod);                                  \
                }                                                                         \
            }
            MATCH_METHOD(pClassData->directMethods,
                         pClassData->header.directMethodsSize, methodName);

            MATCH_METHOD(pClassData->virtualMethods,
                         pClassData->header.virtualMethodsSize, methodName);
        }
    }
    return NULL;
}
const char *dvmResolveTypeUtf(JNIEnv *env, u4 idx) {
    return dexStringByTypeIdx(pDex, idx);
}

jclass dvmResolveClass(JNIEnv *env, u4 idx) {
    char buf[128];
    char *clazzName;
    jclass clazz;

    const char *type = dexStringByTypeIdx(pDex, idx);
    clazz = getCacheClass(env, type);
    if (clazz != NULL) {
        return (jclass) env->NewLocalRef(clazz);
    }


    size_t len = strlen(type);
    if (len > sizeof(buf)) {
        clazzName = (char *) malloc(len);//len-2+1
    } else {
        clazzName = buf;
    }

    //去除开始L和结尾;
    strncpy(clazzName, type + 1, len - 2);
    //len-2 null
    clazzName[len - 2] = '\0';

    clazz = env->FindClass(clazzName);

    if (clazzName != buf) {
        free(clazzName);
    }
    return clazz;
}

jclass dvmFindClass(JNIEnv *env, const char *type) {
    char buf[128];
    char *clazzName;
    jclass clazz;

    clazz = getCacheClass(env, type);
    if (clazz != NULL) {
        return (jclass) env->NewLocalRef(clazz);
    }


    size_t len = strlen(type);
    if (len > sizeof(buf)) {
        clazzName = (char *) malloc(len);//len-2+1
    } else {
        clazzName = buf;
    }

    //去除开始L和结尾;
    strncpy(clazzName, type + 1, len - 2);
    //len-2 null
    clazzName[len - 2] = '\0';

    clazz = env->FindClass(clazzName);

    if (clazzName != buf) {
        free(clazzName);
    }
    return clazz;
}

const vmField *dvmResolveField(JNIEnv *env, u4 idx, bool isStatic) {
    const DexFieldId *pFid = dexGetFieldId(pDex, idx);
    const char *name = dexStringById(pDex, pFid->nameIdx);
    const char *type = dexStringByTypeIdx(pDex, pFid->typeIdx);
    vmField *vf = (vmField *) malloc(sizeof(vmField));
    vf->classIdx = pFid->classIdx;

    ScopedLocalRef<jclass> clazz(env, dvmResolveClass(env, pFid->classIdx));

    vf->type = type[0];

    if (isStatic) {
        vf->fieldId = env->GetStaticFieldID(clazz.get(), name, type);
    } else {
        vf->fieldId = env->GetFieldID(clazz.get(), name, type);
    }
    return vf;
}

jstring dvmResolveString(JNIEnv *env, u4 idx) {
    const char *str = dexStringById(pDex, idx);
    ALOGD("resolve string %s", str);
    return env->NewStringUTF(str);
}

const vmMethod *dvmResolveMethod(JNIEnv *env, u4 idx, bool isStatic) {
    const DexMethodId *pMethodId = dexGetMethodId(pDex, idx);
    const DexProtoId *pProtoId = dexGetProtoId(pDex, pMethodId->protoIdx);
    const DexTypeList *parameters = dexGetProtoParameters(pDex, pProtoId);

    const char *name = dexStringById(pDex, pMethodId->nameIdx);
//    const char *type = dexStringByTypeIdx(pDex, pF->typeIdx);

    ScopedLocalRef<jclass> clazz(env, dvmResolveClass(env, pMethodId->classIdx));
    char params[2048];
    memset(params, 0, sizeof(params));
    if (parameters != NULL) {
        for (int i = 0; i < parameters->size; ++i) {
            const DexTypeItem &item = parameters->list[i];
            sprintf(params, "%s%s", params, dexStringByTypeIdx(pDex, item.typeIdx));
        }
    }

    char sig[2048];
    sprintf(sig, "(%s)%s", params, dexStringByTypeIdx(pDex, pProtoId->returnTypeIdx));

    ALOGD("method name=%s,sig=%s", name, sig);

    vmMethod *vm = (vmMethod *) malloc(sizeof(vmMethod));
    vm->classIdx = pMethodId->classIdx;
    vm->shorty = dexStringById(pDex, pProtoId->shortyIdx);
    if (isStatic) {
        vm->methodId = env->GetStaticMethodID(clazz.get(), name, sig);
    } else {
        vm->methodId = env->GetMethodID(clazz.get(), name, sig);
    }


    return vm;
}
jclass clazz;
void init(JNIEnv *env) {
    static vmMethod methods[] = {
            {
                    .classIdx=0,
                    .shorty="VL",
                    .methodId=env->GetMethodID(clazz, "", "")
            },

    };
}



//JNI

JNIEXPORT void Java_com_nmmedit_vm_Vm_parseDex
        (JNIEnv *env, jclass jcls, jbyteArray dex) {
    jbyte *dexbuf = env->GetByteArrayElements(dex, NULL);
    jsize length = env->GetArrayLength(dex);
    DexFile *pDex = dexFileParse((const u1 *) dexbuf, length, 0);


    for (int i = 0; i < pDex->pHeader->classDefsSize; ++i) {
        const DexClassDef *pDef = dexGetClassDef(pDex, i);
        const char *clsname = dexGetClassDescriptor(pDex, pDef);
        if (strcmp(clsname, "Lcom/nmmedit/vm/VmTest;") == 0) {
            ALOGD("class name %s", clsname);
            const u1 *classData = dexGetClassData(pDex, pDef);
            DexClassData *pClassData = dexReadAndVerifyClassData(&classData, NULL);
            for (int midx = 0; midx < pClassData->header.directMethodsSize; ++midx) {
                DexMethod &dexMethod = pClassData->directMethods[midx];
                const DexMethodId *pMethodId = dexGetMethodId(pDex,
                                                              dexMethod.methodIdx);


                if (strcmp(dexStringById(pDex, pMethodId->nameIdx), "aput") == 0) {
                    const DexCode *pCode = dexGetCode(pDex, &dexMethod);
                    ALOGD("class method %p", pCode);
                    ALOGD("method %s regs=%d insnsSize=%d",
                          dexStringById(pDex, pMethodId->nameIdx),
                          pCode->registersSize, pCode->insnsSize);
                    for (int insnIdx = 0; insnIdx < pCode->insnsSize; ++insnIdx) {
                        ALOGD("method insns[%d]=%04x", insnIdx, pCode->insns[insnIdx]);
                    }
                }
            }
            for (int midx = 0; midx < pClassData->header.virtualMethodsSize; ++midx) {
                DexMethod &dexMethod = pClassData->virtualMethods[midx];
                const DexMethodId *pMethodId = dexGetMethodId(pDex,
                                                              dexMethod.methodIdx);


                if (strcmp(dexStringById(pDex, pMethodId->nameIdx), "ifieldPut") == 0) {
                    const DexCode *pCode = dexGetCode(pDex, &dexMethod);
                    ALOGD("class method %p", pCode);
                    ALOGD("method %s regs=%d insnsSize=%d",
                          dexStringById(pDex, pMethodId->nameIdx),
                          pCode->registersSize, pCode->insnsSize);
                    for (int insnIdx = 0; insnIdx < pCode->insnsSize; ++insnIdx) {
                        ALOGD("method insns[%d]=%04x", insnIdx, pCode->insns[insnIdx]);
                    }
                }
            }
        }
    }

//    ALOGD("dex file %p", pDexFile);


}

JNIEXPORT jint Java_com_nmmedit_vm_VmTest_iadd0
        (JNIEnv *env, jclass jcls, jint a, jint b) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "iadd");
    if (dexCode == NULL) {
        return 0;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = b;
    regs[registersSize - 2] = a;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);


    return value.i;
}
JNIEXPORT jlong Java_com_nmmedit_vm_VmTest_ladd0
        (JNIEnv *env, jclass jcls, jlong a, jlong b) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "ladd");
    if (dexCode == NULL) {
        return 0;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    SET_REGISTER_WIDE(registersSize - 2, b);
    SET_REGISTER_WIDE(registersSize - 4, a);

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return value.j;
}
JNIEXPORT jint Java_com_nmmedit_vm_VmTest_loop0
        (JNIEnv *env, jclass jcls, jint count) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "loop");
    if (dexCode == NULL) {
        return 0;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = count;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return value.i;
}
JNIEXPORT jint Java_com_nmmedit_vm_VmTest_sfieldGet0
        (JNIEnv *env, jclass jcls) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "sfieldGet");
    if (dexCode == NULL) {
        return 0;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;

    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);


    return value.i;
}
JNIEXPORT void Java_com_nmmedit_vm_VmTest_setsfield0
        (JNIEnv *env, jclass jcls, jint v) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "setsfield");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = v;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    vmInterpret(env, &code, &dvmResolver);

}
JNIEXPORT jint Java_com_nmmedit_vm_VmTest_ifieldGet0
        (JNIEnv *env, jobject obj) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "ifieldGet");
    if (dexCode == NULL) {
        return 0;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = (regptr_t) obj;
    reg_flags[registersSize - 1] = 1;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return value.i;
}
JNIEXPORT void Java_com_nmmedit_vm_VmTest_ifieldPut0
        (JNIEnv *env, jobject obj, jboolean v) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "ifieldPut");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = v;
    regs[registersSize - 2] = (regptr_t) obj;
    reg_flags[registersSize - 2] = 1;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    vmInterpret(env, &code, &dvmResolver);

}
JNIEXPORT jint Java_com_nmmedit_vm_VmTest_packedSwitch0
        (JNIEnv *env, jclass jcls, jint key) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "packedSwitch");
    if (dexCode == NULL) {
        return 0;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = key;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);


    return value.i;
}
JNIEXPORT jint Java_com_nmmedit_vm_VmTest_aget0
        (JNIEnv *env, jclass jcls, jintArray array, jint idx) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "aget");
    if (dexCode == NULL) {
        return NULL;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = idx;
    regs[registersSize - 2] = (regptr_t) array;
    reg_flags[registersSize - 2] = 1;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return value.i;
}
JNIEXPORT jstring Java_com_nmmedit_vm_VmTest_aput0
        (JNIEnv *env, jclass jcls, jobjectArray array, jint idx, jstring val) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "aput");
    if (dexCode == NULL) {
        return NULL;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = (regptr_t) val;
    regs[registersSize - 2] = idx;
    regs[registersSize - 3] = (regptr_t) array;
    reg_flags[registersSize - 1] = 1;
    reg_flags[registersSize - 3] = 1;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return static_cast<jstring>(value.l);
}
JNIEXPORT jstring Java_com_nmmedit_vm_VmTest_filledNewArray0
        (JNIEnv *env, jclass jcls, jint idx) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "filledNewArray");
    if (dexCode == NULL) {
        return NULL;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = idx;
    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return static_cast<jstring>(value.l);
}

/*
 * Compute the size, in bytes, of tries and catch_handlers.
 */
size_t dexGetTryHandlerSize(const DexCode *pCode) {
    /*
     * The catch handler data is the last entry.  It has a variable number
     * of variable-size pieces, so we need to create an iterator.
     */
    u4 handlersSize;
    u4 offset;
    u4 ui;

    if (pCode->triesSize != 0) {
        handlersSize = dexGetHandlersSize(pCode);
        offset = dexGetFirstHandlerOffset(pCode);
    } else {
        handlersSize = 0;
        offset = 0;
    }

    for (ui = 0; ui < handlersSize; ui++) {
        DexCatchIterator iterator;
        dexCatchIteratorInit(&iterator, pCode, offset);
        offset = dexCatchIteratorGetEndOffset(&iterator, pCode);
    }

    const u1 *handlerData = dexGetCatchHandlerData(pCode);

    ALOGD("+++ pCode=%p handlerData=%p last offset=%d",
          pCode, handlerData, offset);

    /* return the size of the catch handler + everything before it */
    return (handlerData - (u1 *) dexGetTries(pCode)) + offset;
}

JNIEXPORT void Java_com_nmmedit_vm_VmTest_tryCatch0
        (JNIEnv *env, jobject jobj) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "tryCatch");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = (regptr_t) jobj;
    reg_flags[registersSize - 1] = 1;
    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;

    size_t size = dexGetTryHandlerSize(dexCode);
    const DexTry *tries = dexGetTries(dexCode);

    u1 *tryHandler = (u1 *) malloc(size + 4);
    u2 *ts = (u2 *) tryHandler;
    ts[0] = dexCode->triesSize;
    ts[1] = 0;

    memcpy(tryHandler + 4, tries, size);

    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=tryHandler
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

}

JNIEXPORT void Java_com_nmmedit_vm_VmTest_callMethodA
        (JNIEnv *env, jclass jcls) {
    jclass clazz = env->FindClass("com/nmmedit/vm/VmTest");
    jmethodID methodId = env->GetStaticMethodID(clazz, "ladd0", "(JJ)J");
    jvalue args[5];
    args[0].j = 56;
    args[1].j = 1;
    jlong i = env->CallStaticLongMethodA(clazz, methodId, args);
    ALOGD("davlik ret %d", i);

}
JNIEXPORT jstring Java_com_nmmedit_vm_VmTest_invokeVirtual0
        (JNIEnv *env, jclass jcls, jobject sb) {
    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "invokeVirtual");
    if (dexCode == NULL) {
        return NULL;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = (regptr_t) sb;
    reg_flags[registersSize - 1] = 1;
    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return (jstring) value.l;
}
JNIEXPORT jboolean Java_com_nmmedit_vm_VmTest_invokeSuper0
        (JNIEnv *env, jobject jobj, jstring sb) {
    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "add");
    if (dexCode == NULL) {
        return JNI_FALSE;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = (regptr_t) sb;
    regs[registersSize - 2] = (regptr_t) jobj;

    reg_flags[registersSize - 1] = 1;
    reg_flags[registersSize - 2] = 1;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;

    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);


    return value.z;
}
JNIEXPORT void Java_com_nmmedit_vm_VmTest_listIter0
        (JNIEnv *env, jclass jcls) {


    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "listIter");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

}
JNIEXPORT void Java_com_nmmedit_vm_VmTest_getDay0
        (JNIEnv *env, jclass jcls) {
    jclass clazz = env->FindClass("com/nmmedit/vm/VmTest");
    jmethodID getDayMethod = env->GetStaticMethodID(clazz, "getDay",
                                                    "()Lcom/nmmedit/vm/VmTest$Day;");

    jobject obj1 = env->CallStaticObjectMethod(clazz, getDayMethod);
    jobject obj2 = env->CallStaticObjectMethod(clazz, getDayMethod);
    jboolean sameObject = env->IsSameObject(obj1, obj2);
    ALOGD("ddd");
}
JNIEXPORT jobject Java_com_nmmedit_vm_VmTest_initBitmap0
        (JNIEnv *env, jobject jobj, jlong ptr, jint width, jint height) {
    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "initBitmap");
    if (dexCode == NULL) {
        return JNI_FALSE;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));
    regs[registersSize - 1] = height;
    regs[registersSize - 2] = width;
    SET_REGISTER_WIDE(4, ptr);

    regs[registersSize - 5] = (regptr_t) jobj;

    reg_flags[registersSize - 5] = 1;

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;

    const vmCode code = {
            .regs=regs,
            .reg_flags=reg_flags,
            .insns=insns,
            .insnsSize=insnsSize,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);


    return value.l;
}

JNIEXPORT void Java_com_nmmedit_vm_VmTest_byteBuffer0
        (JNIEnv *env, jclass clazz, jobject jbyteBuffer) {

    jclass bfClass = env->FindClass("java/nio/ByteBuffer");
    jmethodID methodId = env->GetMethodID(bfClass, "limit", "(I)Ljava/nio/Buffer;");
    jmethodID methodId2 = env->GetMethodID(bfClass, "limit", "(I)Ljava/nio/ByteBuffer;");
    ALOGD("methodid %p", methodId);
}

JNIEXPORT void Java_com_nmmedit_vm_VmUnitTest_passClass0
        (JNIEnv *env, jclass jcls, jclass clazz1) {

    jclass pJclass = env->FindClass("myclass/NoClassDef");
    if (pJclass == NULL) {
        return;
    }
    jmethodID methodId = env->GetStaticMethodID(pJclass, "getVersionxxx", "()I");
    jboolean i = env->ExceptionCheck();
//    env->CallStaticIntMethod(pJclass,methodId);
    ALOGD("clazz %p", pJclass);
//    JNINativeMethod nativeMethod = {.name="", .signature="(I)V", .fnPtr=NULL};
//    env->RegisterNatives(NULL,&nativeMethod,1);

}

JNIEXPORT jobject Java_com_nmmedit_vm_FieldTest_getObj0
        (JNIEnv *env, jclass jcls) {
    jclass fieldTestCls = env->FindClass("com/nmmedit/vm/FieldTest$B");
    jfieldID fieldId = env->GetStaticFieldID(fieldTestCls, "obj", "Ljava/lang/Object;");

    jobject objectField = env->GetStaticObjectField(fieldTestCls, fieldId);

    ALOGD("obj=%p", objectField);

    return objectField;
}

JNIEXPORT jint Java_com_nmmedit_vm_FieldTest_getInt0
        (JNIEnv *env, jclass jcls) {
    jclass clsA = env->FindClass("com/nmmedit/vm/FieldTest$A");
    jclass fieldTestCls = env->FindClass("com/nmmedit/vm/FieldTest$B");
    jfieldID fieldId = env->GetStaticFieldID(fieldTestCls, "INT", "I");

    jint objectField = env->GetStaticIntField(fieldTestCls, fieldId);

    jfieldID fieldIdA = env->GetStaticFieldID(clsA, "INT", "I");

    jint objectField2 = env->GetStaticIntField(clsA, fieldIdA);

    ALOGD("int=%d", objectField);

    return objectField;
}


JNIEXPORT void Java_com_nmmedit_vm_VmTest_throwNull0
        (JNIEnv *env, jclass clazz) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "throwNull");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .insns=insns,
            .insnsSize=insnsSize,
            .regs=regs,
            .reg_flags=reg_flags,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

}

JNIEXPORT jboolean Java_com_nmmedit_vm_VmTest_constString0
        (JNIEnv *env, jclass clazz) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "constString");
    if (dexCode == NULL) {
        return JNI_FALSE;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .insns=insns,
            .insnsSize=insnsSize,
            .regs=regs,
            .reg_flags=reg_flags,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return value.z;
}

static jstring myConstString(JNIEnv *env, u4 idx) {
    jclass cls = env->FindClass("com/nmmedit/vm/VmTest");
    jmethodID methodId = env->GetStaticMethodID(cls, "myConst", "(I)Ljava/lang/String;");
    return static_cast<jstring>(env->CallStaticObjectMethod(cls, methodId, (jint) idx));
}


JNIEXPORT jboolean Java_com_nmmedit_vm_VmTest_constString1
        (JNIEnv *env, jclass clazz) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "constString");
    if (dexCode == NULL) {
        return JNI_FALSE;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .insns=insns,
            .insnsSize=insnsSize,
            .regs=regs,
            .reg_flags=reg_flags,
            .triesHandlers=NULL
    };

    //替换constString方法
    dvmResolver.dvmConstantString = myConstString;

    jvalue value = vmInterpret(env, &code, &dvmResolver);

    return value.z;
}

static jobject Java_com_nmmedit_vm_TestJniRegisterNatives_getRealOwner
        (JNIEnv *env, jobject obj) {
    return nullptr;
}

JNIEXPORT void Java_com_nmmedit_vm_TestJniRegisterNatives_initClass
        (JNIEnv *env, jclass clazz) {
    jclass cls = env->FindClass("com/nmmedit/vm/TestJniRegisterNatives");
    JNINativeMethod methods = {
            .fnPtr=(void *) (Java_com_nmmedit_vm_TestJniRegisterNatives_getRealOwner),
            .name="getRealOwner",
            .signature="()Ljava/lang/Object;"
    };

    env->RegisterNatives(cls, &methods, 1);
}


//测试android6下调用jna函数问题
jstring constString2(JNIEnv *env, u4 idx) {
    const char *str = dexStringById(pDex, idx);
    ALOGD("resolve string %s", str);
    jstring pJstring = env->NewStringUTF(str);
//    jobject gstr = env->NewGlobalRef(pJstring);
    return pJstring;
}

JNIEXPORT void Java_com_nmmedit_vm_VmTest_callJna0
        (JNIEnv *env, jclass clazz) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "callJna");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .insns=insns,
            .insnsSize=insnsSize,
            .regs=regs,
            .reg_flags=reg_flags,
            .triesHandlers=NULL
    };

    //替换constString方法
    dvmResolver.dvmConstantString = constString2;

    jvalue value = vmInterpret(env, &code, &dvmResolver);
}


JNIEXPORT void Java_com_nmmedit_vm_VmTest_callJnaPassStr
        (JNIEnv *env, jclass clazz) {
    jclass testCls = env->FindClass("com/nmmedit/jna/TestJna");
    jfieldID fid = env->GetStaticFieldID(testCls, "INSTANCE", "Lcom/nmmedit/jna/TestJna;");
    jmethodID passStrMethodId = env->GetMethodID(testCls, "pass_str", "(Ljava/lang/String;)V");

    jobject instance = env->GetStaticObjectField(testCls, fid);
    //创建参数
    jstring arg = env->NewStringUTF("Hello world");
    jvalue args[1];
    args[0].l = arg;
    //调用pass_str
    env->CallVoidMethodA(instance, passStrMethodId, args);
}
JNIEXPORT void Java_com_nmmedit_vm_VmTest_agetOutOfBounds0
        (JNIEnv *env, jclass clazz) {

    const DexCode *dexCode = findDexCode("Lcom/nmmedit/vm/VmTest;", "agetOutOfBounds");
    if (dexCode == NULL) {
        return;
    }

    u2 registersSize = dexCode->registersSize;

    regptr_t *regs = (regptr_t *) calloc(registersSize, sizeof(regptr_t));
    u1 *reg_flags = (u1 *) calloc(registersSize, sizeof(u1));

    const u2 *insns = dexCode->insns;
    u4 insnsSize = dexCode->insnsSize;
    const vmCode code = {
            .insns=insns,
            .insnsSize=insnsSize,
            .regs=regs,
            .reg_flags=reg_flags,
            .triesHandlers=NULL
    };
    jvalue value = vmInterpret(env, &code, &dvmResolver);

}


#ifdef __cplusplus
}
#endif
