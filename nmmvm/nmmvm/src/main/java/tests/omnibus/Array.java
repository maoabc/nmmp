package tests.omnibus;// Copyright 2008 The Android Open Source Project


import tests.common.MyAssert;

/**
 * Exercise arrays.
 */
public class Array {

    /*
     * Verify array contents.
     */
    static void checkBytes(byte[] bytes) {
        MyAssert.myassert(bytes[0] == 0);
        MyAssert.myassert(bytes[1] == -1);
        MyAssert.myassert(bytes[2] == -2);
        MyAssert.myassert(bytes[3] == -3);
        MyAssert.myassert(bytes[4] == -4);
    }

    static void checkShorts(short[] shorts) {
        MyAssert.myassert(shorts[0] == 20);
        MyAssert.myassert(shorts[1] == 10);
        MyAssert.myassert(shorts[2] == 0);
        MyAssert.myassert(shorts[3] == -10);
        MyAssert.myassert(shorts[4] == -20);
    }

    static void checkChars(char[] chars) {
        MyAssert.myassert(chars[0] == 40000);
        MyAssert.myassert(chars[1] == 40001);
        MyAssert.myassert(chars[2] == 40002);
        MyAssert.myassert(chars[3] == 40003);
        MyAssert.myassert(chars[4] == 40004);
    }

    static void checkInts(int[] ints) {
        MyAssert.myassert(ints[0] == 70000);
        MyAssert.myassert(ints[1] == 70001);
        MyAssert.myassert(ints[2] == 70002);
        MyAssert.myassert(ints[3] == 70003);
        MyAssert.myassert(ints[4] == 70004);
    }

    static void checkBooleans(boolean[] booleans) {
        MyAssert.myassert(booleans[0]);
        MyAssert.myassert(booleans[1]);
        MyAssert.myassert(!booleans[2]);
        MyAssert.myassert(booleans[3]);
        MyAssert.myassert(!booleans[4]);
    }

    static void checkFloats(float[] floats) {
        MyAssert.myassert(floats[0] == -1.5);
        MyAssert.myassert(floats[1] == -0.5);
        MyAssert.myassert(floats[2] == 0.0);
        MyAssert.myassert(floats[3] == 0.5);
        MyAssert.myassert(floats[4] == 1.5);
    }

    static void checkLongs(long[] longs) {
        MyAssert.myassert(longs[0] == 0x1122334455667788L);
        MyAssert.myassert(longs[1] == 0x8877665544332211L);
        MyAssert.myassert(longs[2] == 0L);
        MyAssert.myassert(longs[3] == 1L);
        MyAssert.myassert(longs[4] == -1L);
    }

    static void checkStrings(String[] strings) {
        MyAssert.myassert(strings[0].equals("zero"));
        MyAssert.myassert(strings[1].equals("one"));
        MyAssert.myassert(strings[2].equals("two"));
        MyAssert.myassert(strings[3].equals("three"));
        MyAssert.myassert(strings[4].equals("four"));
    }

    /*
     * Try bad range values, 32 bit get/put.
     */
    static void checkRange32(int[] ints, int[] empty, int negVal1, int negVal2) {
        System.out.println("Array.checkRange32");
        int i = 0;

        MyAssert.myassert(ints.length == 5);

        try {
            i = ints[5];            // exact bound
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            ints[5] = i;            // exact bound
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            i = ints[6];            // one past
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            i = ints[negVal1];      // -1
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            ints[negVal1] = i;      // -1
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            i = ints[negVal2];      // min int
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }


        try {
            i = empty[1];
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
    }

    /*
     * Try bad range values, 64 bit get/put.
     */
    static void checkRange64(long[] longs, int negVal1, int negVal2) {
        System.out.println("Array.checkRange64");
        long l = 0L;

        MyAssert.myassert(longs.length == 5);

        try {
            l = longs[5];            // exact bound
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            longs[5] = l;            // exact bound
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            l = longs[6];            // one past
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            l = longs[negVal1];      // -1
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            longs[negVal1] = l;      // -1
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
        try {
            l = longs[negVal2];      // min int
            MyAssert.myassert(false);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // good
        }
    }

    /*
     * Test negative allocations of object and primitive arrays.
     */
    static void checkNegAlloc(int count) {
        System.out.println("Array.checkNegAlloc");
        String[] strings;
        int[] ints;

        try {
            ints = new int[count];
            MyAssert.myassert(false);
        } catch (NegativeArraySizeException nase) {
            // good
        }

        try {
            strings = new String[count];
            MyAssert.myassert(false);
        } catch (NegativeArraySizeException nase) {
            // good
        }
    }

    public static void run() {
        System.out.println("Array check...");

        byte[] xBytes = new byte[]{0, -1, -2, -3, -4};
        short[] xShorts = new short[]{20, 10, 0, -10, -20};
        char[] xChars = new char[]{40000, 40001, 40002, 40003, 40004};
        int[] xInts = new int[]{70000, 70001, 70002, 70003, 70004};
        boolean[] xBooleans = new boolean[]{true, true, false, true, false};
        float[] xFloats = new float[]{-1.5f, -0.5f, 0.0f, 0.5f, 1.5f};
        long[] xLongs = new long[]{
                0x1122334455667788L, 0x8877665544332211L, 0L, 1L, -1l};
        String[] xStrings = new String[]{
                "zero", "one", "two", "three", "four"};

        int[] xEmpty = new int[0];

        checkBytes(xBytes);
        checkShorts(xShorts);
        checkChars(xChars);
        checkInts(xInts);
        checkBooleans(xBooleans);
        checkFloats(xFloats);
        checkLongs(xLongs);
        checkStrings(xStrings);

        checkRange32(xInts, xEmpty, -1, (int) 0x80000000);
        checkRange64(xLongs, -1, (int) 0x80000000);

        checkNegAlloc(-1);
    }
}
