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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;

public class BytesSource extends Source {
    // Bytes to be written in the zip, after the Local File Header.
    private ByteBuffer zipEntryPayload;

    protected BytesSource(String name) {
        super(name);
    }

    /**
     * @param bytes
     * @param name
     * @param compressionLevel One of java.util.zip.Deflater compression level.
     */
    public BytesSource(@Nonnull byte[] bytes, @Nonnull String name, int compressionLevel)
            throws IOException {
        super(name);
        build(bytes, bytes.length, compressionLevel);
    }

    public BytesSource(@Nonnull Path file, @Nonnull String name, int compressionLevel)
            throws IOException {
        super(name);
        byte[] bytes = Files.readAllBytes(file);
        build(bytes, bytes.length, compressionLevel);
    }

    /** @deprecated Use {@link #BytesSource(Path, String, int)} instead. */
    @Deprecated
    public BytesSource(@Nonnull File file, @Nonnull String name, int compressionLevel)
            throws IOException {
        this(file.toPath(), name, compressionLevel);
    }

    /**
     * @param stream BytesSource takes ownership of the InputStream and will close it after draining
     *     it.
     * @param name
     * @param compressionLevel
     * @throws IOException
     */
    public BytesSource(@Nonnull InputStream stream, @Nonnull String name, int compressionLevel)
            throws IOException {
        super(name);
        try (NoCopyByteArrayOutputStream ncbos = new NoCopyByteArrayOutputStream(16000)) {
            byte[] tmpBuffer = new byte[16000];
            int bytesRead;
            while ((bytesRead = stream.read(tmpBuffer)) != -1) {
                ncbos.write(tmpBuffer, 0, bytesRead);
            }
            stream.close();
            build(ncbos.buf(), ncbos.getCount(), compressionLevel);
        }
    }

    protected void build(byte[] bytes, int size, int compressionLevel) throws IOException {
        crc = Crc32.crc32(bytes, 0, size);
        uncompressedSize = size;
        if (compressionLevel == Deflater.NO_COMPRESSION) {
            zipEntryPayload = ByteBuffer.wrap(bytes, 0, size);
            compressedSize = uncompressedSize;
            compressionFlag = LocalFileHeader.COMPRESSION_NONE;
        } else {
            zipEntryPayload = Compressor.deflate(bytes, 0, size, compressionLevel);
            compressedSize = zipEntryPayload.limit();
            compressionFlag = LocalFileHeader.COMPRESSION_DEFLATE;
        }
    }

    @Override
    public void prepare() {}

    @Override
    public long writeTo(@Nonnull ZipWriter writer) throws IOException {
        return writer.write(zipEntryPayload);
    }
}
