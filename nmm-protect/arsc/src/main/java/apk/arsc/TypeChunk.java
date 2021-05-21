/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package apk.arsc;

import com.google.auto.value.AutoValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import apk.base.Preconditions;
import apk.io.LittleEndianDataOutputStream;

/**
 * Represents a type chunk, which contains the resource values for a specific resource type and
 * configuration in a {@link PackageChunk}. The resource values in this chunk correspond to the
 * array of type strings in the enclosing {@link PackageChunk}.
 *
 * <p>A {@link PackageChunk} can have multiple of these chunks for different (configuration,
 * resource type) combinations.
 */
public class TypeChunk extends Chunk {

    /**
     * If set, the entries in this chunk are sparse and encode both the entry ID and offset into each
     * entry. Available on platforms >= O. Note that this only changes how the {@link TypeChunk} is
     * encoded / decoded.
     */
    private static final int FLAG_SPARSE = 1 << 0;

    /**
     * The size of a TypeChunk's header in bytes.
     */
    static final int HEADER_SIZE = Chunk.METADATA_SIZE + 12 + ResourceConfiguration.SIZE;

    /**
     * The type identifier of the resource type this chunk is holding.
     */
    private int id;

    /**
     * Flags for a type chunk, such as whether or not this chunk has sparse entries.
     */
    private int flags;

    /**
     * The number of resources of this type.
     */
    private int entryCount;

    /**
     * The offset (from {@code offset}) in the original buffer where {@code entries} start.
     */
    private final int entriesStart;

    /**
     * The resource configuration that these resource entries correspond to.
     */
    private ResourceConfiguration configuration;

    /**
     * A sparse list of resource entries defined by this chunk.
     */
    protected final Map<Integer, Entry> entries = new TreeMap<>();

    protected TypeChunk(ByteBuffer buffer, @Nullable Chunk parent) {
        super(buffer, parent);
        id = buffer.get() & 0xFF;
        flags = buffer.get() & 0xFF;
        buffer.position(buffer.position() + 2); // Skip 2 bytes (reserved)
        entryCount = buffer.getInt();
        entriesStart = buffer.getInt();
        configuration = ResourceConfiguration.create(buffer);
    }

    @Override
    protected void init(ByteBuffer buffer) {
        int offset = this.offset + entriesStart;
        if (hasSparseEntries()) {
            initSparseEntries(buffer, offset);
        } else {
            initDenseEntries(buffer, offset);
        }
    }

    private void initSparseEntries(ByteBuffer buffer, int offset) {
        for (int i = 0; i < entryCount; ++i) {
            // Offsets are stored as (offset / 4u).
            // (See android::ResTable_sparseTypeEntry)
            int index = (buffer.getShort() & 0xFFFF);
            int entryOffset = (buffer.getShort() & 0xFFFF) * 4;
            Entry entry = Entry.create(buffer, offset + entryOffset, this, index);
            entries.put(index, entry);
        }
    }

    private void initDenseEntries(ByteBuffer buffer, int offset) {
        for (int i = 0; i < entryCount; ++i) {
            int entryOffset = buffer.getInt();
            if (entryOffset == Entry.NO_ENTRY) {
                continue;
            }
            Entry entry = Entry.create(buffer, offset + entryOffset, this, i);
            entries.put(i, entry);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TypeChunk[id:").append(id).append(", typeName:").append(getTypeName())
                .append(", configuration:").append(getConfiguration())
                .append(", originalEntryCount:").append(getTotalEntryCount())
                .append(", entries:");
        for (Map.Entry<Integer, Entry> entry : entries.entrySet()) {
            builder.append("<").append(entry.getKey()).append("->").append(entry.getValue()).append("> ");
        }
        builder.append("]");
        return builder.toString();
    }

    public void setEntries(Map<Integer, Entry> entries, int totalCount) {
        this.entries.clear();
        this.entries.putAll(entries);
        entryCount = totalCount;
    }

    /**
     * Returns the (1-based) type id of the resource types that this {@link TypeChunk} is holding.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the id of this chunk.
     *
     * @param newId The new id to use.
     */
    public void setId(int newId) {
        // Ids are 1-based.
        Preconditions.checkState(newId >= 1);
        // Ensure that there is a type defined for this id.
        Preconditions.checkState(
                Preconditions.checkNotNull(getPackageChunk()).getTypeStringPool().getStringCount()
                        >= newId);
        id = newId;
    }

    /**
     * Returns true if the entries in this chunk are encoded in a sparse array.
     */
    public boolean hasSparseEntries() {
        return (flags & FLAG_SPARSE) != 0;
    }

    /**
     * If {@code sparseEntries} is true, this chunk's entries will be encoded in a sparse array. Else,
     * this chunk's entries will be encoded in a dense array.
     */
    public void setSparseEntries(boolean sparseEntries) {
        flags = (flags & ~FLAG_SPARSE) | (sparseEntries ? FLAG_SPARSE : 0);
    }

    /**
     * Returns the name of the type this chunk represents (e.g. string, attr, id).
     */
    public String getTypeName() {
        PackageChunk packageChunk = getPackageChunk();
        Preconditions.checkNotNull(packageChunk, "%s has no parent package.", getClass());
        return packageChunk.getTypeString(getId());
    }

    /**
     * Returns the resource configuration that these resource entries correspond to.
     */
    public ResourceConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the resource configuration that this chunk's entries correspond to.
     *
     * @param configuration The new configuration.
     */
    public void setConfiguration(ResourceConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the total number of entries for this type + configuration, including null entries.
     */
    public int getTotalEntryCount() {
        return entryCount;
    }

    /**
     * Sets the total number of entries, including null entries
     */
    public void setTotalEntryCount(int newEntryCount) {
        entryCount = newEntryCount;
    }

    /**
     * Returns a sparse list of 0-based indices to resource entries defined by this chunk.
     */
    public Map<Integer, Entry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    /**
     * Returns true if this chunk contains an entry for {@code resourceId}.
     */
    public boolean containsResource(ResourceIdentifier resourceId) {
        PackageChunk packageChunk = Preconditions.checkNotNull(getPackageChunk());
        int packageId = packageChunk.getId();
        int typeId = getId();
        return resourceId.packageId() == packageId
                && resourceId.typeId() == typeId
                && entries.containsKey(resourceId.entryId());
    }

    /**
     * Overrides the entries in this chunk at the given index:entry pairs in {@code entries}. For
     * example, if the current list of entries is {0: foo, 1: bar, 2: baz}, and {@code entries} is {1:
     * qux, 3: quux}, then the entries will be changed to {0: foo, 1: qux, 2: baz}. If an entry has an
     * index that does not exist in the dense entry list, then it is considered a no-op for that
     * single entry.
     *
     * @param entries A sparse list containing index:entry pairs to override.
     */
    @SuppressWarnings("nullness") // Checker loses Entry by the time of the for loop :(
    public void overrideEntries(Map<Integer, Entry> entries) {
        for (Map.Entry<Integer, Entry> entry : entries.entrySet()) {
            int index = entry.getKey() != null ? entry.getKey() : -1;
            overrideEntry(index, entry.getValue());
        }
    }

    /**
     * Overrides an entry at the given index. Passing null for the {@code entry} will remove that
     * entry from {@code entries}. Indices < 0 or >= {@link #getTotalEntryCount()} are a no-op.
     *
     * @param index The 0-based index for the entry to override.
     * @param entry The entry to override, or null if the entry should be removed at this location.
     */
    public void overrideEntry(int index, @Nullable Entry entry) {
        if (index >= 0 && index < entryCount) {
            if (entry != null) {
                entries.put(index, entry);
            } else {
                entries.remove(index);
            }
        }
    }

    protected String getString(int index) {
        ResourceTableChunk resourceTable = getResourceTableChunk();
        Preconditions.checkNotNull(resourceTable, "%s has no resource table.", getClass());
        return resourceTable.getStringPool().getString(index);
    }

    protected String getKeyName(int index) {
        PackageChunk packageChunk = getPackageChunk();
        Preconditions.checkNotNull(packageChunk, "%s has no parent package.", getClass());
        StringPoolChunk keyPool = packageChunk.getKeyStringPool();
        Preconditions.checkNotNull(keyPool, "%s's parent package has no key pool.", getClass());
        return keyPool.getString(index);
    }

    @Nullable
    private ResourceTableChunk getResourceTableChunk() {
        Chunk chunk = getParent();
        while (chunk != null && !(chunk instanceof ResourceTableChunk)) {
            chunk = chunk.getParent();
        }
        return chunk != null && chunk instanceof ResourceTableChunk ? (ResourceTableChunk) chunk : null;
    }

    /**
     * Returns the package enclosing this chunk, if any. Else, returns null.
     */
    @Nullable
    public PackageChunk getPackageChunk() {
        Chunk chunk = getParent();
        while (chunk != null && !(chunk instanceof PackageChunk)) {
            chunk = chunk.getParent();
        }
        return chunk != null && chunk instanceof PackageChunk ? (PackageChunk) chunk : null;
    }

    @Override
    protected Type getType() {
        return Type.TABLE_TYPE;
    }

    /**
     * Returns the number of bytes needed for offsets based on {@code entries}.
     */
    private int getOffsetSize() {
        return entryCount * 4;
    }

    private int writeEntries(DataOutput payload, ByteBuffer offsets, int options)
            throws IOException {
        int entryOffset = 0;
        if (hasSparseEntries()) {
            for (Map.Entry<Integer, Entry> mapEntry : entries.entrySet()) {
                Entry entry = mapEntry.getValue();
                byte[] encodedEntry = entry.toByteArray(options);
                payload.write(encodedEntry);
                offsets.putShort((short) (mapEntry.getKey() & 0xFFFF));
                offsets.putShort((short) (entryOffset / 4));
                entryOffset += encodedEntry.length;
                // In order for sparse entries to work, entryOffset must always be a multiple of 4.
                Preconditions.checkState(entryOffset % 4 == 0);
            }
        } else {
            for (int i = 0; i < entryCount; ++i) {
                Entry entry = entries.get(i);
                if (entry == null) {
                    offsets.putInt(Entry.NO_ENTRY);
                } else {
                    byte[] encodedEntry = entry.toByteArray(options);
                    payload.write(encodedEntry);
                    offsets.putInt(entryOffset);
                    entryOffset += encodedEntry.length;
                }
            }
        }
        entryOffset = writePad(payload, entryOffset);
        return entryOffset;
    }

    @Override
    protected void writeHeader(ByteBuffer output) {
        int entriesStart = getHeaderSize() + getOffsetSize();
        output.put((byte) id);
        output.put((byte) flags);
        output.putShort((short) 0); // Write 2 bytes for padding / reserved.
        output.putInt(entryCount);
        output.putInt(entriesStart);
        output.put(configuration.toByteArray());
    }

    @Override
    protected void writePayload(DataOutput output, ByteBuffer header, int options)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer offsets = ByteBuffer.allocate(getOffsetSize()).order(ByteOrder.LITTLE_ENDIAN);
        try (LittleEndianDataOutputStream payload = new LittleEndianDataOutputStream(baos)) {
            writeEntries(payload, offsets, options);
        }
        output.write(offsets.array());
        output.write(baos.toByteArray());
    }

    /**
     * An {@link Entry} in a {@link TypeChunk}. Contains one or more {@link ResourceValue}.
     */
    @AutoValue
    public abstract static class Entry implements SerializableResource {

        /**
         * An entry offset that indicates that a given resource is not present.
         */
        public static final int NO_ENTRY = 0xFFFFFFFF;

        /**
         * Set if this is a complex resource. Otherwise, it's a simple resource.
         */
        private static final int FLAG_COMPLEX = 0x0001;

        /**
         * Set if this is a public resource, which allows libraries to reference it.
         */
        static final int FLAG_PUBLIC = 0x0002;

        /**
         * Size of a single resource id + value mapping entry.
         */
        private static final int MAPPING_SIZE = 4 + ResourceValue.SIZE;

        /**
         * Size of a simple resource
         */
        public static final int SIMPLE_HEADERSIZE = 8;

        /**
         * Size of a complex resource
         */
        public static final int COMPLEX_HEADER_SIZE = 16;

        /**
         * Number of bytes in the header of the {@link Entry}.
         */
        public abstract int headerSize();

        /**
         * Resource entry flags.
         */
        public abstract int flags();

        /**
         * Index into {@link PackageChunk#getKeyStringPool} identifying this entry.
         */
        public abstract int keyIndex();

        /**
         * The value of this resource entry, if this is not a complex entry. Else, null.
         */
        @Nullable
        public abstract ResourceValue value();

        /**
         * The extra values in this resource entry if this {@link #isComplex}.
         */
        public abstract Map<Integer, ResourceValue> values();

        /**
         * Entry into {@link PackageChunk} that is the parent {@link Entry} to this entry.
         * This value only makes sense when this is complex ({@link #isComplex} returns true).
         */
        public abstract int parentEntry();

        /**
         * The {@link TypeChunk} that this resource entry belongs to.
         */
        public abstract TypeChunk parent();

        /**
         * The entry's index into the parent TypeChunk.
         */
        public abstract int typeChunkIndex();

        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder headerSize(int h);

            abstract Builder flags(int f);

            abstract Builder keyIndex(int k);

            abstract Builder value(@Nullable ResourceValue r);

            abstract Builder values(Map<Integer, ResourceValue> v);

            abstract Builder parentEntry(int p);

            abstract Builder parent(TypeChunk p);

            abstract Builder typeChunkIndex(int typeChunkIndex);

            abstract Entry build();
        }

        public static Builder builder() {
            return new AutoValue_TypeChunk_Entry.Builder();
        }

        public abstract Builder toBuilder();

        public Entry withKeyIndex(int keyIndex) {
            return toBuilder().keyIndex(keyIndex).build();
        }

        public Entry withValue(@Nullable ResourceValue value) {
            return toBuilder().value(value).build();
        }

        public Entry withValues(Map<Integer, ResourceValue> values) {
            return toBuilder().values(values).build();
        }

        /**
         * Returns the name of the type this chunk represents (e.g. string, attr, id).
         */
        public final String typeName() {
            return parent().getTypeName();
        }

        /**
         * The total number of bytes that this {@link Entry} takes up.
         */
        public final int size() {
            return headerSize() + (isComplex() ? values().size() * MAPPING_SIZE : ResourceValue.SIZE);
        }

        /**
         * Returns the key name identifying this resource entry.
         */
        public final String key() {
            return parent().getKeyName(keyIndex());
        }

        /**
         * Returns true if this is a complex resource.
         */
        public final boolean isComplex() {
            return (flags() & FLAG_COMPLEX) != 0;
        }

        /**
         * Returns true if this is a public resource.
         */
        public final boolean isPublic() {
            return (flags() & FLAG_PUBLIC) != 0;
        }

        /**
         * Creates a new {@link Entry} whose contents start at {@code offset} in the given {@code
         * buffer}.
         *
         * @param buffer         The buffer to read {@link Entry} from.
         * @param offset         Offset into the buffer where {@link Entry} is located.
         * @param parent         The {@link TypeChunk} that this resource entry belongs to.
         * @param typeChunkIndex The entry's index into the parent TypeChunk.
         * @return New {@link Entry}.
         */
        public static Entry create(
                ByteBuffer buffer, int offset, TypeChunk parent, int typeChunkIndex) {
            int position = buffer.position();
            buffer.position(offset); // Set buffer position to resource entry start
            Entry result = newInstance(buffer, parent, typeChunkIndex);
            buffer.position(position);  // Restore buffer position
            return result;
        }

        private static Entry newInstance(ByteBuffer buffer, TypeChunk parent, int typeChunkIndex) {
            int headerSize = buffer.getShort() & 0xFFFF;
            int flags = buffer.getShort() & 0xFFFF;
            int keyIndex = buffer.getInt();
            ResourceValue value = null;
            Map<Integer, ResourceValue> values = new LinkedHashMap<>();
            int parentEntry = 0;
            if ((flags & FLAG_COMPLEX) != 0) {
                parentEntry = buffer.getInt();
                int valueCount = buffer.getInt();
                for (int i = 0; i < valueCount; ++i) {
                    values.put(buffer.getInt(), ResourceValue.create(buffer));
                }
            } else {
                value = ResourceValue.create(buffer);
            }
            return builder()
                    .headerSize(headerSize)
                    .flags(flags)
                    .keyIndex(keyIndex)
                    .value(value)
                    .values(values)
                    .parentEntry(parentEntry)
                    .parent(parent)
                    .typeChunkIndex(typeChunkIndex)
                    .build();
        }

        @Override
        public final byte[] toByteArray() {
            return toByteArray(SerializableResource.NONE);
        }

        @Override
        public final byte[] toByteArray(int options) {
            ByteBuffer buffer = ByteBuffer.allocate(size());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putShort((short) headerSize());
            final int flagMask =
                    ((options & SerializableResource.PRIVATE_RESOURCES) != 0) ? ~FLAG_PUBLIC : ~0;
            buffer.putShort((short) (flags() & flagMask));
            buffer.putInt(keyIndex());
            if (isComplex()) {
                buffer.putInt(parentEntry());
                buffer.putInt(values().size());
                for (Map.Entry<Integer, ResourceValue> entry : values().entrySet()) {
                    buffer.putInt(entry.getKey());
                    buffer.put(entry.getValue().toByteArray(options));
                }
            } else {
                ResourceValue value = value();
                Preconditions.checkNotNull(value, "A non-complex TypeChunk entry must have a value.");
                buffer.put(value.toByteArray());
            }
            return buffer.array();
        }

        @Override
        public final String toString() {
            return String.format("Entry{key=%s,value=%s,values=%s}", key(), value(), values());
        }
    }
}
