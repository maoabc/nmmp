/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package apk.arsc;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

/**
 * Represents an XML chunk structure.
 *
 * <p>An XML chunk can contain many nodes as well as a string pool which contains all of the strings
 * referenced by the nodes.
 */
public final class XmlChunk extends ChunkWithChunks {

    private StringPoolChunk stringPoolChunk;

    protected XmlChunk(ByteBuffer buffer, @Nullable Chunk parent) {
        super(buffer, parent);
    }

    @Override
    protected Type getType() {
        return Type.XML;
    }

    /**
     * Returns a string at the provided (0-based) index if the index exists in the string pool.
     */
    public String getString(int index) {
        if (stringPoolChunk == null) {
            for (Chunk chunk : getChunks().values()) {
                if (chunk instanceof StringPoolChunk) {
                    stringPoolChunk = (StringPoolChunk) chunk;
                }
            }
        }
        if (stringPoolChunk != null) {
            return stringPoolChunk.getString(index);
        }
        throw new IllegalStateException("XmlChunk did not contain a string pool.");
    }
}
