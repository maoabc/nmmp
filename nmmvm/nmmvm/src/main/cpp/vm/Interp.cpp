//
// Created by mao on 20-8-17.
//

#include <cstdlib>
#include <cstring>
#include "Interp.h"
#include "DexOpcodes.h"
#include "Exception.h"


/*
 * Find the matching case.  Returns the offset to the handler instructions.
 *
 * Returns 3 if we don't find a match (it's the size of the packed-switch
 * instruction).
 */
s4 dvmInterpHandlePackedSwitch(JNIEnv *env, const u2 *switchData, s4 testVal) {
    const int kInstrLen = 3;

    /*
     * Packed switch data format:
     *  ushort ident = 0x0100   magic value
     *  ushort size             number of entries in the table
     *  int first_key           first (and lowest) switch case value
     *  int targets[size]       branch targets, relative to switch opcode
     *
     * Total size is (4+size*2) 16-bit code units.
     */
    if (*switchData++ != kPackedSwitchSignature) {
        /* should have been caught by verifier */
        dvmThrowInternalError(env, "bad packed switch magic");
        return kInstrLen;
    }

    u2 size = *switchData++;
    assert(size > 0);

    s4 firstKey = *switchData++;
    firstKey |= (*switchData++) << 16;

    int index = testVal - firstKey;
    if (index < 0 || index >= size) {
        LOGVV("Value %d not found in switch (%d-%d)",
              testVal, firstKey, firstKey + size - 1);
        return kInstrLen;
    }

    /* The entries are guaranteed to be aligned on a 32-bit boundary;
     * we can treat them as a native int array.
     */
    const s4 *entries = (const s4 *) switchData;

    assert(index >= 0 && index < size);
    LOGVV("Value %d found in slot %d (goto 0x%02x)",
          testVal, index,
          s4FromSwitchData(&entries[index]));
    return s4FromSwitchData(&entries[index]);
}

/*
 * Find the matching case.  Returns the offset to the handler instructions.
 *
 * Returns 3 if we don't find a match (it's the size of the sparse-switch
 * instruction).
 */
s4 dvmInterpHandleSparseSwitch(JNIEnv *env, const u2 *switchData, s4 testVal) {
    const int kInstrLen = 3;
    u2 size;
    const s4 *keys;
    const s4 *entries;

    /*
     * Sparse switch data format:
     *  ushort ident = 0x0200   magic value
     *  ushort size             number of entries in the table; > 0
     *  int keys[size]          keys, sorted low-to-high; 32-bit aligned
     *  int targets[size]       branch targets, relative to switch opcode
     *
     * Total size is (2+size*4) 16-bit code units.
     */

    if (*switchData++ != kSparseSwitchSignature) {
        /* should have been caught by verifier */
        dvmThrowInternalError(env, "bad sparse switch magic");
        return kInstrLen;
    }

    size = *switchData++;
    assert(size > 0);

    /* The keys are guaranteed to be aligned on a 32-bit boundary;
     * we can treat them as a native int array.
     */
    keys = (const s4 *) switchData;

    /* The entries are guaranteed to be aligned on a 32-bit boundary;
     * we can treat them as a native int array.
     */
    entries = keys + size;

    /*
     * Binary-search through the array of keys, which are guaranteed to
     * be sorted low-to-high.
     */
    int lo = 0;
    int hi = size - 1;
    while (lo <= hi) {
        int mid = (lo + hi) >> 1;

        s4 foundVal = s4FromSwitchData(&keys[mid]);
        if (testVal < foundVal) {
            hi = mid - 1;
        } else if (testVal > foundVal) {
            lo = mid + 1;
        } else {
            LOGVV("Value %d found in entry %d (goto 0x%02x)",
                  testVal, mid, s4FromSwitchData(&entries[mid]));
            return s4FromSwitchData(&entries[mid]);
        }
    }

    LOGVV("Value %d not found in switch", testVal);
    return kInstrLen;
}


/*
 * Fill the array with predefined constant values.
 *
 * Returns true if job is completed, otherwise false to indicate that
 * an exception has been thrown.
 */
bool dvmInterpHandleFillArrayData(JNIEnv *env, jarray arrayObj, const u2 *arrayData) {
    u2 width;
    u4 size;

    if (arrayObj == NULL) {
        dvmThrowNullPointerException(env, NULL);
        return false;
    }

    /*
     * Array data table format:
     *  ushort ident = 0x0300   magic value
     *  ushort width            width of each element in the table
     *  uint   size             number of elements in the table
     *  ubyte  data[size*width] table of data values (may contain a single-byte
     *                          padding at the end)
     *
     * Total size is 4+(width * size + 1)/2 16-bit code units.
     */
    if (arrayData[0] != kArrayDataSignature) {
        dvmThrowInternalError(env, "bad array data magic");
        return false;
    }

    width = arrayData[1];
    size = arrayData[2] | (((u4) arrayData[3]) << 16);

    jsize arrayLength = env->GetArrayLength(arrayObj);
    if (size > arrayLength) {
        dvmThrowArrayIndexOutOfBoundsException(env, arrayLength, size);
        return false;
    }
    void *arrp;
    switch (width) {
        case 1:
            arrp = env->GetPrimitiveArrayCritical(arrayObj, NULL);
            memcpy(arrp, &arrayData[4], size * 1);
            env->ReleasePrimitiveArrayCritical(arrayObj, arrp, 0);
            break;
        case 2:
            arrp = env->GetPrimitiveArrayCritical(arrayObj, NULL);
            memcpy(arrp, &arrayData[4], size * 2);
            env->ReleasePrimitiveArrayCritical(arrayObj, arrp, 0);
            break;
        case 4:
            arrp = env->GetPrimitiveArrayCritical(arrayObj, NULL);
            memcpy(arrp, &arrayData[4], size * 4);
            env->ReleasePrimitiveArrayCritical(arrayObj, arrp, 0);
            break;
        case 8:
            arrp = env->GetPrimitiveArrayCritical(arrayObj, NULL);
            memcpy(arrp, &arrayData[4], size * 8);
            env->ReleasePrimitiveArrayCritical(arrayObj, arrp, 0);
            break;
        default:
            ALOGV("Unexpected width %d in copySwappedArrayData", width);
            abort();
            break;
    }
    return true;
}
