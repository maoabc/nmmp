/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.zipflinger;

class Ints {
    public static final long USHRT_MAX = 65_535L;
    public static final long UINT_MAX = 0xFF_FF_FF_FFL;

    static long uintToLong(int i) {
        return i & 0xFF_FF_FF_FFL;
    }

    static int ushortToInt(short i) {
        return i & 0xFF_FF;
    }

    static int longToUint(long i) {
        if ((i & 0xFF_FF_FF_FF_00_00_00_00L) != 0) {
            throw new IllegalStateException("Long cannot fit in uint");
        }
        return (int) i;
    }

    static short intToUshort(int i) {
        if ((i & 0xFF_FF_00_00) != 0) {
            throw new IllegalStateException("Int cannot fit in ushort");
        }
        return (short) i;
    }

    static short longToUshort(long i) {
        if ((i & 0xFF_FF_FF_FF_FF_FF_00_00L) != 0) {
            throw new IllegalStateException("long cannot fit in ushort");
        }
        return (short) i;
    }

    public static long ulongToLong(long i) {
        if ((i & 0x80_00_00_00_00_00_00_00L) != 0) {
            throw new IllegalStateException("ulong cannot fit in long");
        }
        return i;
    }

    private Ints() {}
}
