package com.nmmedit.apkprotect.util;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ModifiedUtf8Test extends TestCase {
    private static final String ZZ_ACTION_PACKED_0 =
            "\10\0\1\1\1\2\2\3\1\2\4\3\1\4\2\1";

    @Test
    public void testEncode() throws UTFDataFormatException {
        final byte[] mUtf8 = ModifiedUtf8.encode(ZZ_ACTION_PACKED_0);
        final byte[] utf8 = ZZ_ACTION_PACKED_0.getBytes(StandardCharsets.UTF_8);
        assert !Arrays.equals(mUtf8,utf8);
    }
}