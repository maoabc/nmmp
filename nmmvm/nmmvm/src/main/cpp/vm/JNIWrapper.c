//
// Created by mao on 2023/8/27.
//
#include "JNIWrapper.h"

static jint Throw(JNIEnv *env, jthrowable throwable) {
    return (*env)->Throw(env, throwable);
}

static jthrowable ExceptionOccurred(JNIEnv *env) {
    return (*env)->ExceptionOccurred(env);
}

static void ExceptionClear(JNIEnv *env) {
    (*env)->ExceptionClear(env);
}

static void DeleteLocalRef(JNIEnv *env, jobject obj) {
    (*env)->DeleteLocalRef(env, obj);
}

static jboolean IsSameObject(JNIEnv *env, jobject obj1, jobject obj2) {
    return (*env)->IsSameObject(env, obj1, obj2);
}

static jobject NewLocalRef(JNIEnv *env, jobject obj) {
    return (*env)->NewLocalRef(env, obj);
}

static jobject AllocObject(JNIEnv *env, jclass cls) {
    return (*env)->AllocObject(env, cls);
}

static jboolean IsInstanceOf(JNIEnv *env, jobject obj, jclass cls) {
    return (*env)->IsInstanceOf(env, obj, cls);
}

static jobject CallObjectMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallObjectMethodA(env, obj, methodId, v);
}

static jboolean CallBooleanMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallBooleanMethodA(env, obj, methodId, v);
}

static jbyte CallByteMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallByteMethodA(env, obj, methodId, v);
}

static jchar CallCharMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallCharMethodA(env, obj, methodId, v);
}

static jshort CallShortMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallShortMethodA(env, obj, methodId, v);
}

static jint CallIntMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallIntMethodA(env, obj, methodId, v);
}

static jlong CallLongMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallLongMethodA(env, obj, methodId, v);
}

static jfloat CallFloatMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallFloatMethodA(env, obj, methodId, v);
}

static jdouble CallDoubleMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    return (*env)->CallDoubleMethodA(env, obj, methodId, v);
}

static void CallVoidMethodA(JNIEnv *env, jobject obj, jmethodID methodId, const jvalue *v) {
    (*env)->CallVoidMethodA(env, obj, methodId, v);
}

static jobject CallNonvirtualObjectMethodA(JNIEnv *env, jobject obj, jclass cls,
                                           jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualObjectMethodA(env, obj, cls, methodId, v);
}

static jboolean CallNonvirtualBooleanMethodA(JNIEnv *env, jobject obj, jclass cls,
                                             jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualBooleanMethodA(env, obj, cls, methodId, v);
}

static jbyte CallNonvirtualByteMethodA(JNIEnv *env, jobject obj, jclass cls,
                                       jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualByteMethodA(env, obj, cls, methodId, v);
}

static jchar CallNonvirtualCharMethodA(JNIEnv *env, jobject obj, jclass cls,
                                       jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualCharMethodA(env, obj, cls, methodId, v);
}

static jshort CallNonvirtualShortMethodA(JNIEnv *env, jobject obj, jclass cls,
                                         jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualShortMethodA(env, obj, cls, methodId, v);
}

static jint CallNonvirtualIntMethodA(JNIEnv *env, jobject obj, jclass cls,
                                     jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualIntMethodA(env, obj, cls, methodId, v);
}

static jlong CallNonvirtualLongMethodA(JNIEnv *env, jobject obj, jclass cls,
                                       jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualLongMethodA(env, obj, cls, methodId, v);
}

static jfloat CallNonvirtualFloatMethodA(JNIEnv *env, jobject obj, jclass cls,
                                         jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualFloatMethodA(env, obj, cls, methodId, v);
}

static jdouble CallNonvirtualDoubleMethodA(JNIEnv *env, jobject obj, jclass cls,
                                           jmethodID methodId, const jvalue *v) {
    return (*env)->CallNonvirtualDoubleMethodA(env, obj, cls, methodId, v);
}

static void CallNonvirtualVoidMethodA(JNIEnv *env, jobject obj, jclass cls,
                                      jmethodID methodId, const jvalue *v) {
    (*env)->CallNonvirtualVoidMethodA(env, obj, cls, methodId, v);
}

static jobject GetObjectField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetObjectField(env, obj, fieldId);
}

static jboolean GetBooleanField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetBooleanField(env, obj, fieldId);
}

static jbyte GetByteField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetByteField(env, obj, fieldId);
}

static jchar GetCharField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetCharField(env, obj, fieldId);
}

static jshort GetShortField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetShortField(env, obj, fieldId);
}

static jint GetIntField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetIntField(env, obj, fieldId);
}

static jlong GetLongField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetLongField(env, obj, fieldId);
}

static jfloat GetFloatField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetFloatField(env, obj, fieldId);
}

static jdouble GetDoubleField(JNIEnv *env, jobject obj, jfieldID fieldId) {
    return (*env)->GetDoubleField(env, obj, fieldId);
}

static void SetObjectField(JNIEnv *env, jobject obj, jfieldID fieldId, jobject o) {
    (*env)->SetObjectField(env, obj, fieldId, o);
}

static void SetBooleanField(JNIEnv *env, jobject obj, jfieldID fieldId, jboolean b) {
    (*env)->SetBooleanField(env, obj, fieldId, b);
}

static void SetByteField(JNIEnv *env, jobject obj, jfieldID fieldId, jbyte b) {
    (*env)->SetByteField(env, obj, fieldId, b);
}

static void SetCharField(JNIEnv *env, jobject obj, jfieldID fieldId, jchar ch) {
    (*env)->SetCharField(env, obj, fieldId, ch);
}

static void SetShortField(JNIEnv *env, jobject obj, jfieldID fieldId, jshort s) {
    (*env)->SetShortField(env, obj, fieldId, s);
}

static void SetIntField(JNIEnv *env, jobject obj, jfieldID fieldId, jint i) {
    (*env)->SetIntField(env, obj, fieldId, i);
}

static void SetLongField(JNIEnv *env, jobject obj, jfieldID fieldId, jlong l) {
    (*env)->SetLongField(env, obj, fieldId, l);
}

static void SetFloatField(JNIEnv *env, jobject obj, jfieldID fieldId, jfloat f) {
    (*env)->SetFloatField(env, obj, fieldId, f);
}

static void SetDoubleField(JNIEnv *env, jobject obj, jfieldID fieldId, jdouble d) {
    (*env)->SetDoubleField(env, obj, fieldId, d);
}


static jobject
CallStaticObjectMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticObjectMethodA(env, cls, methodId, v);
}

static jboolean
CallStaticBooleanMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticByteMethodA(env, cls, methodId, v);
}

static jbyte CallStaticByteMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticByteMethodA(env, cls, methodId, v);
}

static jchar CallStaticCharMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticCharMethodA(env, cls, methodId, v);
}

static jshort CallStaticShortMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticShortMethodA(env, cls, methodId, v);
}

static jint CallStaticIntMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticIntMethodA(env, cls, methodId, v);
}

static jlong CallStaticLongMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticLongMethodA(env, cls, methodId, v);
}

static jfloat CallStaticFloatMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticFloatMethodA(env, cls, methodId, v);
}

static jdouble
CallStaticDoubleMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    return (*env)->CallStaticDoubleMethodA(env, cls, methodId, v);
}

static void CallStaticVoidMethodA(JNIEnv *env, jclass cls, jmethodID methodId, const jvalue *v) {
    (*env)->CallStaticVoidMethodA(env, cls, methodId, v);
}

static jobject GetStaticObjectField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticObjectField(env, cls, fieldId);
}

static jboolean GetStaticBooleanField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticBooleanField(env, cls, fieldId);
}

static jbyte GetStaticByteField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticByteField(env, cls, fieldId);
}

static jchar GetStaticCharField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticCharField(env, cls, fieldId);
}

static jshort GetStaticShortField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticShortField(env, cls, fieldId);
}

static jint GetStaticIntField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticIntField(env, cls, fieldId);
}

static jlong GetStaticLongField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticLongField(env, cls, fieldId);
}

static jfloat GetStaticFloatField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticFloatField(env, cls, fieldId);
}

static jdouble GetStaticDoubleField(JNIEnv *env, jclass cls, jfieldID fieldId) {
    return (*env)->GetStaticDoubleField(env, cls, fieldId);
}

static void SetStaticObjectField(JNIEnv *env, jclass cls, jfieldID fieldId, jobject o) {
    (*env)->SetStaticObjectField(env, cls, fieldId, o);
}

static void SetStaticBooleanField(JNIEnv *env, jclass cls, jfieldID fieldId, jboolean b) {
    (*env)->SetStaticBooleanField(env, cls, fieldId, b);
}

static void SetStaticByteField(JNIEnv *env, jclass cls, jfieldID fieldId, jbyte b) {
    (*env)->SetStaticByteField(env, cls, fieldId, b);
}

static void SetStaticCharField(JNIEnv *env, jclass cls, jfieldID fieldId, jchar ch) {
    (*env)->SetStaticCharField(env, cls, fieldId, ch);
}

static void SetStaticShortField(JNIEnv *env, jclass cls, jfieldID fieldId, jshort s) {
    (*env)->SetStaticShortField(env, cls, fieldId, s);
}

static void SetStaticIntField(JNIEnv *env, jclass cls, jfieldID fieldId, jint i) {
    (*env)->SetStaticIntField(env, cls, fieldId, i);
}

static void SetStaticLongField(JNIEnv *env, jclass cls, jfieldID fieldId, jlong l) {
    (*env)->SetStaticLongField(env, cls, fieldId, l);
}

static void SetStaticFloatField(JNIEnv *env, jclass cls, jfieldID fieldId, jfloat f) {
    (*env)->SetStaticFloatField(env, cls, fieldId, f);
}

static void SetStaticDoubleField(JNIEnv *env, jclass cls, jfieldID fieldId, jdouble d) {
    (*env)->SetStaticDoubleField(env, cls, fieldId, d);
}

static jsize GetArrayLength(JNIEnv *env, jarray array) {
    return (*env)->GetArrayLength(env, array);
}

static jobjectArray NewObjectArray(JNIEnv *env, jsize size, jclass cls, jobject obj) {
    return (*env)->NewObjectArray(env, size, cls, obj);
}

static jobject GetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize index) {
    return (*env)->GetObjectArrayElement(env, array, index);
}

static void SetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize idx, jobject o) {
    (*env)->SetObjectArrayElement(env, array, idx, o);
}

static jbooleanArray NewBooleanArray(JNIEnv *env, jsize length) {
    return (*env)->NewBooleanArray(env, length);
}

static jbyteArray NewByteArray(JNIEnv *env, jsize length) {
    return (*env)->NewByteArray(env, length);
}

static jcharArray NewCharArray(JNIEnv *env, jsize length) {
    return (*env)->NewCharArray(env, length);
}

static jshortArray NewShortArray(JNIEnv *env, jsize length) {
    return (*env)->NewShortArray(env, length);
}

static jintArray NewIntArray(JNIEnv *env, jsize length) {
    return (*env)->NewIntArray(env, length);
}

static jlongArray NewLongArray(JNIEnv *env, jsize length) {
    return (*env)->NewLongArray(env, length);
}

static jfloatArray NewFloatArray(JNIEnv *env, jsize length) {
    return (*env)->NewFloatArray(env, length);
}

static jdoubleArray NewDoubleArray(JNIEnv *env, jsize length) {
    return (*env)->NewDoubleArray(env, length);
}

static void GetBooleanArrayRegion(JNIEnv *env, jbooleanArray arr,
                                  jsize start, jsize len, jboolean *b) {
    (*env)->GetBooleanArrayRegion(env, arr, start, len, b);
}

static void GetByteArrayRegion(JNIEnv *env, jbyteArray arr,
                               jsize start, jsize len, jbyte *b) {
    (*env)->GetByteArrayRegion(env, arr, start, len, b);
}

static void GetCharArrayRegion(JNIEnv *env, jcharArray arr,
                               jsize start, jsize len, jchar *buf) {
    (*env)->GetCharArrayRegion(env, arr, start, len, buf);
}

static void GetShortArrayRegion(JNIEnv *env, jshortArray arr,
                                jsize start, jsize len, jshort *buf) {
    (*env)->GetShortArrayRegion(env, arr, start, len, buf);
}

static void GetIntArrayRegion(JNIEnv *env, jintArray arr,
                              jsize start, jsize len, jint *buf) {
    (*env)->GetIntArrayRegion(env, arr, start, len, buf);
}

static void GetLongArrayRegion(JNIEnv *env, jlongArray arr,
                               jsize start, jsize len, jlong *buf) {
    (*env)->GetLongArrayRegion(env, arr, start, len, buf);
}

static void GetFloatArrayRegion(JNIEnv *env, jfloatArray arr,
                                jsize start, jsize len, jfloat *buf) {
    (*env)->GetFloatArrayRegion(env, arr, start, len, buf);
}

static void GetDoubleArrayRegion(JNIEnv *env, jdoubleArray arr,
                                 jsize start, jsize len, jdouble *buf) {
    (*env)->GetDoubleArrayRegion(env, arr, start, len, buf);
}

static void SetBooleanArrayRegion(JNIEnv *env, jbooleanArray arr,
                                  jsize start, jsize len, const jboolean *buf) {
    (*env)->SetBooleanArrayRegion(env, arr, start, len, buf);
}

static void SetByteArrayRegion(JNIEnv *env, jbyteArray arr,
                               jsize start, jsize len, const jbyte *buf) {
    (*env)->SetByteArrayRegion(env, arr, start, len, buf);
}

static void SetCharArrayRegion(JNIEnv *env, jcharArray arr,
                               jsize start, jsize len, const jchar *buf) {
    (*env)->SetCharArrayRegion(env, arr, start, len, buf);
}

static void SetShortArrayRegion(JNIEnv *env, jshortArray arr,
                                jsize start, jsize len, const jshort *buf) {
    (*env)->SetShortArrayRegion(env, arr, start, len, buf);
}

static void SetIntArrayRegion(JNIEnv *env, jintArray arr,
                              jsize start, jsize len, const jint *buf) {
    (*env)->SetIntArrayRegion(env, arr, start, len, buf);
}

static void SetLongArrayRegion(JNIEnv *env, jlongArray arr,
                               jsize start, jsize len, const jlong *buf) {
    (*env)->SetLongArrayRegion(env, arr, start, len, buf);
}

static void SetFloatArrayRegion(JNIEnv *env, jfloatArray arr,
                                jsize start, jsize len, const jfloat *buf) {
    (*env)->SetFloatArrayRegion(env, arr, start, len, buf);
}

static void SetDoubleArrayRegion(JNIEnv *env, jdoubleArray arr,
                                 jsize start, jsize len, const jdouble *buf) {
    (*env)->SetDoubleArrayRegion(env, arr, start, len, buf);
}

static jint MonitorEnter(JNIEnv *env, jobject obj) {
    return (*env)->MonitorEnter(env, obj);
}

static jint MonitorExit(JNIEnv *env, jobject obj) {
    return (*env)->MonitorExit(env, obj);
}

static void *GetPrimitiveArrayCritical(JNIEnv *env, jarray arr, jboolean *isCopy) {
    return (*env)->GetPrimitiveArrayCritical(env, arr, isCopy);
}

static void ReleasePrimitiveArrayCritical(JNIEnv *env, jarray arr, void *carr, jint mode) {
    (*env)->ReleasePrimitiveArrayCritical(env, arr, carr, mode);
}

static jboolean ExceptionCheck(JNIEnv *env) {
    return (*env)->ExceptionCheck(env);
}


static const JNIWrapper wrapperImp = {
        .Throw=Throw,
        .ExceptionOccurred=ExceptionOccurred,
        .ExceptionClear=ExceptionClear,
        .DeleteLocalRef=DeleteLocalRef,
        .IsSameObject=IsSameObject,
        .NewLocalRef=NewLocalRef,
        .AllocObject=AllocObject,
        .IsInstanceOf=IsInstanceOf,
        .CallObjectMethodA=CallObjectMethodA,
        .CallBooleanMethodA=CallBooleanMethodA,
        .CallByteMethodA=CallByteMethodA,
        .CallCharMethodA=CallCharMethodA,
        .CallShortMethodA=CallShortMethodA,
        .CallIntMethodA=CallIntMethodA,
        .CallLongMethodA=CallLongMethodA,
        .CallFloatMethodA=CallFloatMethodA,
        .CallDoubleMethodA=CallDoubleMethodA,
        .CallVoidMethodA=CallVoidMethodA,
        .CallNonvirtualObjectMethodA=CallNonvirtualObjectMethodA,
        .CallNonvirtualBooleanMethodA=CallNonvirtualBooleanMethodA,
        .CallNonvirtualByteMethodA=CallNonvirtualByteMethodA,
        .CallNonvirtualCharMethodA=CallNonvirtualCharMethodA,
        .CallNonvirtualShortMethodA=CallNonvirtualShortMethodA,
        .CallNonvirtualIntMethodA=CallNonvirtualIntMethodA,
        .CallNonvirtualLongMethodA=CallNonvirtualLongMethodA,
        .CallNonvirtualFloatMethodA=CallNonvirtualFloatMethodA,
        .CallNonvirtualDoubleMethodA=CallNonvirtualDoubleMethodA,
        .CallNonvirtualVoidMethodA=CallNonvirtualVoidMethodA,
        .GetObjectField=GetObjectField,
        .GetBooleanField=GetBooleanField,
        .GetByteField=GetByteField,
        .GetCharField=GetCharField,
        .GetShortField=GetShortField,
        .GetIntField=GetIntField,
        .GetLongField=GetLongField,
        .GetFloatField=GetFloatField,
        .GetDoubleField=GetDoubleField,
        .SetObjectField=SetObjectField,
        .SetBooleanField=SetBooleanField,
        .SetByteField=SetByteField,
        .SetCharField=SetCharField,
        .SetShortField=SetShortField,
        .SetIntField=SetIntField,
        .SetLongField=SetLongField,
        .SetFloatField=SetFloatField,
        .SetDoubleField=SetDoubleField,
        .CallStaticObjectMethodA=CallStaticObjectMethodA,
        .CallStaticBooleanMethodA=CallStaticBooleanMethodA,
        .CallStaticByteMethodA=CallStaticByteMethodA,
        .CallStaticCharMethodA=CallStaticCharMethodA,
        .CallStaticShortMethodA=CallStaticShortMethodA,
        .CallStaticIntMethodA=CallStaticIntMethodA,
        .CallStaticLongMethodA=CallStaticLongMethodA,
        .CallStaticFloatMethodA=CallStaticFloatMethodA,
        .CallStaticDoubleMethodA=CallStaticDoubleMethodA,
        .CallStaticVoidMethodA=CallStaticVoidMethodA,
        .GetStaticObjectField=GetStaticObjectField,
        .GetStaticBooleanField=GetStaticBooleanField,
        .GetStaticByteField=GetStaticByteField,
        .GetStaticCharField=GetStaticCharField,
        .GetStaticShortField=GetStaticShortField,
        .GetStaticIntField=GetStaticIntField,
        .GetStaticLongField=GetStaticLongField,
        .GetStaticFloatField=GetStaticFloatField,
        .GetStaticDoubleField=GetStaticDoubleField,
        .SetStaticObjectField=SetStaticObjectField,
        .SetStaticBooleanField=SetStaticBooleanField,
        .SetStaticByteField=SetStaticByteField,
        .SetStaticCharField=SetStaticCharField,
        .SetStaticShortField=SetStaticShortField,
        .SetStaticIntField=SetStaticIntField,
        .SetStaticLongField=SetStaticLongField,
        .SetStaticFloatField=SetStaticFloatField,
        .SetStaticDoubleField=SetStaticDoubleField,
        .GetArrayLength=GetArrayLength,
        .NewObjectArray=NewObjectArray,
        .GetObjectArrayElement=GetObjectArrayElement,
        .SetObjectArrayElement=SetObjectArrayElement,
        .NewBooleanArray=NewBooleanArray,
        .NewByteArray=NewByteArray,
        .NewCharArray=NewCharArray,
        .NewShortArray=NewShortArray,
        .NewIntArray=NewIntArray,
        .NewLongArray=NewLongArray,
        .NewFloatArray=NewFloatArray,
        .NewDoubleArray=NewDoubleArray,
        .GetBooleanArrayRegion=GetBooleanArrayRegion,
        .GetByteArrayRegion=GetByteArrayRegion,
        .GetCharArrayRegion=GetCharArrayRegion,
        .GetShortArrayRegion=GetShortArrayRegion,
        .GetIntArrayRegion=GetIntArrayRegion,
        .GetLongArrayRegion=GetLongArrayRegion,
        .GetFloatArrayRegion=GetFloatArrayRegion,
        .GetDoubleArrayRegion=GetDoubleArrayRegion,
        .SetBooleanArrayRegion=SetBooleanArrayRegion,
        .SetByteArrayRegion=SetByteArrayRegion,
        .SetCharArrayRegion=SetCharArrayRegion,
        .SetShortArrayRegion=SetShortArrayRegion,
        .SetIntArrayRegion=SetIntArrayRegion,
        .SetLongArrayRegion=SetLongArrayRegion,
        .SetFloatArrayRegion=SetFloatArrayRegion,
        .SetDoubleArrayRegion=SetDoubleArrayRegion,
        .MonitorEnter=MonitorEnter,
        .MonitorExit=MonitorExit,
        .GetPrimitiveArrayCritical=GetPrimitiveArrayCritical,
        .ReleasePrimitiveArrayCritical=ReleasePrimitiveArrayCritical,
        .ExceptionCheck=ExceptionCheck,
};


const JNIWrapper * getJNIWrapper(){
    return &wrapperImp;
}
