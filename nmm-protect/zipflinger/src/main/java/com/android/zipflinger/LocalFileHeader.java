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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class LocalFileHeader {
    private static final int SIGNATURE = 0x04034b50;

    public static final int LOCAL_FILE_HEADER_SIZE = 30;

    // Minimum number of bytes needed to create a virtual zip entry (an entry not present in
    // the Central Directory with name length = 0 and an extra field containing padding data).
    public static final long VIRTUAL_HEADER_SIZE = LOCAL_FILE_HEADER_SIZE;

    public static final short COMPRESSION_NONE = 0;
    public static final short COMPRESSION_DEFLATE = 8;

    static final long VIRTUAL_ENTRY_MAX_SIZE = LOCAL_FILE_HEADER_SIZE + Ints.USHRT_MAX;
    static final long OFFSET_TO_NAME = 26;

    // Zip64 extra payload must only include uncompressed size and compressed size. It differs
    // from the Central Directory Record which also features an uint64_t offset to the LFH.
    private static final int ZIP64_PAYLOAD_SIZE = Long.BYTES * 2;
    private static final int ZIP64_EXTRA_SIZE = Short.BYTES * 2 + ZIP64_PAYLOAD_SIZE;

    private final byte[] nameBytes;
    private final short compressionFlag;
    private final int crc;
    private final long compressedSize;
    private final long uncompressedSize;
    private final boolean isZip64;
    private int padding;

    LocalFileHeader(Source source) {
        this.nameBytes = source.getNameBytes();
        this.compressionFlag = source.getCompressionFlag();
        this.crc = source.getCrc();
        this.compressedSize = source.getCompressedSize();
        this.uncompressedSize = source.getUncompressedSize();
        this.isZip64 = compressedSize > Zip64.LONG_MAGIC || uncompressedSize > Zip64.LONG_MAGIC;
        this.padding = 0;
    }

    public static void fillVirtualEntry(@Nonnull ByteBuffer virtualEntry) {
        int sizeToFill = virtualEntry.capacity();
        if (sizeToFill < VIRTUAL_HEADER_SIZE) {
            String message = String.format("Not enough space for virtual entry (%d)", sizeToFill);
            throw new IllegalStateException(message);
        }
        virtualEntry.order(ByteOrder.LITTLE_ENDIAN);
        virtualEntry.putInt(SIGNATURE);
        virtualEntry.putShort((short) 0); // Version needed
        virtualEntry.putShort((short) 0); // general purpose flag
        virtualEntry.putShort(COMPRESSION_NONE);
        virtualEntry.putShort(CentralDirectoryRecord.DEFAULT_TIME);
        virtualEntry.putShort(CentralDirectoryRecord.DEFAULT_DATE);
        virtualEntry.putInt(0); // CRC-32
        virtualEntry.putInt(0); // compressed size
        virtualEntry.putInt(0); // uncompressed size
        virtualEntry.putShort((short) 0); // file name length
        // -2 for the extra length ushort we have to write
        virtualEntry.putShort(Ints.intToUshort(virtualEntry.remaining() - 2)); // extra length
        virtualEntry.rewind();
    }

    public void setPadding(int padding) {
        if (padding > Ints.USHRT_MAX) {
            String err = String.format("Padding cannot be more than %s bytes", Ints.USHRT_MAX);
            throw new IllegalStateException(err);
        }
        this.padding = padding;
    }

    public void write(@Nonnull ZipWriter writer) throws IOException {
        ByteBuffer extraField = buildExtraField();
        int bytesNeeded = LOCAL_FILE_HEADER_SIZE + nameBytes.length + extraField.capacity();

        short versionNeeded = isZip64 ? Zip64.VERSION_NEEDED : 0;
        int size = isZip64 ? Zip64.INT_MAGIC : Ints.longToUint(uncompressedSize);
        int csize = isZip64 ? Zip64.INT_MAGIC : Ints.longToUint(compressedSize);

        ByteBuffer buffer = ByteBuffer.allocate(bytesNeeded).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(SIGNATURE);
        buffer.putShort(versionNeeded);

        buffer.putShort((short) 0); // general purpose flag
        buffer.putShort(compressionFlag);
        buffer.putShort(CentralDirectoryRecord.DEFAULT_TIME);
        buffer.putShort(CentralDirectoryRecord.DEFAULT_DATE);
        buffer.putInt(crc);
        buffer.putInt(csize); // compressed size
        buffer.putInt(size); // size
        buffer.putShort(Ints.intToUshort(nameBytes.length));
        buffer.putShort(Ints.intToUshort(extraField.capacity())); // Extra size
        buffer.put(nameBytes);
        buffer.put(extraField);

        buffer.rewind();
        writer.write(buffer);
    }

    public long getSize() {
        long extraSize = isZip64 ? ZIP64_EXTRA_SIZE : 0;
        return LOCAL_FILE_HEADER_SIZE + nameBytes.length + extraSize;
    }

    @Nonnull
    private ByteBuffer buildExtraField() {
        if (!isZip64) {
            return ByteBuffer.allocate(padding);
        }

        ByteBuffer zip64extra = ByteBuffer.allocate(ZIP64_EXTRA_SIZE + padding);
        zip64extra.order(ByteOrder.LITTLE_ENDIAN);
        zip64extra.putShort(Zip64.EXTRA_ID);
        zip64extra.putShort(Ints.intToUshort(ZIP64_PAYLOAD_SIZE));
        zip64extra.putLong(uncompressedSize);
        zip64extra.putLong(compressedSize);
        zip64extra.rewind();
        return zip64extra;
    }
}
