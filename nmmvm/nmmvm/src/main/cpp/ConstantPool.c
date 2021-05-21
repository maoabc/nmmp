//
// Created by mao on 20-9-11.
//

#include "ConstantPool.h"

s4 binarySearch(const u4 *pool, int poolSize,
                u4 idx) {
    // Note: Signed type is important for max and min.
    int min = 0;
    int max = poolSize - 1;

    while (max >= min) {
        //数据量不会超过最大int, 不考虑溢出
        int guess = (min + max) >> 1;
        const u4 off = pool[guess];

        if (idx < off) {
            max = guess - 1;
            continue;
        }
        if (idx > off) {
            min = guess + 1;
            continue;
        }

        // We have a winner!
        return guess;
    }

    // No match.
    return -1;
}
