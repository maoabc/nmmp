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

/*
 * A wrapper around a FileChannel providing a bounded view of
 * it according to the provided Location.
 *
 * Does not need to be closed. And does not close the wrapped
 * channel either.
 */
public class PayloadInputStream extends InputStream {

    private FileChannel channel;
    private Location boundaries;
    private long position;

    public PayloadInputStream(@Nonnull FileChannel channel, @Nonnull Location location)
            throws IOException {
        this.channel = channel;
        this.boundaries = location;
        this.position = location.first;

        if (location.first < 0 || location.last >= channel.size()) {
            throw new IllegalStateException("Location not within channel boundaries");
        }
    }

    @Override
    public int read() throws IOException {
        if (position > boundaries.last) {
            return -1;
        }
        byte[] buffer = new byte[1];
        read(buffer);
        // Convert from [-128, 127] to [0-255] according to InputStream requirements.
        return buffer[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position > boundaries.last) {
            return -1;
        }

        long available = boundaries.last - position + 1;
        available = Math.min(available, Integer.MAX_VALUE);

        int toRead = Math.min(Math.toIntExact(available), len);
        ByteBuffer buffer = ByteBuffer.wrap(b, off, toRead);
        int read = channel.read(buffer, position);
        position += read;
        return read;
    }
}
