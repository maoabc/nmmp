/*
 * Copyright (C) 2021 The Android Open Source Project
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;

public class Sources {

    // The general maximum amount of memory (in bytes) a Source should hold onto.
    public static final int LARGE_LIMIT = 100_000_000;

    public static Source from(File file, @Nonnull String name, int compressionLevel)
            throws IOException {
        return from(file.toPath(), name, compressionLevel);
    }

    public static Source dir(@Nonnull String name) throws IOException {
        if (!Source.isNameDirectory(name)) {
            name = Source.directoryName(name);
        }
        Source source = new BytesSource(new byte[0], name, Deflater.NO_COMPRESSION);
        source.setExternalAttributes(Source.PERMISSION_DIR_DEFAULT);
        return source;
    }

    public static Source from(Path path, @Nonnull String name, int compressionLevel)
            throws IOException {
        if (Files.size(path) > LARGE_LIMIT) {
            return new LargeFileSource(path, name, compressionLevel);
        } else {
            return new BytesSource(path, name, compressionLevel);
        }
    }

    public static Source from(InputStream in, String name, int compressionLevel)
            throws IOException {
        return from(in, name, compressionLevel, LARGE_LIMIT);
    }

    public static Source from(InputStream in, String name, int compressionLevel, int largeLimit)
            throws IOException {
        return new StreamSource(in, name, compressionLevel, largeLimit);
    }
}
