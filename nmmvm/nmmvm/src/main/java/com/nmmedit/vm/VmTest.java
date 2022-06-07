package com.nmmedit.vm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.LongSparseArray;

import androidx.annotation.Keep;

import com.nmmedit.jna.TestJna;

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

    //整数运算
    public static int iadd(int a, int b) {
        return a + b;
    }

    public static native int iadd0(int a, int b);


    //长整数及方法调用
    public static long ladd(long a, long b) {
        long max = Math.max(a, b);
        return a + b - 6;
    }

    public static native long ladd0(long a, long b);


    //跳转指令,判断指令是否正常
    public static int loop(int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += i;
        }
        return sum;
    }

    public static native int loop0(int count);

    public static int f = 3;

    //静态域测试
    public static int sfieldGet() {
        return f;
    }

    public static native int sfieldGet0();

    public static void setsfield(int v) {
        f = v;
    }

    public static native void setsfield0(int v);

    public int v = 3;

    //实例域相关测试
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


    //两条switch指令,只测试一条,另外的没测试,实际也是正确的

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

    //测试取得数组元素
    public static int aget(int[] arr, int idx) {
        return arr[idx];
    }

    public static native int aget0(int[] arr, int idx);

    //测试设置数组元素
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

    //测试异常处理是否正常
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

    //测试if-eq,if-ne指令,比较对象
    public static Day getDay() {
        return Day.One;
    }

    public static native void getDay0();

    private LongSparseArray<Bitmap> bitmapSurface = new LongSparseArray<>(5);

    //测试参数传递问题,有long/double时是否能正确传递
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


    //java执行会抛空指针异常
    public static void throwNull() throws IOException {
        IOException e = null;
        throw e;
    }

    //测试当异常为null时throw指令是否正常抛异常
    public native static void throwNull0() throws IOException;

    //todo const-string指令相关问题,

    public static String S = "string";

    //这个方法被native化,通过vm执行
    public static boolean constString() {
        // java执行的话为true,但是通过vm执行这个只能是false
        // 如果要和java保持一致的话,比如可以在处理dex生成一个Constants的类包含String constString(int idx)方法,
        // 里面一个字符串数组,包含所有const-string*需要的字符串,jni不再通过NewStringUTF创建字符串,
        // 而是通过调用constString得到字符串,这样可以保证S和"string"是同一对象
        return S == "string";
    }

    public native static boolean constString0();


    //验证jni从java层加载字符串常量可以保证字符串对象一致

    public static String myConst(int idx) {
        return "string";
    }

    public native static boolean constString1();

    //测试native化后,在安卓6下传递对象到jna方法错误问题
    public static void callJna() {
        String s = "hello world";
        TestJna.INSTANCE.pass_str(s);
    }

    //callJna native化后
    public native static void callJna0();

    // 手写jni代码去调用pass_str,
    // 经过测试在android6上面依然还会崩溃, 所以vm部分应该没问题, 问题出在
    // android6的art或者jna
    public native static void callJnaPassStr();


    //java运行抛数组越界异常
    public static void agetOutOfBounds() {
        final int[] ints = new int[2];

        final int i = ints[3];
    }

    public native static void agetOutOfBounds0();
}


