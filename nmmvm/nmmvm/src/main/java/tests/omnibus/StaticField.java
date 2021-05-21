package tests.omnibus;/*
 * Copyright (C) 2008 The Android Open Source Project
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

import tests.common.MyAssert;

public class StaticField {
    public static boolean mBoolean1, mBoolean2;
    public static byte mByte1, mByte2;
    public static char mChar1, mChar2;
    public static short mShort1, mShort2;
    public static int mInt1, mInt2;
    public static float mFloat1, mFloat2;
    public static long mLong1, mLong2;
    public static double mDouble1, mDouble2;
    public static volatile long mVolatileLong1, mVolatileLong2;

    public static void run() {
        assignFields();
        checkFields();
    }

    public static void assignFields() {
        System.out.println("StaticField assign...");
        mBoolean1 = true;
        mBoolean2 = false;
        mByte1 = 127;
        mByte2 = -128;
        mChar1 = 32767;
        mChar2 = 65535;
        mShort1 = 32767;
        mShort2 = -32768;
        mInt1 = 65537;
        mInt2 = -65537;
        mFloat1 = 3.1415f;
        mFloat2 = -1.0f / 0.0f;                // -inf
        mLong1 = 1234605616436508552L;     // 0x1122334455667788
        mLong2 = -1234605616436508552L;
        mDouble1 = 3.1415926535;
        mDouble2 = 1.0 / 0.0;               // +inf
        mVolatileLong1 = mLong1 - 1;
        mVolatileLong2 = mLong2 + 1;
    }

    public static void checkFields() {
        System.out.println("StaticField check...");
        MyAssert.myassert(mBoolean1);
        MyAssert.myassert(!mBoolean2);
        MyAssert.myassert(mByte1 == 127);
        MyAssert.myassert(mByte2 == -128);
        MyAssert.myassert(mChar1 == 32767);
        MyAssert.myassert(mChar2 == 65535);
        MyAssert.myassert(mShort1 == 32767);
        MyAssert.myassert(mShort2 == -32768);
        MyAssert.myassert(mInt1 == 65537);
        MyAssert.myassert(mInt2 == -65537);
        MyAssert.myassert(mFloat1 > 3.141f && mFloat2 < 3.142f);
        MyAssert.myassert(mFloat2 < mFloat1);
        MyAssert.myassert(mLong1 == 1234605616436508552L);
        MyAssert.myassert(mLong2 == -1234605616436508552L);
        MyAssert.myassert(mDouble1 > 3.141592653 && mDouble1 < 3.141592654);
        MyAssert.myassert(mDouble2 > mDouble1);
        MyAssert.myassert(mVolatileLong1 == 1234605616436508551L);
        MyAssert.myassert(mVolatileLong2 == -1234605616436508551L);
    }
}
