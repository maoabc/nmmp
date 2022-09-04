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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class CentralDirectoryRecord {

    public static final int SIGNATURE = 0x02014b50;
    public static final int SIZE = 46;
    public static final int DATA_DESCRIPTOR_FLAG = 0x0008;
    public static final int DATA_DESCRIPTOR_SIGNATURE = 0x08074b50;

    // JDK 9 consider time&data field with value 0 as invalid. Use 1 instead.
    // These are in MS-DOS 16-bit format. For actual specs, see:
    // https://msdn.microsoft.com/en-us/library/windows/desktop/ms724247(v=vs.85).aspx
    public static final short DEFAULT_TIME = 1 | 1 << 5 | 1 << 11;
    public static final short DEFAULT_DATE = 1 | 1 << 5 | 1 << 9;

    // Zip64 extra format:
    // uint16_t id (0x0001)
    // uint16_t size payload (0x18)
    // Payload:
    //    - uint64_t Uncompressed size.
    //    - uint64_t Compressed size.
    //    - uint64_t offset to LFH in archive.
    private static final int ZIP64_PAYLOAD_SIZE = Long.BYTES * 3;
    private static final int ZIP64_EXTRA_SIZE = Short.BYTES * 2 + ZIP64_PAYLOAD_SIZE;

    private final byte[] nameBytes;
    private final int crc;
    private final long compressedSize;
    private final long uncompressedSize;
    // Location of the Local file header to end of payload in file space.
    private final Location location;
    private final short compressionFlag;
    private final short versionMadeBy;
    private final int externalAttribute;
    private final Location payloadLocation;
    private final boolean isZip64;

    CentralDirectoryRecord(@Nonnull Source source,
            Location location,
            Location payloadLocation) {
        this.nameBytes = source.getNameBytes();
        this.crc = source.getCrc();
        this.compressedSize = source.getCompressedSize();
        this.uncompressedSize = source.getUncompressedSize();
        this.location = location;
        this.compressionFlag = source.getCompressionFlag();
        this.payloadLocation = payloadLocation;
        this.isZip64 =
                compressedSize > Zip64.LONG_MAGIC
                        || uncompressedSize > Zip64.LONG_MAGIC
                        || location.first > Zip64.LONG_MAGIC;
        this.versionMadeBy = source.getVersionMadeBy();
        this.externalAttribute = source.getExternalAttributes();
    }

    void write(@Nonnull ByteBuffer buf) {
        short versionNeeded = isZip64 ? Zip64.VERSION_NEEDED : 0;
        int size = isZip64 ? Zip64.INT_MAGIC : Ints.longToUint(uncompressedSize);
        int csize = isZip64 ? Zip64.INT_MAGIC : Ints.longToUint(compressedSize);
        int offset = isZip64 ? Zip64.INT_MAGIC : Ints.longToUint(location.first);

        ByteBuffer extra = buildExtraField();

        buf.putInt(SIGNATURE);
        buf.putShort(versionMadeBy);
        buf.putShort(versionNeeded);
        buf.putShort((short) 0); // flag
        buf.putShort(compressionFlag);
        buf.putShort(DEFAULT_TIME);
        buf.putShort(DEFAULT_DATE);
        buf.putInt(crc);
        buf.putInt(csize); // compressed size
        buf.putInt(size); // size
        buf.putShort(Ints.intToUshort(nameBytes.length));
        buf.putShort(Ints.intToUshort(extra.capacity()));
        buf.putShort((short) 0); // comment size
        buf.putShort((short) 0); // disk # start
        buf.putShort((short) 0); // internal att
        buf.putInt(externalAttribute);
        buf.putInt(offset);
        buf.put(nameBytes);
        buf.put(extra);
    }

    short getCompressionFlag() {
        return compressionFlag;
    }

    long getSize() {
        long extraSize = isZip64 ? ZIP64_EXTRA_SIZE : 0;
        return SIZE + nameBytes.length + extraSize;
    }

    @Nonnull
    Location getPayloadLocation() {
        return payloadLocation;
    }

    @Nonnull
    Location getLocation() {
        return location;
    }

    @Nonnull
    private ByteBuffer buildExtraField() {
        if (!isZip64) {
            return ByteBuffer.allocate(0);
        }
        ByteBuffer buf = ByteBuffer.allocate(ZIP64_EXTRA_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(Zip64.EXTRA_ID);
        buf.putShort(Ints.intToUshort(ZIP64_PAYLOAD_SIZE));
        buf.putLong(uncompressedSize);
        buf.putLong(compressedSize);
        buf.putLong(location.first);
        buf.rewind();
        return buf;
    }
}
