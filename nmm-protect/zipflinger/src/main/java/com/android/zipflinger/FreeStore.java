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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// This works like a memory allocator except it deals with file address space instead of
// memory address space.
class FreeStore {

    static final long DEFAULT_ALIGNMENT = 4;
    static final long PAGE_ALIGNMENT = 4096;

    private Zone head;

    // A zone tracks the free file address space. Two consecutive zones are never contiguous which
    // mean that upon modification, if two zone "touch" each others, they are merged together into
    // a bigger free zone.
    //
    // Used space is not tracked but inferred from each gap between free zones.
    protected static class Zone {
        public Zone next;
        public Zone prev;
        public Location loc;

        public Zone() {
            this.next = null;
            this.prev = null;
        }

        public void shrinkBy(long amount) {
            assert loc.size() > amount;
            loc = new Location(loc.first + amount, loc.size() - amount);

            // If the zone is empty, remove it from the list.
            if (loc.size() == 0) {
                prev.next = next;
                if (next != null) {
                    next.prev = prev;
                }
            }
        }
    }

    FreeStore(@Nonnull Map<String, Entry> zipEntries) {
        // Create an immutable marker of unusable space which make "insert on head" ugly code go away.
        head = new Zone();
        head.loc = new Location(-1, 1);

        // Use zip entries location (used space) to build the free zones list.
        List<Location> usedLocations = new ArrayList<>();
        for (Entry entry : zipEntries.values()) {
            usedLocations.add(entry.getLocation());
        }
        Collections.sort(usedLocations);

        Zone prevFreeZone = head;
        Location prevUsedLoc = prevFreeZone.loc;
        for (Location usedLoc : usedLocations) {
            // If there is a gap, mark is as FREE space.
            long gap = usedLoc.first - prevUsedLoc.last - 1;
            if (gap > 0) {
                Zone free = new Zone();
                prevFreeZone.next = free;
                free.prev = prevFreeZone;
                free.loc = new Location(prevUsedLoc.last + 1, gap);
                prevFreeZone = free;
            }
            prevUsedLoc = usedLoc;
        }

        // Mark everything remaining as a free zone.
        Zone remainingZone = new Zone();
        remainingZone.prev = prevFreeZone;
        remainingZone.next = null;
        prevFreeZone.next = remainingZone;
        remainingZone.loc =
                new Location(prevUsedLoc.last + 1, Long.MAX_VALUE - 1 - prevUsedLoc.last);
    }

    // Performs unaligned allocation.
    @Nonnull
    Location ualloc(long requestedSize) {
        Zone cursor;
        for (cursor = head.next; cursor != null; cursor = cursor.next) {
            // We are searching for a block big enough to contain:
            // - The requested size
            // - Post-padding space for potentially needed virtual entry to fill holes.
            if (cursor.loc.size() >= requestedSize + LocalFileHeader.VIRTUAL_HEADER_SIZE) {
                break;
            }
        }

        if (cursor == null) {
            throw new IllegalStateException("Out of file address space.");
        }

        Location allocated = new Location(cursor.loc.first, requestedSize);
        cursor.shrinkBy(requestedSize);
        return allocated;
    }

    // Performs aligned allocation. The offset is necessary because what needs to be aligned is not
    // the first byte in the allocation but the first byte in the zip entry payload.
    // This method may return more than requested. If it does the extra space is padding that must
    // be consumed by an "extra" field.
    @Nonnull
    Location alloc(long requestedSize, long payloadOffset, long alignment) {
        Zone cursor;
        for (cursor = head.next; cursor != null; cursor = cursor.next) {
            long padding = padFor(cursor.loc.first, payloadOffset, alignment);
            // We are searching for a block big enough to contain:
            // - The requested size
            // - Pre-padding space for extra field ALIGNMENT
            // - Post-padding space for potentially needed virtual entry to fill holes.
            if (cursor.loc.size()
                    >= requestedSize + padding + LocalFileHeader.VIRTUAL_HEADER_SIZE) {
                requestedSize += padding;
                break;
            }
        }

        if (cursor == null) {
            throw new IllegalStateException("Out of file address space.");
        }

        Location allocated = new Location(cursor.loc.first, requestedSize);
        cursor.shrinkBy(requestedSize);
        return allocated;
    }

    // Mark an area of the file available for allocation. This will merge up to two zones into one
    // if they touch each others.
    void free(@Nonnull Location loc) {
        Zone cursor = head.next;
        while (cursor != null) {
            if (loc.first > cursor.prev.loc.last && loc.last < cursor.loc.first) {
                break;
            }
            cursor = cursor.next;
        }

        if (cursor == null) {
            throw new IllegalStateException("Double free");
        }

        // Insert a free zone
        Zone newFreeZone = new Zone();
        newFreeZone.loc = loc;
        newFreeZone.prev = cursor.prev;
        newFreeZone.next = cursor;
        cursor.prev.next = newFreeZone;
        cursor.prev = newFreeZone;
        cursor = newFreeZone;

        // If previous zone is contiguous, merge this zone into previous.
        if (cursor.prev.loc.last + 1 == cursor.loc.first && cursor.prev != head) {
            Zone prev = cursor.prev;
            prev.next = cursor.next;
            cursor.next.prev = prev;
            prev.loc = new Location(prev.loc.first, prev.loc.size() + cursor.loc.size());
            cursor = prev;
        }

        // If next zone is contiguous, merge this zone into next.
        if (cursor.next != null && cursor.next.loc.first - 1 == cursor.loc.last) {
            Zone next = cursor.next;
            next.prev = cursor.prev;
            cursor.prev.next = next;
            next.loc = new Location(cursor.loc.first, cursor.loc.size() + next.loc.size());
        }
    }

    @Nonnull
    Location getLastFreeLocation() {
        Zone zone = head.next;
        while (zone.next != null) {
            zone = zone.next;
        }
        return zone.loc;
    }

    @Nonnull
    List<Location> getFreeLocations() {
        List<Location> locs = new ArrayList<>();
        Zone cursor = head.next;
        while (cursor != null) {
            locs.add(cursor.loc);
            cursor = cursor.next;
        }
        return locs;
    }

    // How much padding is needed if this address+offset is not aligned (a.k.a: An extra field will
    // have to be created in order to fill this space).
    static long padFor(long address, long offset, long alignment) {
        long pointer = address + offset;
        if ((pointer % alignment) == 0) {
            return 0;
        } else {
            return alignment - (pointer % alignment);
        }
    }
}
