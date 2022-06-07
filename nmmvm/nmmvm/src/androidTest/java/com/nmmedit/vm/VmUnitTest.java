package com.nmmedit.vm;

import android.content.Context;
import android.graphics.Canvas;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(AndroidJUnit4.class)
public class VmUnitTest {

    private String publicSourceDir;
    private byte[] dexbytes;

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.nmmedit.vm.test", appContext.getPackageName());
    }

    @Before
    public void before() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        publicSourceDir = appContext.getPackageCodePath();
        try {
            ZipFile zipFile = new ZipFile(publicSourceDir);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                //需要查找VmTest这个类在debug版本apk哪个dex里,测试版apk在build/outputs/apk/androidTest/debug目录下
                if (zipEntry.getName().equals("classes2.dex")) {
                    InputStream stream = zipFile.getInputStream(zipEntry);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    copyStream(stream, out);
                    dexbytes = out.toByteArray();
                    break;

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        VmTest.loadDex0(dexbytes);
    }


    @Test
    public void testParseDex() throws IOException {
        Vm.parseDex(dexbytes);

    }

    @Test
    public void testIadd() throws IOException {
        int iadd = VmTest.iadd(3, -5);
        assertEquals(iadd, VmTest.iadd0(3, -5));
    }

    @Test
    public void testLadd() throws IOException {
        long ladd = VmTest.ladd(3, -5);
        long l = VmTest.ladd0(3, -5);
        assertEquals(ladd, l);
    }

    @Test
    public void testLoop() throws IOException {
        int sum = VmTest.loop(5);
        int sum2 = VmTest.loop0(5);
        assertEquals(sum, sum2);

    }

    @Test
    public void testGetField() {
        int sf = VmTest.sfieldGet();
        int sf2 = VmTest.sfieldGet0();
        assertEquals(sf, sf2);

    }

    @Test
    public void testSetField() {
        VmTest.setsfield(78);
        int f1 = VmTest.f;
        VmTest.f = 0;
        VmTest.setsfield0(78);
        int f2 = VmTest.f;

        assertEquals(f1, f2);

    }

    @Test
    public void testiFieldGet() {
        VmTest test = new VmTest();
        int i = test.ifieldGet();
        assertEquals(i, 3);
        test.v = 10;
        int get0 = test.ifieldGet0();
        assertEquals(get0, 10);
    }

    @Test
    public void testiFieldPut() {
        VmTest test = new VmTest();
        test.ifieldPut(true);
        assertTrue(test.b);
        test.b = false;

        test.ifieldPut0(true);
        assertTrue(test.b);

    }

    @Test
    public void testSwitch() {
        int s = VmTest.packedSwitch(4);
        int s2 = VmTest.packedSwitch0(4);
        assertEquals(s, s2);
    }

    @Test
    public void testAget() {
        int[] ints = new int[3];
        ints[0] = 5784398;
        ints[1] = 1234;
        ints[2] = 567;

        int aget = VmTest.aget(ints, 2);

        int i = VmTest.aget0(ints, 2);

        assertEquals(aget, i);

    }

    @Test
    public void testaput() {
        String[] strings = new String[4];
        String hello_world = VmTest.aput0(strings, 0, "hello world");
        System.out.println("dalvik " + hello_world + "   " + (hello_world == "hello world"));
    }

    @Test
    public void testFilledNewArray() {
        String s = VmTest.filledNewArray0(2);
        System.out.println("dalvik " + s);
    }

    @Test
    public void testTry() throws IOException {
        VmTest vmTest = new VmTest();
        vmTest.tryCatch0();
    }

    @Test
    public void testCallMethodA() {
        VmTest.callMethodA();
    }

    @Test
    public void testInvokeVirtual() {
        StringBuilder sb = new StringBuilder("test");
        String s = VmTest.invokeVirtual0(sb);
        System.out.println("dalvik " + s);
    }

    @Test
    public void testInvokeSuper() {
        VmTest vmTest = new VmTest();
        boolean hello = vmTest.invokeSuper0("hello");
        System.out.println("dalvik " + hello);
    }

    @Test
    public void testListIter() {
        VmTest.listIter();
        VmTest.getVersion();
//        VmTest.listIter0();
    }

    @Test
    public void testGetDay() {
        VmTest.getDay0();
    }

    @Test
    public void testInitMap() {
        VmTest vmTest = new VmTest();
        Canvas canvas = vmTest.initBitmap0(45, 20, 30);
    }

    @Test
    public void testByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(30);
        VmTest.byteBuffer0(buffer);
    }


    @Test
    public void testFindClass() {
        System.loadLibrary("nmmp");
//        Class<?> version = VmTest.getVersion();
        passClass0(Integer.class);
        try {
            int f = VmTest.f;
        } catch (NoSuchFieldError e) {

        }
    }

    @Test
    public void testInstanceOf() {
        tests.myinstanceof.Main.main(null);
    }

    private static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buff = new byte[4096];
        int rc;
        while ((rc = is.read(buff)) != -1) {
            os.write(buff, 0, rc);
        }
        os.flush();
    }

    public native static void passClass0(Class<?> clazz);


    @Test
    public void testStaticField() {
        System.loadLibrary("nmmp");
        final int i = FieldTest.getInt();
        final int i0 = FieldTest.getInt0();
        assert i0 != i;
        final Object obj0 = FieldTest.getObj0();
        final Object obj = FieldTest.getObj();
        assert obj != obj0;
    }


    @Test
    public void testThrow() throws IOException {
        try {
            VmTest.throwNull();
        } catch (NullPointerException e) {

        }

        VmTest.throwNull0();

    }

    @Test
    public void testConstString() throws IOException {
        final boolean b = VmTest.constString();
        System.out.println(b);
        final boolean b1 = VmTest.constString0();
        System.out.println(b1);
        assert b != b1;
        // 新的const-string实现
        final boolean b2 = VmTest.constString1();
        System.out.println(b2);

        assert b == b2;

    }


    @Test
    public void testPrivateMethod() {
        final TestJniRegisterNatives inflater = new TestJniRegisterNatives();
        final Object realOwner = inflater.getRealOwner();
    }

    @Test
    public void testPassStringJna() {
        VmTest.callJna0();
    }

    @Test
    public void testJniCallJnaPassStr() {
        VmTest.callJnaPassStr();
    }


    @Test
    public void testAgetOutOfBounds() {
        try {
            VmTest.agetOutOfBounds();
        } catch (ArrayIndexOutOfBoundsException e) {
            try {
                VmTest.agetOutOfBounds0();
                throw new RuntimeException("aget error");
            }catch (ArrayIndexOutOfBoundsException e2){
                //正确执行
            }
        }
    }
}

