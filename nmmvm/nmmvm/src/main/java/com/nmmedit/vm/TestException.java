package com.nmmedit.vm;

import java.io.IOException;

public class TestException extends IOException {
    public TestException(String msg, String msg2, int i, int j, int k) {
        super(msg);
    }
}
