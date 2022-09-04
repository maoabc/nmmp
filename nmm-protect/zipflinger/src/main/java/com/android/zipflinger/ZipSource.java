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
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

public class ZipSource {
    public static final int COMPRESSION_NO_CHANGE = -2;
    private FileChannel channel;
    private ZipMap map;

    private final List<Source> selectedEntries = new ArrayList<>();

    public ZipSource(ZipMap map) {
        this.map = map;
    }

    public ZipSource(@Nonnull Path file) throws IOException {
        this(ZipMap.from(file, false));
    }

    @Nonnull
    public void select(@Nonnull String entryName, @Nonnull String newName) {
        select(entryName, newName, COMPRESSION_NO_CHANGE, Source.NO_ALIGNMENT);
    }

    /**
     * Select an entry to be copied to the archive managed by zipflinger.
     *
     * <p>An entry will remain unchanged and zero-copy will happen when: - compression level is
     * COMPRESSION_NO_CHANGE. - compression level is 1-9 and the entry is already compressed. -
     * compression level is Deflater.NO_COMPRESSION and the entry is already uncompressed.
     *
     * <p>Otherwise, the entry is deflated/inflated accordingly via transfer to memory, crc
     * calculation , and written to the target archive.
     *
     * @param entryName Name of the entry in the source zip.
     * @param newName Name of the entry in the destination zip.
     * @param compressionLevel The desired compression level.
     * @return
     */
    @Nonnull
    public void select(
            @Nonnull String entryName,
            @Nonnull String newName,
            int compressionLevel,
            long alignment) {
        Entry entry = map.getEntries().get(entryName);
        if (entry == null) {
            throw new IllegalStateException(
                    String.format("Cannot find '%s' in archive '%s'", entryName, map.getPath()));
        }
        Source entrySource = newZipSourceEntryFor(newName, entry, compressionLevel);
        entrySource.align(alignment);
        entrySource.versionMadeBy = entry.getVersionMadeBy();
        entrySource.externalAttributes = entry.getExternalAttributes();
        selectedEntries.add(entrySource);
    }

    public Map<String, Entry> entries() {
        return map.getEntries();
    }

    public static ZipSource selectAll(@Nonnull Path file) throws IOException {
        ZipSource source = new ZipSource(file);
        for (Entry e : source.entries().values()) {
            source.select(e.getName(), e.getName(), COMPRESSION_NO_CHANGE, Source.NO_ALIGNMENT);
        }
        return source;
    }

    void open() throws IOException {
        channel = FileChannel.open(map.getPath(), StandardOpenOption.READ);
    }

    void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
    }

    FileChannel getChannel() {
        return channel;
    }

    public List<? extends Source> getSelectedEntries() {
        return selectedEntries;
    }

    @Nonnull
    private Source newZipSourceEntryFor(String newName, Entry entry, int compressionLevel) {

        //    Source       Destination
        //    ========================
        //     X           NO_CHANGE   -> No changes
        //     INFLATED    INFLATED    -> No changes
        //     INFLATED    DEFLATED    -> Deflate
        //     DEFLATED    INFLATED    -> Inflate
        //     DEFLATED    DEFLATED    -> Inflate then Deflate

        if (compressionLevel == COMPRESSION_NO_CHANGE
                || (!entry.isCompressed() && compressionLevel == Deflater.NO_COMPRESSION)) {
            return new ZipSourceEntry(newName, entry, this);
        }

        return new ZipSourceEntryPipe(newName, entry, this, compressionLevel);
    }

    String getName() {
        return map.getPath().toString();
    }

    @Nonnull
    public byte[] getComment() {
        return map.getComment();
    }
}
