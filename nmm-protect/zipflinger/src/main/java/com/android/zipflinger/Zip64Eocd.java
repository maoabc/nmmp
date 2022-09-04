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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class Zip64Eocd {
    private static final int SIGNATURE = 0x06064b50;
    static final int SIZE = 56;

    private long numEntries;
    private Location cdLocation;

    public Zip64Eocd(long numEntries, @Nonnull Location cdLocation) {
        this.numEntries = numEntries;
        this.cdLocation = cdLocation;
    }

    private Zip64Eocd() {
        this(0, Location.INVALID);
    }

    @Nonnull
    public Location write(@Nonnull ZipWriter writer) throws IOException {
        ByteBuffer eocd = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);
        eocd.putInt(SIGNATURE);
        eocd.putLong(SIZE - 12); // Peculiar specs mandate not to include 12 bytes already written.
        eocd.putShort((short) 0); // Version made by
        eocd.putShort(Zip64.VERSION_NEEDED); // Version needed to extract
        eocd.putInt(0); // disk #
        eocd.putInt(0); // total # of disks
        eocd.putLong(numEntries); // # entries in cd on this disk
        eocd.putLong(numEntries); // total # entries in cd
        eocd.putLong(cdLocation.size()); // CD offset.
        eocd.putLong(cdLocation.first); // size of CD.
        eocd.rewind();

        long position = writer.position();
        writer.write(eocd);

        return new Location(position, SIZE);
    }

    @Nonnull
    Location getCdLocation() {
        return cdLocation;
    }

    @Nonnull
    static Zip64Eocd parse(@Nonnull FileChannel channel, long eocdOffset) throws IOException {
        Zip64Eocd zip64Eocd = new Zip64Eocd();
        long fileSize = channel.size();
        if (eocdOffset < 0 || eocdOffset + SIZE > fileSize) {
            return zip64Eocd;
        }

        ByteBuffer buffer = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(buffer, eocdOffset);
        buffer.rewind();

        int signature = buffer.getInt(); // signature
        if (signature != SIGNATURE) {
            return zip64Eocd;
        }

        // Skip uninteresting fields
        buffer.position(buffer.position() + 28);
        // eocd.getLong();  8 // size of zip64EOCD
        // eocd.getShort(); 2 // Version made by
        // eocd.getShort(); 2 // Version needed to extract
        // eocd.getInt();   4 // disk #
        // eocd.getInt();   4 // total # of disks
        // eocd.getLong();  8 // # entries in cd on this disk
        long numEntries = buffer.getLong(); // total # entries in cd
        long size = Ints.ulongToLong(buffer.getLong()); // size of CD.
        long offset = Ints.ulongToLong(buffer.getLong()); // CD offset.

        zip64Eocd.numEntries = numEntries;
        zip64Eocd.cdLocation = new Location(offset, size);
        return zip64Eocd;
    }
}
