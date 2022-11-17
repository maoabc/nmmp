package com.nmmedit.apkprotect.aar;

import com.nmmedit.apkprotect.ApkFolders;

import java.io.File;

public class AarFolders {
    private final File aar;
    private final File out;
    public final ApkFolders apkFolders;

    public AarFolders(File aar, File out) {
        this.aar = aar;
        this.out = out;
        this.apkFolders = new ApkFolders(getConvertedDexJar(), out);
    }

    public File getAar() {
        return aar;
    }


    public File getConvertedDexJar() {
        return new File(getTempDir(), aar.getName() + "-dex.jar");
    }

    public File getTempDir() {
        final File file = new File(out, ".dx_temp");
        if (!file.exists()) file.mkdirs();
        return file;
    }

    public File getOutputAar() {
        String name = aar.getName();
        final int i = name.lastIndexOf('.');
        if (i != -1) {
            name = name.substring(0, i);
        }
        return new File(out, name + "-protect.aar");
    }

}
