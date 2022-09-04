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
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class LargeFileSource extends Source {

    private final Path transferSrc;
    private final int compressionLevel;

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    // Does not load the whole file in memory. If the entry is not compressed, only read it to
    // compute the CRC32 and zero-copy src when needed. If compression is requested, uses a tmp
    // storage to store the deflated payload then zero-copy it when needed.
    public LargeFileSource(
            @Nonnull Path src,
            @Nullable Path tmpStorage,
            @Nonnull String name,
            int compressionLevel)
            throws IOException {
        super(name);
        this.compressionLevel = compressionLevel;

        if (tmpStorage == null && compressionLevel != Deflater.NO_COMPRESSION) {
            String msg = "Compression without a provided tmp Path is not supported";
            throw new IllegalStateException(msg);
        }

        try (CheckedInputStream in =
                new CheckedInputStream(Files.newInputStream(src), new CRC32())) {
            if (compressionLevel == Deflater.NO_COMPRESSION) {
                buildStored(in);
                transferSrc = src;
            } else {
                buildCompressed(in, compressionLevel, tmpStorage);
                transferSrc = tmpStorage;
            }
            // At this point the input file has been completely read. We can request the crc32.
            crc = Ints.longToUint(in.getChecksum().getValue());
        }
    }

    public LargeFileSource(@Nonnull Path src, @Nonnull String name, int compressionLevel)
            throws IOException {
        this(src, getTmpStoragePath(src.getFileName().toString()), name, compressionLevel);
    }

    @Nonnull
    public static Path getTmpStoragePath(@Nonnull String entryName) {
        StringBuilder filename = new StringBuilder();
        filename.append(Integer.toHexString(entryName.hashCode()));
        filename.append("-");
        filename.append(Thread.currentThread().getId());
        filename.append("-");
        filename.append(System.nanoTime());
        filename.append(".tmp");

        Path tmp = Paths.get(TMP_DIR, filename.toString());
        if (Files.exists(tmp)) {
            String msg = String.format(Locale.US, "Cannot use path '%s' (exists)", tmp);
            throw new IllegalStateException(msg);
        }
        return tmp;
    }

    private void buildStored(@Nonnull InputStream in) throws IOException {
        byte[] buffer = new byte[4096];
        long inputSize = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            inputSize += read;
        }
        compressedSize = inputSize;
        uncompressedSize = compressedSize;
        compressionFlag = LocalFileHeader.COMPRESSION_NONE;
    }

    private void buildCompressed(@Nonnull InputStream in, int compressionLevel, @Nonnull Path tmp)
            throws IOException {
        // Make sure we are not going to overwrite another tmp file.
        if (Files.exists(tmp)) {
            String msg = String.format("Tmp storage '%s' already exists", tmp.toAbsolutePath());
            throw new IllegalStateException(msg);
        }

        // Pipe the src into the tmp compressed file.
        Deflater deflater = Compressor.getDeflater(compressionLevel);
        try (DeflaterOutputStream out =
                new DeflaterOutputStream(
                        Files.newOutputStream(tmp, StandardOpenOption.CREATE_NEW), deflater)) {

            // Just in case we crash before writeTo is called, attempt to clean up on VM exit.
            tmp.toFile().deleteOnExit();

            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        compressedSize = deflater.getBytesWritten();
        uncompressedSize = deflater.getBytesRead();
        compressionFlag = LocalFileHeader.COMPRESSION_DEFLATE;
    }

    @Override
    public void prepare() throws IOException {}

    @Override
    public long writeTo(@Nonnull ZipWriter writer) throws IOException {
        try (FileChannel src = FileChannel.open(transferSrc, StandardOpenOption.READ)) {
            writer.transferFrom(src, 0, this.compressedSize);
            return this.compressedSize;
        } finally {
            if (compressionLevel != Deflater.NO_COMPRESSION) {
                Files.delete(transferSrc);
            }
        }
    }
}
