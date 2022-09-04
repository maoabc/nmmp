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
import java.text.NumberFormat;

public class Location implements Comparable<Location> {

    public static final Location INVALID = new Location(Long.MAX_VALUE, Long.MAX_VALUE);

    public final long first;
    public final long last;

    public Location(long first, long size) {
        this.first = first;
        this.last = first + size - 1;
    }

    public long size() {
        return last - first + 1;
    }

    public boolean isValid() {
        return !this.equals(INVALID);
    }

    @Nonnull
    @Override
    public String toString() {
        return "(offset="
                + NumberFormat.getInstance().format(first)
                + ", size="
                + NumberFormat.getInstance().format(size())
                + ")";
    }

    @Override
    public boolean equals(@Nonnull Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Location)) {
            return false;
        }
        Location other = (Location) obj;
        return first == other.first && last == other.last;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(first);
    }

    @Override
    public int compareTo(Location o) {
        return Math.toIntExact(this.first - o.first);
    }
}
