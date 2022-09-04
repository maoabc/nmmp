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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class ZipRepo implements Closeable {
    private final ZipMap zipMap;
    private final FileChannel channel;
    private final Path file;

    public ZipRepo(@Nonnull String filePath) throws IOException {
        this(ZipMap.from(Paths.get(filePath), false, Zip64.Policy.ALLOW));
    }

    public ZipRepo(@Nonnull Path path) throws IOException {
        this(ZipMap.from(path, false, Zip64.Policy.ALLOW));
    }

    public ZipRepo(@Nonnull ZipMap zipMap) throws IOException {
        this.zipMap = zipMap;
        this.channel = FileChannel.open(zipMap.getPath(), StandardOpenOption.READ);
        this.file = zipMap.getPath();
    }

    @Nonnull
    public Map<String, Entry> getEntries() {
        return zipMap.getEntries();
    }

    @Nonnull
    ZipMap getZipMap() {
        return zipMap;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Nonnull
    private Entry getEntry(@Nonnull String entryName) {
        Entry entry = zipMap.getEntries().get(entryName);
        if (entry == null) {
            String msg = String.format("No entry '%s' in file '%s'", entryName, file);
            throw new IllegalArgumentException(msg);
        }
        return entry;
    }

    // Is it the caller's responsibility to close() the returned InputStream.
    @Nonnull
    public InputStream getInputStream(@Nonnull String entryName) throws IOException {
        Entry entry = getEntry(entryName);
        Location payloadLocation = entry.getPayloadLocation();
        InputStream inputStream = new PayloadInputStream(channel, payloadLocation);

        if (!entry.isCompressed()) {
            return inputStream;
        }

        return Compressor.wrapToInflate(inputStream);
    }

    @Nonnull
    public ByteBuffer getContent(@Nonnull String entryName) throws IOException {
        Entry entry = getEntry(entryName);
        Location payloadLocation = entry.getPayloadLocation();
        ByteBuffer payloadByteBuffer = ByteBuffer.allocate(Math.toIntExact(payloadLocation.size()));
        channel.read(payloadByteBuffer, payloadLocation.first);
        payloadByteBuffer.rewind();

        if (entry.isCompressed()) {
            return Compressor.inflate(payloadByteBuffer.array());
        } else {
            return payloadByteBuffer;
        }
    }

    @Nonnull
    public byte[] getComment() {
        return zipMap.getComment();
    }
}
