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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ZipReader implements Closeable {
    private final Path file;
    private FileChannel channel;
    private boolean isOpen;

    ZipReader(Path file) {
        this.file = file;
        isOpen = false;
    }

    @Override
    public void close() throws IOException {
        if (!isOpen) {
            return;
        }
        channel.close();
    }

    void read(ByteBuffer byteBuffer, long offset) throws IOException {
        ensureOpen();
        channel.read(byteBuffer, offset);
        byteBuffer.rewind();
    }

    void ensureOpen() throws IOException {
        if (isOpen) {
            return;
        }
        this.channel = FileChannel.open(file, StandardOpenOption.READ);
        if (!channel.isOpen()) {
            throw new IllegalStateException("Unable to open Channel to " + file.toAbsolutePath());
        }
        isOpen = true;
    }

    FileChannel getChannel() throws IOException {
        ensureOpen();
        return channel;
    }
}
