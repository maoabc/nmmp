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
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Deflater;

/*
 *  A source acting as a pipe from an Entry in a zip archive into a
 *  local ByteBuffer.
 *
 *  Able to inflate the input stream if needed. Able to deflate the
 *  output stream if needed.
 */
class ZipSourceEntryPipe extends Source {

    private final ZipSource zipSource;
    private final int compressionLevel;
    private Entry entry;
    private ByteBuffer byteBuffer;

    ZipSourceEntryPipe(String newName, Entry entry, ZipSource zipSource, int compressionLevel) {
        super(newName);
        this.entry = entry;
        this.zipSource = zipSource;
        this.compressionLevel = compressionLevel;
    }

    @Override
    public void prepare() throws IOException {
        crc = entry.getCrc();
        if (compressionLevel != Deflater.NO_COMPRESSION) {
            compressionFlag = LocalFileHeader.COMPRESSION_DEFLATE;
        } else {
            compressionFlag = LocalFileHeader.COMPRESSION_NONE;
        }
        uncompressedSize = entry.getUncompressedSize();

        FileChannel channel = zipSource.getChannel();
        Location loc = entry.getPayloadLocation();
        try (NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream(8192);
                InputStream in = new PayloadInputStream(channel, loc)) {

            Compressor.pipe(in, out, entry.isCompressed(), compressionLevel);
            byteBuffer = out.getByteBuffer();
            compressedSize = byteBuffer.limit();
        }
    }

    @Override
    public long writeTo(@Nonnull ZipWriter writer) throws IOException {
        return writer.write(byteBuffer);
    }
}
