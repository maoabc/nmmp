/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.zipflinger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

public class Compressor {

    @Nonnull
    public static ByteBuffer deflate(
            @Nonnull byte[] bytes, int offset, int size, int compressionLevel) throws IOException {
        NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream(size);

        Deflater deflater = new Deflater(compressionLevel, true);

        try (DeflaterOutputStream dout = new DeflaterOutputStream(out, deflater)) {
            dout.write(bytes, offset, size);
            dout.flush();
        }

        return out.getByteBuffer();
    }

    @Nonnull
    public static ByteBuffer deflate(@Nonnull byte[] bytes, int compressionLevel)
            throws IOException {
        return deflate(bytes, 0, bytes.length, compressionLevel);
    }

    @Nonnull
    public static ByteBuffer inflate(@Nonnull byte[] bytes) throws IOException {
        NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream(bytes.length);
        Inflater inflater = new Inflater(true);

        try (InflaterOutputStream dout = new InflaterOutputStream(out, inflater)) {
            dout.write(bytes);
            dout.flush();
        }

        return out.getByteBuffer();
    }

    // Exhaust input content into output, inflate / deflate data as needed.
    // Closes both streams once piping is done.
    public static void pipe(
            @Nonnull InputStream in,
            @Nonnull OutputStream out,
            boolean inDeflated,
            int outputCompression)
            throws IOException {

        Inflater inflater = new Inflater(true);
        Deflater deflater = new Deflater(outputCompression, true);
        boolean outDeflated = outputCompression != Deflater.NO_COMPRESSION;

        try (InputStream ins = inDeflated ? new InflaterInputStream(in, inflater) : in;
                OutputStream outs = outDeflated ? new DeflaterOutputStream(out, deflater) : out) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = ins.read(buffer)) != -1) {
                outs.write(buffer, 0, read);
            }
        }
    }

    // Is it the caller's responsibility to close() the returned InputStream.
    @Nonnull
    static InputStream wrapToInflate(@Nonnull InputStream inputStream) {
        Inflater inflater = new Inflater(true);
        return new InflaterInputStream(inputStream, inflater);
    }

    @Nonnull
    static Deflater getDeflater(int compressionLevel) {
        return new Deflater(compressionLevel, true);
    }

    private Compressor() {}
}
