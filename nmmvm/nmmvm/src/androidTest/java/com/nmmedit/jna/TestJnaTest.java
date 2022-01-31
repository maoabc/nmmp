package com.nmmedit.jna;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestJnaTest {

    @Test
    public void pass_str() {
        TestJna.INSTANCE.pass_str("pass str");
    }
}