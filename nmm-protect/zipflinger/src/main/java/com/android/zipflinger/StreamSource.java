/*
 * Copyright (C) 2020 The Android Open Source Project
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
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class StreamSource extends Source {

    private static final int TMP_BUFFER_SIZE = 4096;

    @Nonnull
    private final NoCopyByteArrayOutputStream buffer;

    @Nullable
    private Path tmpStore = null;
    private long tmpStoreSize = 0;

    // Drain the InputStream until it is empty. Keep up to largeLimit bytes
    // in memory and use a backing storage if the InputStream is bigger.
    public StreamSource(@Nonnull InputStream src, @Nonnull String name, int compressionLevel, int largeLimit)
            throws IOException {
        super(name);
        buffer = new NoCopyByteArrayOutputStream(TMP_BUFFER_SIZE);


        long bytesRead = 0;
        try (CheckedInputStream in = new CheckedInputStream(src, new CRC32());
                OutputStream out = getOutput(compressionLevel)) {
            int read;
            byte[] bytes = new byte[TMP_BUFFER_SIZE];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
                bytesRead += read;

                if (buffer.getCount() > largeLimit) {
                    flushBuffer();
                }
            }
            crc = Ints.longToUint(in.getChecksum().getValue());
        } finally {
            src.close();
        }

        compressedSize = buffer.getCount() + tmpStoreSize;
        uncompressedSize = bytesRead;
        if (compressionLevel == Deflater.NO_COMPRESSION) {
            compressionFlag = LocalFileHeader.COMPRESSION_NONE;
        } else {
            compressionFlag = LocalFileHeader.COMPRESSION_DEFLATE;
        }
    }

    private void flushBuffer() throws IOException {
        FileChannel fc;
        if (tmpStore == null) {
            tmpStore = LargeFileSource.getTmpStoragePath(getName());
            fc = FileChannel.open(tmpStore, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);

            // Just in case we crash before writeTo is called, attempt to clean up on VM exit.
            tmpStore.toFile().deleteOnExit();
        } else {
            fc = FileChannel.open(tmpStore, StandardOpenOption.APPEND);
        }

        try(FileChannel channel = fc) {
            ByteBuffer b = buffer.getByteBuffer();
            tmpStoreSize += b.remaining();
            channel.write(b);
            buffer.reset();
        }
    }

    private OutputStream getOutput(int compressionLevel) {
        if (compressionLevel == Deflater.NO_COMPRESSION) {
            return buffer;
        } else {
            Deflater deflater = Compressor.getDeflater(compressionLevel);
            return new DeflaterOutputStream(buffer, deflater);
        }
    }

    @Override
    public void prepare() throws IOException {}

    @Override
    public long writeTo(@Nonnull ZipWriter writer) throws IOException {
        if (tmpStore != null) {
            try (FileChannel src = FileChannel.open(tmpStore, StandardOpenOption.READ)) {
                writer.transferFrom(src, 0, tmpStoreSize);
            } finally {
                Files.delete(tmpStore);
            }
        }

        writer.write(buffer.getByteBuffer());
        return buffer.getCount() + tmpStoreSize;
    }
}
