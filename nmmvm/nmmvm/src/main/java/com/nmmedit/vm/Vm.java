package com.nmmedit.vm;

public class Vm {

    static {
        System.loadLibrary("nmmp");
    }

    public static native void parseDex(byte[] dex);


    public static native int callIntMethod0(byte[] dex, String className, String methodName, Object[] args);


}
