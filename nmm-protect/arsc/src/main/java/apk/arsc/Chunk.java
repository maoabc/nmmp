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


import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import apk.base.Preconditions;
import apk.io.LittleEndianDataOutputStream;

/**
 * Represents a generic chunk.
 */
public abstract class Chunk implements SerializableResource {

    /**
     * Types of chunks that can exist.
     */
    // See
    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/libs/androidfw/include/androidfw/ResourceTypes.h
    public enum Type {
        NULL(0x0000),
        STRING_POOL(0x0001),
        TABLE(0x0002),
        XML(0x0003),
        XML_START_NAMESPACE(0x0100),
        XML_END_NAMESPACE(0x0101),
        XML_START_ELEMENT(0x0102),
        XML_END_ELEMENT(0x0103),
        XML_CDATA(0x0104),
        XML_RESOURCE_MAP(0x0180),
        TABLE_PACKAGE(0x0200),
        TABLE_TYPE(0x0201),
        TABLE_TYPE_SPEC(0x0202),
        TABLE_LIBRARY(0x0203),
        TABLE_OVERLAYABLE(0x204),
        TABLE_OVERLAYABLE_POLICY(0x205);

        private final short code;

        private static final Map<Short, Type> FROM_SHORT;

        static {
            Map<Short, Type> map = new HashMap<>();
            for (Type type : values()) {
                map.put(type.code(), type);
            }
            FROM_SHORT = Collections.unmodifiableMap(map);
        }

        Type(int code) {
            this.code = (short) code;
        }

        public short code() {
            return code;
        }

        public static Type fromCode(short code) {
            Type type = FROM_SHORT.get(code);
            if (type == null) {
                throw new NullPointerException("Unknown chunk type: " + code);
            }
            return type;
        }
    }

    /**
     * The byte boundary to pad chunks on.
     */
    public static final int PAD_BOUNDARY = 4;

    /**
     * The number of bytes in every chunk that describes chunk type, header size, and chunk size.
     */
    public static final int METADATA_SIZE = 8;
    /**
     * The number of bytes in every chunk that describes header size and chunk size, but not the type.
     */
    public static final int METADATA_SIZE_NO_TYPE = METADATA_SIZE - 2;

    /**
     * The offset in bytes, from the start of the chunk, where the chunk size can be found.
     */
    private static final int CHUNK_SIZE_OFFSET = 4;

    /**
     * The parent to this chunk, if any.
     */
    @Nullable
    private final Chunk parent;

    /**
     * Size of the chunk header in bytes.
     */
    protected final int headerSize;

    /**
     * headerSize + dataSize. The total size of this chunk.
     */
    protected final int chunkSize;

    /**
     * Offset of this chunk from the start of the file.
     */
    protected final int offset;

    protected Chunk(ByteBuffer buffer, @Nullable Chunk parent) {
        this.parent = parent;
        offset = buffer.position() - 2;
        headerSize = (buffer.getShort() & 0xFFFF);
        chunkSize = buffer.getInt();
    }

    /**
     * Finishes initialization of a chunk. This should be called immediately after the constructor.
     * This is separate from the constructor so that the header of a chunk can be fully initialized
     * before the payload of that chunk is initialized for chunks that require such behavior.
     *
     * @param buffer The buffer that the payload will be initialized from.
     */
    protected void init(ByteBuffer buffer) {
    }

    /**
     * Returns the parent to this chunk, if any. A parent is a chunk whose payload contains this
     * chunk. If there's no parent, null is returned.
     */
    @Nullable
    public Chunk getParent() {
        return parent;
    }

    protected abstract Type getType();

    /**
     * Returns the size of this chunk's header.
     */
    public final int getHeaderSize() {
        return headerSize;
    }

    /**
     * Returns the size of this chunk when it was first read from a buffer. A chunk's size can deviate
     * from this value when its data is modified (e.g. adding an entry, changing a string).
     *
     * <p>A chunk's current size can be determined from the length of the byte array returned from
     * {@link #toByteArray}.
     */
    public final int getOriginalChunkSize() {
        return chunkSize;
    }

    /**
     * Reposition the buffer after this chunk. Use this at the end of a Chunk constructor.
     *
     * @param buffer The buffer to be repositioned.
     */
    protected final void seekToEndOfChunk(ByteBuffer buffer) {
        buffer.position(offset + chunkSize);
    }

    /**
     * Writes the type and header size. We don't know how big this chunk will be (it could be
     * different since the last time we checked), so this needs to be passed in.
     *
     * @param output    The buffer that will be written to.
     * @param chunkSize The total size of this chunk in bytes, including the header.
     */
    protected final void writeHeader(ByteBuffer output, int chunkSize) {
        int start = output.position();
        output.putShort(getTypeValue());
        output.putShort((short) headerSize);
        output.putInt(chunkSize);
        writeHeader(output);
        int headerBytes = output.position() - start;
        Preconditions.checkState(headerBytes == getHeaderSize(),
                "Written header is wrong size. Got %s, want %s", headerBytes, getHeaderSize());
    }

    /**
     * Allows overwriting what value gets written as the type of a chunk. Subclasses may use a
     * specialized type value schema.
     *
     * @return The type value for this chunk.
     */
    protected short getTypeValue() {
        return getType().code();
    }

    /**
     * Writes the remaining header (after the type, {@code headerSize}, and {@code chunkSize}).
     *
     * @param output The buffer that the header will be written to.
     */
    protected void writeHeader(ByteBuffer output) {
    }

    /**
     * Writes the chunk payload. The payload is data in a chunk which is not in the first {@code
     * headerSize} bytes of the chunk.
     *
     * @param output  The stream that the payload will be written to.
     * @param header  The already-written header. This can be modified to fix payload offsets.
     * @param options The serialization options to be applied to the result.
     * @throws IOException Thrown if {@code output} could not be written to (out of memory).
     */
    protected void writePayload(DataOutput output, ByteBuffer header, int options)
            throws IOException {
    }

    /**
     * Pads {@code output} until {@code currentLength} is on a 4-byte boundary.
     *
     * @param output        The {@link DataOutput} that will be padded.
     * @param currentLength The current length, in bytes, of {@code output}
     * @return The new length of {@code output}
     * @throws IOException Thrown if {@code output} could not be written to.
     */
    protected static int writePad(DataOutput output, int currentLength) throws IOException {
        while (currentLength % PAD_BOUNDARY != 0) {
            output.write(0);
            ++currentLength;
        }
        return currentLength;
    }

    @Override
    public final byte[] toByteArray() throws IOException {
        return toByteArray(SerializableResource.NONE);
    }

    /**
     * Converts this chunk into an array of bytes representation. Normally you will not need to
     * override this method unless your header changes based on the contents / size of the payload.
     */
    @Override
    public final byte[] toByteArray(int options) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(getHeaderSize()).order(ByteOrder.LITTLE_ENDIAN);
        writeHeader(header, 0);  // The chunk size isn't known yet. This will be filled in later.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LittleEndianDataOutputStream payload = new LittleEndianDataOutputStream(baos)) {
            writePayload(payload, header, options);
        }

        byte[] payloadBytes = baos.toByteArray();
        int chunkSize = getHeaderSize() + payloadBytes.length;
        header.putInt(CHUNK_SIZE_OFFSET, chunkSize);

        // Combine results
        ByteBuffer result = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN);
        result.put(header.array());
        result.put(payloadBytes);
        return result.array();
    }

    /**
     * Creates a new chunk whose contents start at {@code buffer}'s current position.
     *
     * @param buffer A buffer positioned at the start of a chunk.
     * @return new chunk
     */
    public static Chunk newInstance(ByteBuffer buffer) {
        return newInstance(buffer, null);
    }

    /**
     * Creates a new chunk whose contents start at {@code buffer}'s current position.
     *
     * @param buffer A buffer positioned at the start of a chunk.
     * @param parent The parent to this chunk (or null if there's no parent).
     * @return new chunk
     */
    public static Chunk newInstance(ByteBuffer buffer, @Nullable Chunk parent) {
        Chunk result;
        Type type = Type.fromCode(buffer.getShort());
        switch (type) {
            case STRING_POOL:
                result = new StringPoolChunk(buffer, parent);
                break;
            case TABLE:
                result = new ResourceTableChunk(buffer, parent);
                break;
            case XML:
                result = new XmlChunk(buffer, parent);
                break;
            case XML_START_NAMESPACE:
                result = new XmlNamespaceStartChunk(buffer, parent);
                break;
            case XML_END_NAMESPACE:
                result = new XmlNamespaceEndChunk(buffer, parent);
                break;
            case XML_START_ELEMENT:
                result = new XmlStartElementChunk(buffer, parent);
                break;
            case XML_END_ELEMENT:
                result = new XmlEndElementChunk(buffer, parent);
                break;
            case XML_CDATA:
                result = new XmlCdataChunk(buffer, parent);
                break;
            case XML_RESOURCE_MAP:
                result = new XmlResourceMapChunk(buffer, parent);
                break;
            case TABLE_PACKAGE:
                result = new PackageChunk(buffer, parent);
                break;
            case TABLE_TYPE:
                result = new TypeChunk(buffer, parent);
                break;
            case TABLE_TYPE_SPEC:
                result = new TypeSpecChunk(buffer, parent);
                break;
            case TABLE_LIBRARY:
                result = new LibraryChunk(buffer, parent);
                break;
            default:
                result = new UnknownChunk(buffer, parent);
        }
        result.init(buffer);
        result.seekToEndOfChunk(buffer);
        return result;
    }

}
