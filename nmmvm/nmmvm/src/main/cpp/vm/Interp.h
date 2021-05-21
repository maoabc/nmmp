//
// Created by mao on 20-8-17.
//

#ifndef DEX_EDITOR_INTERP_H
#define DEX_EDITOR_INTERP_H

#include <jni.h>
#include "Common.h"
s4 dvmInterpHandlePackedSwitch(JNIEnv *env, const u2 *switchData, s4 testVal);

s4 dvmInterpHandleSparseSwitch(JNIEnv *env, const u2 *switchData, s4 testVal);

bool dvmInterpHandleFillArrayData(JNIEnv *env, jarray arrayObj, const u2 *arrayData);
/*
 * Construct an s4 from two consecutive half-words of switch data.
 * This needs to check endianness because the DEX optimizer only swaps
 * half-words in instruction stream.
 *
 * "switchData" must be 32-bit aligned.
 */
static inline s4 s4FromSwitchData(const void *switchData) {
    return *(s4 *) switchData;
}
#endif //DEX_EDITOR_INTERP_H
