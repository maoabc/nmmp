package com.nmmedit.apkprotect.util;

public class OsDetector {

    public static boolean isWindows() {
        final String osName = System.getProperty("os.name");
        if (osName == null) {
            return false;
        }
        return osName.toLowerCase().contains("windows");
    }

}
