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

public class Zip64Locator {

    private static final int SIGNATURE = 0x07064b50;
    public static final int SIZE = 20;

    static final int TOTAL_NUMBER_DISK = EndOfCentralDirectory.DISK_NUMBER + 1;

    private Location location;
    private long offsetToEOCD64;

    private Zip64Locator() {
        location = Location.INVALID;
        offsetToEOCD64 = 0;
    }

    @Nonnull
    public Location getLocation() {
        return location;
    }

    public long getOffsetToEOCD64() {
        return offsetToEOCD64;
    }

    public static Location write(@Nonnull ZipWriter writer, @Nonnull Location eocdLocation)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(SIGNATURE);
        buffer.putInt(0); // CD disk number
        buffer.putLong(eocdLocation.first); // offset
        buffer.putInt(TOTAL_NUMBER_DISK);
        buffer.rewind();

        long position = writer.position();
        writer.write(buffer);
        return new Location(position, SIZE);
    }

    @Nonnull
    static Zip64Locator find(@Nonnull FileChannel channel, @Nonnull EndOfCentralDirectory eocd)
            throws IOException {
        Zip64Locator locator = new Zip64Locator();
        Location locatorLocation = new Location(eocd.getLocation().first - SIZE, SIZE);
        long fileSize = channel.size();
        if (locatorLocation.last >= fileSize) {
            return locator;
        }
        if (locatorLocation.first < 0) {
            return locator;
        }

        ByteBuffer locatorBuffer = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(locatorBuffer, locatorLocation.first);
        locatorBuffer.rewind();

        if (locator.parse(locatorBuffer)) {
            locator.location = locatorLocation;
        }
        return locator;
    }

    private boolean parse(@Nonnull ByteBuffer buffer) {
        int signature = buffer.getInt();
        if (signature != SIGNATURE) {
            return false;
        }

        buffer.position(buffer.position() + 4); // skip CD disk number
        offsetToEOCD64 = Ints.ulongToLong(buffer.getLong());
        // Don't read the rest, this is not needed

        return true;
    }
}
