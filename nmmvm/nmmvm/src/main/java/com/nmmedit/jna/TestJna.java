package com.nmmedit.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface TestJna extends Library {
    TestJna INSTANCE = Native.load("test-jna", TestJna.class);

    void pass_str(String msg);
}
