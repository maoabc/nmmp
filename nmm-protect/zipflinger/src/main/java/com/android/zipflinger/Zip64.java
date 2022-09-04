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

public class Zip64 {
    static final short EXTRA_ID = 0x0001;

    static final long LONG_MAGIC = 0xFF_FF_FF_FFL;
    static final int INT_MAGIC = (int) LONG_MAGIC;
    static final int SHORT_MAGIC = (short) LONG_MAGIC;

    static final short VERSION_NEEDED = 0x2D;

    public static boolean needZip64Footer(long numEntries, @Nonnull Location cdLocation) {
        return numEntries > Ints.USHRT_MAX
                || cdLocation.first > Ints.UINT_MAX
                || cdLocation.size() > Ints.UINT_MAX;
    }

    static void checkFooterPolicy(
            @Nonnull Policy policy, long numEntries, @Nonnull Location cdLocation) {
        if (policy == Policy.ALLOW) {
            return;
        }

        if (numEntries > Ints.USHRT_MAX) {
            String msg =
                    String.format("Too many zip entries %d (MAX=%d)", numEntries, Ints.USHRT_MAX);
            throw new IllegalStateException(msg);
        }

        if (cdLocation.first > Ints.UINT_MAX) {
            String msg =
                    String.format(
                            "Zip32 cannot place Central directory at offset %d (MAX=%d)",
                            cdLocation.first, Ints.UINT_MAX);
            throw new IllegalStateException(msg);
        }

        if (cdLocation.size() > Ints.UINT_MAX) {
            String msg =
                    String.format(
                            "Zip32 cannot write Central Directory of size %d (MAX=%d)",
                            cdLocation.size(), Ints.UINT_MAX);
            throw new IllegalStateException(msg);
        }
    }

    static void checkEntryPolicy(
            @Nonnull Policy policy,
            @Nonnull Source source,
            @Nonnull Location cdloc,
            @Nonnull Location payloadLoc) {
        if (policy == Policy.ALLOW) {
            return;
        }

        if (source.getUncompressedSize() > Zip64.LONG_MAGIC) {
            String msg =
                    String.format(
                            "Zip32 cannot handle entry '%s' compressed size %d (MAX=%d)",
                            source.getName(), source.getUncompressedSize(), LONG_MAGIC);
            throw new IllegalStateException(msg);
        }

        if (source.getCompressedSize() > Zip64.LONG_MAGIC) {
            String msg =
                    String.format(
                            "Zip32 cannot handle entry '%s' size %d (MAX=%d)",
                            source.getName(), source.getCompressedSize(), LONG_MAGIC);
            throw new IllegalStateException(msg);
        }

        if (cdloc.first > Zip64.LONG_MAGIC) {
            String msg =
                    String.format(
                            "Zip32 cannot place CD entry '%s' payload at %d (MAX=%d)",
                            source.getName(), cdloc.first, LONG_MAGIC);
            throw new IllegalStateException(msg);
        }

        if (payloadLoc.first > Zip64.LONG_MAGIC) {
            String msg =
                    String.format(
                            "Zip32 cannot place entry '%s' payload at %d (MAX=%d)",
                            source.getName(), payloadLoc.first, LONG_MAGIC);
            throw new IllegalStateException(msg);
        }
    }

    public enum Policy {
        ALLOW,
        FORBID
    };

    private Zip64() {}
}
