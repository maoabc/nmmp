//
// Created by mao on 20-8-17.
//

#ifndef DEX_EDITOR_EXCEPTION_H
#define DEX_EDITOR_EXCEPTION_H

#include <jni.h>
#include "vm.h"
#include "DexCatch.h"


void dvmThrowArithmeticException(JNIEnv *env, const char *msg);

void dvmThrowInternalError(JNIEnv *env, const char *msg);

void dvmThrowArrayIndexOutOfBoundsException(JNIEnv *env, int length, int index);

void dvmThrowNegativeArraySizeException(JNIEnv *env, s4 size);

void dvmThrowClassCastException(JNIEnv *env, jobject obj, jclass clazz2);

void dvmThrowRuntimeException(JNIEnv *env, const char *msg);

void dvmThrowNullPointerException(JNIEnv *env, const char *msg);

void dvmThrowClassNotFoundException(JNIEnv *env, const char *name);

/*
 * Like dvmThrowException, but takes printf-style args for the message.
 */
void dvmThrowExceptionFmtV(JNIEnv *env, const char *name,
                           const char *fmt, va_list args);


inline void dvmThrowExceptionFmt(JNIEnv *env, const char *name,
                                 const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    dvmThrowExceptionFmtV(env, name, fmt, args);
    va_end(args);
}


int dvmFindCatchBlock(JNIEnv *env, const vmResolver *resolver, int relPc, jthrowable exception,
                      TryCatchHandler *pHandler);

#endif //DEX_EDITOR_EXCEPTION_H
