package com.nmmedit.apkprotect.andres;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AxmlEditTest {

    @Test
    public void testRenameApplicationName() throws IOException {
        final InputStream stream = getClass().getResourceAsStream("/binManifest.xml");
        final byte[] bytes = ByteStreams.toByteArray(stream);
        final byte[] newData = AxmlEdit.renameApplicationName(bytes, "com.nmmedit.protect.LoadLibApp");

        final File to = File.createTempFile("aaa", "Manifest.xml");
        Files.write(newData, to);

    }
    @Test
    public void testGetMinSdk() throws IOException {
        final InputStream stream = getClass().getResourceAsStream("/binManifest.xml");
        final byte[] bytes = ByteStreams.toByteArray(stream);
        final int minSdk = AxmlEdit.getMinSdk(bytes);
    }
}