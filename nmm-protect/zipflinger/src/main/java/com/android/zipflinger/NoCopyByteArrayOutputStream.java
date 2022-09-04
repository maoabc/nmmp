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
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

// A class which contrary to ByteArrayOutputStream allows to peek into the buffer
// without performing a full copy of the content. This stream does not need to be closed.
public class NoCopyByteArrayOutputStream extends ByteArrayOutputStream {
    public NoCopyByteArrayOutputStream(int size) {
        super(size);
    }

    @Nonnull
    public byte[] buf() {
        return buf;
    }

    public int getCount() {
        return count;
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(buf, 0, count);
    }
}
