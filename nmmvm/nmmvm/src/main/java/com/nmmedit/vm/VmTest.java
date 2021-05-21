package com.nmmedit.vm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.LongSparseArray;

import androidx.annotation.Keep;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class VmTest extends ArrayList {
    static {
        System.loadLibrary("nmmp");
    }


    public static native void loadDex0(byte[] dex);

    public static int iadd(int a, int b) {
        return a + b;
    }

    public static native int iadd0(int a, int b);


    public static long ladd(long a, long b) {
        long max = Math.max(a, b);
        return a + b - 6;
    }

    public static native long ladd0(long a, long b);


    public static int loop(int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += i;
        }
        return sum;
    }

    public static native int loop0(int count);

    public static int f = 3;

    public static int sfieldGet() {
        return f;
    }

    public static native int sfieldGet0();

    public static void setsfield(int v) {
        f = v;
    }

    public static native void setsfield0(int v);

    public int v = 3;

    public int ifieldGet() {
        return v;
    }

    public native int ifieldGet0();

    public boolean b;

    public void ifieldPut(boolean b) {
        this.b = b;
    }

    public native void ifieldPut0(boolean b);

    private static volatile double id;

    public static double sfieldGetDouble() {
        return id;
    }


    public static int packedSwitch(int key) {
        switch (key) {
            case 1:
                return 100;
            case 2:
                return 200;
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return 30000;
            case 9:
                return 40000;
            default:
                return 0;
        }
    }

    public static native int packedSwitch0(int key);

    public static int aget(int[] arr, int idx) {
        return arr[idx];
    }

    public static native int aget0(int[] arr, int idx);

    public static String aput(String[] arr, int idx, String val) {
        arr[idx] = val;
        return val;
    }

    public static native String aput0(String[] arr, int idx, String val);


    public static int filledArray(int idx) {
        return new int[]{1, 2, 3, 4, 5, 6}[idx];
    }

    public static String filledNewArray(int idx) {
        return new String[]{"1", "3", "5", "6", "7", "9"}[idx];
    }

    public static native String filledNewArray0(int idx);

    public void tryCatch() throws IOException {
        synchronized (this) {
            try {
                List<Object> list = new ArrayList<>();
                list.add("hello");
                list.add("world");
                StringBuilder ggg = new StringBuilder("ggg");
                Object o = list.get(1);
                for (int i = 0; i < 6000; i++) {
                    list.add(i);
                    ggg.toString();
                }
                long l = System.currentTimeMillis();
                System.out.println("dalvik hello world" + o + "   " + l);
                newExcept("myExcepcetion ");
            } catch (IOException e) {
                throw e;
            }
        }

    }

    private static void newExcept(String str) throws TestException {
        throw new TestException(str, "eee", 1, 2, 3);
    }

    public native void tryCatch0() throws IOException;

    //测试jni Call*MehtodA这类方法参数传递
    public static native void callMethodA();

    public static String invokeVirtual(StringBuilder sb) {
        return sb.toString();
    }

    public static native String invokeVirtual0(StringBuilder sb);


    @Override
    public boolean add(Object o) {
        return super.add(o);
    }

    //实际通过vm执行add(Object o),用来测试invokeSuper是否正常
    public native boolean invokeSuper0(String txt);


    public static void listIter() {
        List<String> list = new ArrayList<>();
        list.add("1");

        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            System.nanoTime();
        }

    }

    public native static void listIter0();


    public enum Day {
        One,
        Two,
        Three
    }

    public static Day getDay() {
        return Day.One;
    }

    public static native void getDay0();

    private LongSparseArray<Bitmap> bitmapSurface = new LongSparseArray<>(5);

    public Canvas initBitmap(long nativeSurface, int width, int height) {
//        Log.d(TAG, "initBitmap: " + nativeSurface);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmapSurface.put(nativeSurface, bitmap);
        return new Canvas(bitmap);
    }

    public native Canvas initBitmap0(long nativeSurface, int width, int height);


    public static Class<?> getVersion() {
//        Class<NoClassDef> noClassDefClass = NoClassDef.class;
//        System.out.println(noClassDefClass);
        return VmTest.class;
    }

    //测试Buffer.clear()及ByteBuffer.clear()这类方法兼容问题
    public static native void byteBuffer0(ByteBuffer buffer);


}
