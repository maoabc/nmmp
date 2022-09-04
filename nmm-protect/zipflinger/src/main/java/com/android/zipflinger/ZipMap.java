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
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class ZipMap {
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private CentralDirectory cd = null;

    // To build an accurate location of entries in the zip payload, data descriptors must be read.
    // This is not useful if an user only wants a list of entries in the zip but it is mandatory
    // if zip entries are deleted/added.
    private final boolean accountDataDescriptors;

    private final Path zipFile;
    private long fileSize;

    private Location payloadLocation;
    private Location cdLocation;
    private Location eocdLocation;

    @Nonnull private EndOfCentralDirectory eocd;

    static final String LFH_LENGTH_ERROR =
            "The provided zip (%s) is invalid. Entry '%s' name field is %d bytes"
                    + " in the Central Directory but %d in the Local File Header";

    private ZipMap(@Nonnull Path zipFile, boolean accountDataDescriptors) {
        this.zipFile = zipFile;
        this.accountDataDescriptors = accountDataDescriptors;
    }

    @Nonnull
    public static ZipMap from(@Nonnull Path zipFile) throws IOException {
        return from(zipFile, false);
    }

    /** @deprecated Use {@link #from(Path)} instead. */
    @Deprecated
    @Nonnull
    public static ZipMap from(@Nonnull File zipFile) throws IOException {
        return from(zipFile.toPath(), false);
    }

    @Nonnull
    public static ZipMap from(@Nonnull Path zipFile, boolean accountDataDescriptors)
            throws IOException {
        return from(zipFile, accountDataDescriptors, Zip64.Policy.ALLOW);
    }

    @Nonnull
    public static ZipMap from(
            @Nonnull Path zipFile, boolean accountDataDescriptors, Zip64.Policy policy)
            throws IOException {
        ZipMap map = new ZipMap(zipFile, accountDataDescriptors);
        map.parse(policy);
        return map;
    }

    @Nonnull
    public Location getPayloadLocation() {
        return payloadLocation;
    }

    @Nonnull
    public Location getCdLoc() {
        return cdLocation;
    }

    @Nonnull
    public Location getEocdLoc() {
        return eocdLocation;
    }

    private void parse(Zip64.Policy policy) throws IOException {
        try (FileChannel channel = FileChannel.open(zipFile, StandardOpenOption.READ)) {
            fileSize = channel.size();

            eocd = EndOfCentralDirectory.find(channel);
            if (!eocd.getLocation().isValid()) {
                throw new IllegalStateException(
                        String.format("Could not find EOCD in '%s'", zipFile));
            }
            eocdLocation = eocd.getLocation();
            cdLocation = eocd.getCdLocation();

            // Check if this is a zip64 archive
            Zip64Locator locator = Zip64Locator.find(channel, eocd);
            if (locator.getLocation().isValid()) {
                if (policy == Zip64.Policy.FORBID) {
                    String message =
                            String.format("Cannot parse forbidden zip64 archive %s", zipFile);
                    throw new IllegalStateException(message);
                }
                Zip64Eocd zip64EOCD = Zip64Eocd.parse(channel, locator.getOffsetToEOCD64());
                cdLocation = zip64EOCD.getCdLocation();
                if (!cdLocation.isValid()) {
                    String message = String.format("Zip64Locator led to bad EOCD64 in %s", zipFile);
                    throw new IllegalStateException(message);
                }
            }

            if (!cdLocation.isValid()) {
                throw new IllegalStateException(
                        String.format("Could not find CD in '%s'", zipFile));
            }

            parseCentralDirectory(channel, cdLocation, policy);

            payloadLocation = new Location(0, cdLocation.first);
        }
    }

    private void parseCentralDirectory(
            @Nonnull FileChannel channel, @Nonnull Location location, Zip64.Policy policy)
            throws IOException {
        if (location.size() > Integer.MAX_VALUE) {
            throw new IllegalStateException("CD larger than 2GiB not supported");
        }
        int size = Math.toIntExact(location.size());
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(buf, location.first);
        buf.rewind();

        while (buf.remaining() >= 4 && buf.getInt() == CentralDirectoryRecord.SIGNATURE) {
            Entry entry = new Entry();
            parseCentralDirectoryRecord(buf, channel, entry);
            if (!entry.getName().isEmpty()) {
                entries.put(entry.getName(), entry);
            }
            checkPolicy(entry, policy);
        }

        cd = new CentralDirectory(buf, entries);

        sanityCheck(location);
    }

    private static void checkPolicy(@Nonnull Entry entry, Zip64.Policy policy) {
        if (policy == Zip64.Policy.ALLOW) {
            return;
        }

        if (entry.getUncompressedSize() > Zip64.LONG_MAGIC
                || entry.getCompressedSize() > Zip64.LONG_MAGIC
                || entry.getLocation().first > Zip64.LONG_MAGIC) {
            String message =
                    String.format(
                            "Entry %s infringes forbidden zip64 policy (size=%d, csize=%d, loc=%s)",
                            entry.getName(),
                            entry.getUncompressedSize(),
                            entry.getCompressedSize(),
                            entry.getLocation());
            throw new IllegalStateException(message);
        }
    }

    private void sanityCheck(Location cdLocation) {
        //Sanity check that:
        //  - All payload locations are within the file (and not in the CD).
        for (Entry e : entries.values()) {
            Location loc = e.getLocation();
            if (loc.first < 0) {
                throw new IllegalStateException("Invalid first loc '" + e.getName() + "' " + loc);
            }
            if (loc.last >= fileSize) {
                throw new IllegalStateException(
                        fileSize + "Invalid last loc '" + e.getName() + "' " + loc);
            }
            Location cdLoc = e.getCdLocation();
            if (cdLoc.first < 0) {
                throw new IllegalStateException(
                        "Invalid first cdloc '" + e.getName() + "' " + cdLoc);
            }
            long cdSize = cdLocation.size();
            if (cdLoc.last >= cdSize) {
                throw new IllegalStateException(
                        cdSize + "Invalid last loc '" + e.getName() + "' " + cdLoc);
            }
        }
    }

    @Nonnull
    public Map<String, Entry> getEntries() {
        return entries;
    }

    @Nonnull
    CentralDirectory getCentralDirectory() {
        return cd;
    }

    @Nonnull
    byte[] getComment() {
        return eocd.getComment();
    }

    public void parseCentralDirectoryRecord(
            @Nonnull ByteBuffer buf, @Nonnull FileChannel channel, @Nonnull Entry entry)
            throws IOException {
        long cdEntryStart = (long) buf.position() - 4;

        entry.setVersionMadeBy(buf.getShort());
        buf.getShort(); // versionNeededToExtract
        short flags = buf.getShort();
        short compressionFlag = buf.getShort();
        entry.setCompressionFlag(compressionFlag);
        buf.position(buf.position() + 4);
        //short modTime = buf.getShort();
        //short modDate = buf.getShort();

        int crc = buf.getInt();
        entry.setCrc(crc);

        entry.setCompressedSize(Ints.uintToLong(buf.getInt()));
        entry.setUncompressedSize(Ints.uintToLong(buf.getInt()));

        int pathLength = Ints.ushortToInt(buf.getShort());
        int extraLength = Ints.ushortToInt(buf.getShort());
        int commentLength = Ints.ushortToInt(buf.getShort());
        buf.position(buf.position() + 4);
        // short diskNumber = buf.getShort();
        // short intAttributes = buf.getShort();
        entry.setExternalAttributes(buf.getInt());

        entry.setLocation(new Location(Ints.uintToLong(buf.getInt()), 0));

        parseName(buf, pathLength, entry);

        // Process extra field. If the entry is zip64, this may change size, csize, and offset.
        if (extraLength > 0) {
            int position = buf.position();
            int limit = buf.limit();
            buf.limit(position + extraLength);
            parseExtra(buf.slice(), entry);
            buf.limit(limit);
            buf.position(position + extraLength);
        }

        // Skip comment field
        buf.position(buf.position() + commentLength);

        // Retrieve the local header extra size since there are no guarantee it is the same as the
        // central directory size.
        // Semi-paranoid mode: Also check that the local name size is the same as the cd name size.
        ByteBuffer localFieldBuffer =
                readLocalFields(
                        entry.getLocation().first + LocalFileHeader.OFFSET_TO_NAME, entry, channel);
        int localPathLength = Ints.ushortToInt(localFieldBuffer.getShort());
        int localExtraLength = Ints.ushortToInt(localFieldBuffer.getShort());
        if (pathLength != localPathLength) {
            String path = this.zipFile.toAbsolutePath().toString();
            String entryName = entry.getName();
            String msg = LFH_LENGTH_ERROR;
            String message = String.format(msg, path, entryName, localPathLength, pathLength);
            throw new IllegalStateException(message);
        }

        // At this point we have everything we need to calculate payload location.
        boolean isCompressed = compressionFlag != 0;
        long payloadSize = isCompressed ? entry.getCompressedSize() : entry.getUncompressedSize();
        long start = entry.getLocation().first;
        long end =
                start
                        + LocalFileHeader.LOCAL_FILE_HEADER_SIZE
                        + pathLength
                        + localExtraLength
                        + payloadSize;
        entry.setLocation(new Location(start, end - start));

        Location payloadLocation =
                new Location(
                        start
                                + LocalFileHeader.LOCAL_FILE_HEADER_SIZE
                                + pathLength
                                + localExtraLength,
                        payloadSize);
        entry.setPayloadLocation(payloadLocation);

        // At this point we have everything we need to calculate CD location.
        long cdEntrySize =
                (long) CentralDirectoryRecord.SIZE + pathLength + extraLength + commentLength;
        entry.setCdLocation(new Location(cdEntryStart, cdEntrySize));

        // Parse data descriptor to adjust crc, compressed size, and uncompressed size.
        boolean hasDataDescriptor =
                ((flags & CentralDirectoryRecord.DATA_DESCRIPTOR_FLAG)
                        == CentralDirectoryRecord.DATA_DESCRIPTOR_FLAG);
        if (hasDataDescriptor) {
            if (accountDataDescriptors) {
                // This is expensive. Fortunately ZIP archive rarely use DD nowadays.
                channel.position(end);
                parseDataDescriptor(channel, entry);
            } else {
                entry.setLocation(Location.INVALID);
            }
        }
    }

    private static void parseExtra(@Nonnull ByteBuffer buf, @Nonnull Entry entry) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        while (buf.remaining() >= 4) {
            short id = buf.getShort();
            int size = Ints.ushortToInt(buf.getShort());
            if (id == Zip64.EXTRA_ID) {
                parseZip64Extra(buf, entry);
            }
            if (buf.remaining() >= size) {
                buf.position(buf.position() + size);
            }
        }
    }

    private static void parseZip64Extra(@Nonnull ByteBuffer buf, @Nonnull Entry entry) {
        if (entry.getUncompressedSize() == Zip64.LONG_MAGIC) {
            if (buf.remaining() < 8) {
                throw new IllegalStateException("Bad zip64 extra for entry " + entry.getName());
            }
            entry.setUncompressedSize(Ints.ulongToLong(buf.getLong()));
        }
        if (entry.getCompressedSize() == Zip64.LONG_MAGIC) {
            if (buf.remaining() < 8) {
                throw new IllegalStateException("Bad zip64 extra for entry " + entry.getName());
            }
            entry.setCompressedSize(Ints.ulongToLong(buf.getLong()));
        }
        if (entry.getLocation().first == Zip64.LONG_MAGIC) {
            if (buf.remaining() < 8) {
                throw new IllegalStateException("Bad zip64 extra for entry " + entry.getName());
            }
            long offset = Ints.ulongToLong(buf.getLong());
            entry.setLocation(new Location(offset, 0));
        }
    }

    private ByteBuffer readLocalFields(long offset, Entry entry, FileChannel channel)
            throws IOException {
        // The extra field is not guaranteed to be the same in the LFH and in the CDH. In practice there is
        // often padding space that is not in the CD. We need to read the LFH.
        ByteBuffer localFieldsBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        if (offset < 0 || (offset + 4) > fileSize) {
            throw new IllegalStateException(
                    "Entry :" + entry.getName() + " invalid offset (" + offset + ")");
        }
        channel.read(localFieldsBuffer, offset);
        localFieldsBuffer.rewind();
        return localFieldsBuffer;
    }

    private static void parseName(@Nonnull ByteBuffer buf, int length, @Nonnull Entry entry) {
        byte[] pathBytes = new byte[length];
        buf.get(pathBytes);
        entry.setNameBytes(pathBytes);
    }

    private static void parseDataDescriptor(@Nonnull FileChannel channel, @Nonnull Entry entry)
            throws IOException {
        // If zip entries have data descriptor, we need to go an fetch every single entry to look if
        // the "optional" marker is there. Adjust zip entry area accordingly.

        ByteBuffer dataDescriptorBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(dataDescriptorBuffer);
        dataDescriptorBuffer.rewind();

        int dataDescriptorLength = 12;
        if (dataDescriptorBuffer.getInt() == CentralDirectoryRecord.DATA_DESCRIPTOR_SIGNATURE) {
            dataDescriptorLength += 4;
        }

        Location adjustedLocation =
                new Location(
                        entry.getLocation().first,
                        entry.getLocation().size() + dataDescriptorLength);
        entry.setLocation(adjustedLocation);
    }

    @Nonnull
    public Path getPath() {
        return zipFile;
    }

    /** @deprecated Use {@link #getPath()} instead. */
    @Deprecated
    @Nonnull
    public File getFile() {
        return zipFile.toFile();
    }
}
