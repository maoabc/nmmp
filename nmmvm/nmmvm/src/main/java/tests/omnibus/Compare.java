package tests.omnibus;// Copyright 2008 The Android Open Source Project


import tests.common.MyAssert;

/**
 * Test comparison operators.
 */
public class Compare {

    /*
     * Test the integer comparisons in various ways.
     */
    static void testIntCompare(int minus, int plus, int plus2, int zero) {
        System.out.println("IntMath.testIntCompare");

        if (minus > plus)
            MyAssert.myassert(false);
        if (minus >= plus)
            MyAssert.myassert(false);
        if (plus < minus)
            MyAssert.myassert(false);
        if (plus <= minus)
            MyAssert.myassert(false);
        if (plus == minus)
            MyAssert.myassert(false);
        if (plus != plus2)
            MyAssert.myassert(false);

        /* try a branch-taken */
        if (plus != minus) {
            MyAssert.myassert(true);
        } else {
            MyAssert.myassert(false);
        }

        if (minus > 0)
            MyAssert.myassert(false);
        if (minus >= 0)
            MyAssert.myassert(false);
        if (plus < 0)
            MyAssert.myassert(false);
        if (plus <= 0)
            MyAssert.myassert(false);
        if (plus == 0)
            MyAssert.myassert(false);
        if (zero != 0)
            MyAssert.myassert(false);

        if (zero == 0) {
            MyAssert.myassert(true);
        } else {
            MyAssert.myassert(false);
        }
    }

    /*
     * Test cmp-long.
     *
     * minus=-5, alsoMinus=0xFFFFFFFF00000009, plus=4, alsoPlus=8
     */
    static void testLongCompare(long minus, long alsoMinus, long plus,
        long alsoPlus) {

        System.out.println("IntMath.testLongCompare");
        if (minus > plus)
            MyAssert.myassert(false);
        if (plus < minus)
            MyAssert.myassert(false);
        if (plus == minus)
            MyAssert.myassert(false);

        if (plus >= plus+1)
            MyAssert.myassert(false);
        if (minus >= minus+1)
            MyAssert.myassert(false);

        /* try a branch-taken */
        if (plus != minus) {
            MyAssert.myassert(true);
        } else {
            MyAssert.myassert(false);
        }

        /* compare when high words are equal but low words differ */
        if (plus > alsoPlus)
            MyAssert.myassert(false);
        if (alsoPlus < plus)
            MyAssert.myassert(false);
        if (alsoPlus == plus)
            MyAssert.myassert(false);

        /* high words are equal, low words have apparently different signs */
        if (minus < alsoMinus)      // bug!
            MyAssert.myassert(false);
        if (alsoMinus > minus)
            MyAssert.myassert(false);
        if (alsoMinus == minus)
            MyAssert.myassert(false);
    }

    /*
     * Test cmpl-float and cmpg-float.
     */
    static void testFloatCompare(float minus, float plus, float plus2,
        float nan) {

        System.out.println("IntMath.testFloatCompare");
        if (minus > plus)
            MyAssert.myassert(false);
        if (plus < minus)
            MyAssert.myassert(false);
        if (plus == minus)
            MyAssert.myassert(false);
        if (plus != plus2)
            MyAssert.myassert(false);

        if (plus <= nan)
            MyAssert.myassert(false);
        if (plus >= nan)
            MyAssert.myassert(false);
        if (minus <= nan)
            MyAssert.myassert(false);
        if (minus >= nan)
            MyAssert.myassert(false);
        if (nan >= plus)
            MyAssert.myassert(false);
        if (nan <= plus)
            MyAssert.myassert(false);

        if (nan == nan)
            MyAssert.myassert(false);
    }

    static void testDoubleCompare(double minus, double plus, double plus2,
        double nan) {

        System.out.println("IntMath.testDoubleCompare");
        if (minus > plus)
            MyAssert.myassert(false);
        if (plus < minus)
            MyAssert.myassert(false);
        if (plus == minus)
            MyAssert.myassert(false);
        if (plus != plus2)
            MyAssert.myassert(false);

        if (plus <= nan)
            MyAssert.myassert(false);
        if (plus >= nan)
            MyAssert.myassert(false);
        if (minus <= nan)
            MyAssert.myassert(false);
        if (minus >= nan)
            MyAssert.myassert(false);
        if (nan >= plus)
            MyAssert.myassert(false);
        if (nan <= plus)
            MyAssert.myassert(false);

        if (nan == nan)
            MyAssert.myassert(false);
    }

    public static void run() {
        testIntCompare(-5, 4, 4, 0);
        testLongCompare(-5L, -4294967287L, 4L, 8L);

        testFloatCompare(-5.0f, 4.0f, 4.0f, (1.0f/0.0f) / (1.0f/0.0f));
        testDoubleCompare(-5.0, 4.0, 4.0, (1.0/0.0) / (1.0/0.0));
    }
}
