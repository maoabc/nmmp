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
import java.nio.charset.StandardCharsets;

public abstract class Source {
    private final String name;
    private final byte[] nameBytes;

    public static final long NO_ALIGNMENT = 0;
    private long alignment = NO_ALIGNMENT;

    protected long compressedSize;
    protected long uncompressedSize;
    protected int crc;
    protected short compressionFlag;

    protected short versionMadeBy;
    public static final short MADE_BY_UNIX = 3 << 8;

    // For more of these magical values, see zipinfo.c in unzip source code.
    // All these values are shifted left 16 bits because this is where they
    // are expected in the zip external attribute field.
    private static final int TYPE_FREG = 0100000 << 16; // Regular File
    private static final int TYPE_FLNK = 0120000 << 16; // Symbolic link
    private static final int TYPE_FDIR = 0040000 << 16; // Directory

    private static final int UNX_IRUSR = 00400 << 16; /* Unix read    : owner */
    private static final int UNX_IWUSR = 00200 << 16; /* Unix write   : owner */
    private static final int UNX_IXUSR = 00100 << 16; /* Unix execute : owner */
    private static final int UNX_IRGRP = 00040 << 16; /* Unix read    : group */
    private static final int UNX_IWGRP = 00020 << 16; /* Unix write   : group */
    private static final int UNX_IXGRP = 00010 << 16; /* Unix execute : group */
    private static final int UNX_IROTH = 00004 << 16; /* Unix read    : other */
    private static final int UNX_IWOTH = 00002 << 16; /* Unix write   : other */
    private static final int UNX_IXOTH = 00001 << 16; /* Unix execute : other */
    private static final int UNX_IRALL = UNX_IRUSR | UNX_IRGRP | UNX_IROTH;
    private static final int UNX_IWALL = UNX_IWUSR | UNX_IWGRP | UNX_IWOTH;
    private static final int UNX_IXALL = UNX_IXUSR | UNX_IXGRP | UNX_IXOTH;

    public static final int PERMISSION_USR_RW = UNX_IRUSR | UNX_IWUSR;
    public static final int PERMISSION_RW = UNX_IRALL | UNX_IWALL;
    
    public static final int PERMISSION_EXEC = UNX_IXALL;
    public static final int PERMISSION_LINK = TYPE_FLNK;

    // -rw_r__r__
    public static final int PERMISSION_DEFAULT = TYPE_FREG | UNX_IRALL | UNX_IWUSR;

    // drwxr_xr_x
    public static final int PERMISSION_DIR_DEFAULT = TYPE_FDIR | UNX_IRALL | UNX_IXALL | UNX_IWUSR;

    protected int externalAttributes;

    static final String DIRECTORY_MARKER = "/";

    protected Source(@Nonnull String name) {
        this.name = name;
        nameBytes = name.getBytes(StandardCharsets.UTF_8);
        versionMadeBy = MADE_BY_UNIX;
        externalAttributes = PERMISSION_DEFAULT;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    byte[] getNameBytes() {
        return nameBytes;
    }

    boolean isAligned() {
        return alignment != NO_ALIGNMENT;
    }

    public void align(long alignment) {
        this.alignment = alignment;
    }

    long getAlignment() {
        return alignment;
    }

    int getCrc() {
        return crc;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public long getUncompressedSize() {
        return uncompressedSize;
    }

    short getCompressionFlag() {
        return compressionFlag;
    }

    public short getVersionMadeBy() {
        return versionMadeBy;
    }

    public int getExternalAttributes() {
        return externalAttributes;
    }

    public void setExternalAttributes(int externalAttributes) {
        this.externalAttributes = externalAttributes;
    }

    // Guaranteed to be called before writeTo. After this method has been called, every fields
    // in an entry must be known (csize, size, crc32, and compressionFlag).
    public abstract void prepare() throws IOException;

    // Return the number of bytes written.
    public abstract long writeTo(@Nonnull ZipWriter writer) throws IOException;

    static boolean isNameDirectory(String name) {
        return name.endsWith(DIRECTORY_MARKER);
    }

    static String directoryName(@Nonnull String name) {
        return name + DIRECTORY_MARKER;
    }
}
