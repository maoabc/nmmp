#include "GlobalCache.h"
#include "ConstantPool.h"
#include <stdatomic.h>
#include <pthread.h>

//解析器示例,代码模板, 处理程序解析dex并根据它的结构体之类生成完整解析器

static const u1 gBaseStrPtr[] = {
        0x00,
        0x20, 0x00,
};

typedef struct {
    u4 off;
} StringId;
static const StringId gStringIds[] = {
        {.off=0x0000},
};
//ends string ids


typedef struct {
    u4 idx;
} TypeId;
static const TypeId gTypeIds[] = {
        {.idx=14},
};
//ends type ids


typedef struct {
    u4 idx;
} ClassId;
static const ClassId gClassIds[] = {
        {.idx=14},
};
//ends class name ids

typedef struct {
    u4 idx;
} SignatureId;
static const SignatureId gSignatureIds[] = {
        {.idx=655},
};
//ends method signature pool


typedef struct {
    u2 classIdx;
    u4 nameIdx;
    u2 typeIdx;
} FieldId;

static const FieldId gFieldIds[] = {
        {.classIdx=30, .nameIdx=489, .typeIdx=9},
};
//ends field id

static vmField gFields[59];

typedef struct {
    u2 classIdx;
    u4 nameIdx;
    u4 shortyIdx;
    u4 sigIdx;
} MethodId;

static const MethodId gMethodIds[] = {
        {.classIdx=9, .nameIdx=493, .shortyIdx=195, .sigIdx=46},
};
//ends method data

static vmMethod gMethods[199];

//字符串常量缓存,缓存索引,然后使用二分法查找到数组位置创建String对象
static const u4 gStringConstantIds[] = {};//使用处理程序初始化
static jstring gStringConstants[99];


static void resolver_init(JNIEnv *env) {
    memset(gFields, 0, sizeof(gFields));
    memset(gMethods, 0, sizeof(gMethods));
    memset(gStringConstants, 0, sizeof(gStringConstants));
}

#define STRING_BY_ID(_idx) ((const char *) (gBaseStrPtr + gStringIds[_idx].off))

#define STRING_BY_TYPE_ID(_idx) (STRING_BY_ID(gTypeIds[_idx].idx))

#define STRING_BY_CLASS_ID(_idx) (STRING_BY_ID(gClassIds[_idx].idx))

#define STRING_BY_SIGNATURE_ID(_idx) (STRING_BY_ID(gSignatureIds[_idx].idx))

#define FIND_CLASS_BY_NAME(_className)                          \
    clazz = (*env)->FindClass(env, _className);                 \
    if (clazz == NULL) {                                        \
        /*转换异常类型,保持和正常java抛一样异常*/                   \
        (*env)->ExceptionClear(env);                            \
        vmThrowNoClassDefFoundError(env, _className);           \
        return NULL;                                            \
    }


static void vmThrowNoClassDefFoundError(JNIEnv *env, const char *msg) {
    (*env)->ThrowNew(env, gVm.exNoClassDefFoundError, msg);
}

static void vmThrowNoSuchFieldError(JNIEnv *env, const char *msg) {
    (*env)->ThrowNew(env, gVm.exNoSuchFieldError, msg);
}

static void vmThrowNoSuchMethodError(JNIEnv *env, const char *msg) {
    (*env)->ThrowNew(env, gVm.exNoSuchMethodError, msg);
}

static const vmField *dvmResolveField(JNIEnv *env, u4 idx, bool isStatic) {
    vmField *field = &gFields[idx];
    if (field->fieldId == NULL) {
        FieldId fieldId = gFieldIds[idx];

        jclass clazz;
        FIND_CLASS_BY_NAME(STRING_BY_CLASS_ID(fieldId.classIdx));

        const char *type = STRING_BY_TYPE_ID(fieldId.typeIdx);
        const char *name = STRING_BY_ID(fieldId.nameIdx);

        field->classIdx = fieldId.classIdx;
        field->type = (*type == '[') ? 'L' : *type;

        //和方法解析同理,最后赋值fieldId
        jfieldID fid;
        if (isStatic) {
            fid = (*env)->GetStaticFieldID(env, clazz, name, type);
        } else {
            fid = (*env)->GetFieldID(env, clazz, name, type);
        }
        if (fid == NULL) {
            (*env)->DeleteLocalRef(env, clazz);

            (*env)->ExceptionClear(env);
            vmThrowNoSuchFieldError(env, name);
            return NULL;
        }
        (*env)->DeleteLocalRef(env, clazz);


        field->fieldId = fid;

    }
    return field;
}

static const vmMethod *dvmResolveMethod(JNIEnv *env, u4 idx, bool isStatic) {
    vmMethod *method = &gMethods[idx];
    if (method->methodId == NULL) {
        MethodId methodId = gMethodIds[idx];

        jclass clazz;
        FIND_CLASS_BY_NAME(STRING_BY_CLASS_ID(methodId.classIdx));

        method->shorty = STRING_BY_ID(methodId.shortyIdx);

        method->classIdx = methodId.classIdx;

        const char *name = STRING_BY_ID(methodId.nameIdx);
        const char *sig = STRING_BY_SIGNATURE_ID(methodId.sigIdx);

        jmethodID mid;
        if (isStatic) {
            mid = (*env)->GetStaticMethodID(env, clazz, name, sig);
        } else {
            mid = (*env)->GetMethodID(env, clazz, name, sig);
        }
        if (mid == NULL) {
            (*env)->DeleteLocalRef(env, clazz);

            (*env)->ExceptionClear(env);
            vmThrowNoSuchMethodError(env, name);
            return NULL;
        }
        (*env)->DeleteLocalRef(env, clazz);

        //只根据method->methodId判断是否需要解析,最后赋值为了防止结构体解析一半被其他线程使用从而导致错误
        //todo 可能需要加锁

        method->methodId = mid;

    }
    return method;
}

static pthread_mutex_t str_mutex = PTHREAD_MUTEX_INITIALIZER;

static jstring dvmConstantString(JNIEnv *env, u4 idx) {
    //先查找索引位置是否存在缓存,不用频繁创建string对象
    s4 i = binarySearch(gStringConstantIds, sizeof(gStringConstantIds) / sizeof(u4), idx);
    if (i >= 0) {
        if (gStringConstants[i] == NULL) {
            pthread_mutex_lock(&str_mutex);
            jstring str;
            if (gStringConstants[i] == NULL) {
                str = (*env)->NewStringUTF(env, STRING_BY_ID(idx));
                gStringConstants[i] = (*env)->NewGlobalRef(env, str);
            } else {
                str = (*env)->NewLocalRef(env, gStringConstants[i]);
            }
            pthread_mutex_unlock(&str_mutex);

            return str;
        } else {
            return (*env)->NewLocalRef(env, gStringConstants[i]);
        }
    }
    return (*env)->NewStringUTF(env, STRING_BY_ID(idx));
}

static const char *dvmResolveTypeUtf(JNIEnv *env, u4 idx) {
    return STRING_BY_TYPE_ID(idx);
}

static jclass dvmResolveClass(JNIEnv *env, u4 idx) {
    jclass clazz = getCacheClass(env, STRING_BY_TYPE_ID(idx));
    if (clazz != NULL) {
        return (jclass) (*env)->NewLocalRef(env, clazz);
    }

    FIND_CLASS_BY_NAME(STRING_BY_CLASS_ID(idx));

    return clazz;
}

static jclass dvmFindClass(JNIEnv *env, const char *type) {
    jclass clazz = getCacheClass(env, type);
    if (clazz != NULL) {
        return (jclass) (*env)->NewLocalRef(env, clazz);
    }
    if (*type == 'L') {
        char clazzName[42];
        size_t len = strlen(type);
        strncpy(clazzName, type + 1, len - 2);
        clazzName[len - 2] = 0;

        FIND_CLASS_BY_NAME(clazzName);

        return clazz;
    }

    FIND_CLASS_BY_NAME(type);

    return clazz;
}

static const vmResolver dvmResolver = {
        .dvmResolveField = dvmResolveField,
        .dvmResolveMethod = dvmResolveMethod,
        .dvmResolveTypeUtf = dvmResolveTypeUtf,
        .dvmResolveClass = dvmResolveClass,
        .dvmFindClass = dvmFindClass,
        .dvmConstantString = dvmConstantString,
};


static jclass retrieveClass(JNIEnv *env, jobject classLoader,
                            const char *className) {
    jmethodID findClass = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, classLoader),
                                              "loadClass",
                                              "(Ljava/lang/String;)Ljava/lang/Class;");
    jstring strClassName = (*env)->NewStringUTF(env, className);
    jclass classRetrieved = (jclass) (*env)->CallObjectMethod(env, classLoader, findClass,
                                                              strClassName);
    (*env)->DeleteLocalRef(env, strClassName);
    return classRetrieved;
}
