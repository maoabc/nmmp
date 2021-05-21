package tests.omnibus;// Copyright 2008 The Android Open Source Project


import tests.common.MyAssert;

/**
 * Exercise monitors.
 */
public class Monitor {
    public static int mVal = 0;

    public synchronized void subTest() {
        Object obj = new Object();
        synchronized (obj) {
            mVal++;
            obj = null;     // does NOT cause a failure on exit
            MyAssert.myassert(obj == null);
        }
    }


    public static void run() {
        System.out.println("Monitor.run");

        Object obj = null;

        try {
            synchronized (obj) {
                mVal++;
            }
            MyAssert.myassert(false);
        } catch (NullPointerException npe) {
            /* expected */
        }

        obj = new Object();
        synchronized (obj) {
            mVal++;
        }

        new Monitor().subTest();

        MyAssert.myassert(mVal == 2);
    }

    public native static void runn();
}
