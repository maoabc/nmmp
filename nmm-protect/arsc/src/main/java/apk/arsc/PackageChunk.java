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


import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.annotation.Nullable;

import apk.base.Preconditions;

/**
 * A package chunk is a collection of resource data types within a package.
 */
public class PackageChunk extends ChunkWithChunks {

    /**
     * Offset in bytes, from the start of the chunk, where {@code typeStringsOffset} can be found.
     */
    protected static final int TYPE_OFFSET_OFFSET = 268;

    /**
     * Offset in bytes, from the start of the chunk, where {@code keyStringsOffset} can be found.
     */
    protected static final int KEY_OFFSET_OFFSET = 276;

    protected static final int HEADER_SIZE = KEY_OFFSET_OFFSET + 12;

    /**
     * The package id if this is a base package, or 0 if not a base package.
     */
    private int id;

    /**
     * The name of the package.
     */
    private String packageName;

    /**
     * The offset (from {@code offset}) in the original buffer where type strings start.
     */
    protected final int typeStringsOffset;

    /**
     * The index into the type string pool of the last public type.
     */
    private final int lastPublicType;

    /**
     * An offset to the string pool that contains the key strings for this package.
     */
    protected final int keyStringsOffset;

    /**
     * The index into the key string pool of the last public key.
     */
    private final int lastPublicKey;

    /**
     * An offset to the type ID(s). This is undocumented in the original code.
     */
    private final int typeIdOffset;

    /**
     * Contains a mapping of a type index to its {@link TypeSpecChunk}.
     */
    private final Map<Integer, TypeSpecChunk> typeSpecs = new HashMap<>();

    /**
     * Contains a mapping of a type index to all of the {@link TypeChunk} with that index.
     */
    private final Map<Integer, Set<TypeChunk>> types = new HashMap<>();

    /**
     * May contain a library chunk for mapping dynamic references to resolved references.
     */
    private LibraryChunk libraryChunk = null;

    protected PackageChunk(ByteBuffer buffer, @Nullable Chunk parent) {
        super(buffer, parent);
        id = buffer.getInt();
        packageName = PackageUtils.readPackageName(buffer, buffer.position());
        typeStringsOffset = buffer.getInt();
        lastPublicType = buffer.getInt();
        keyStringsOffset = buffer.getInt();
        lastPublicKey = buffer.getInt();
        typeIdOffset = buffer.getInt();
    }

    @Override
    protected void init(ByteBuffer buffer) {
        super.init(buffer);
        initializeChildMappings();
    }

    protected void reinitializeChildMappings() {
        types.clear();
        typeSpecs.clear();
        libraryChunk = null;
        initializeChildMappings();
    }

    private void initializeChildMappings() {
        for (Chunk chunk : getChunks().values()) {
            if (chunk instanceof TypeChunk) {
                TypeChunk typeChunk = (TypeChunk) chunk;
                putIntoTypes(typeChunk);
            } else if (chunk instanceof TypeSpecChunk) {
                TypeSpecChunk typeSpecChunk = (TypeSpecChunk) chunk;
                typeSpecs.put(typeSpecChunk.getId(), typeSpecChunk);
            } else if (chunk instanceof LibraryChunk) {
                if (libraryChunk != null) {
                    throw new IllegalStateException("Multiple library chunks present in package chunk.");
                }
                // NB: this is currently unused except for the above assertion that there's <=1 chunk.
                libraryChunk = (LibraryChunk) chunk;
            } else if (!(chunk instanceof StringPoolChunk) && !(chunk instanceof UnknownChunk)) {
                throw new IllegalStateException(
                        String.format("PackageChunk contains an unexpected chunk: %s", chunk.getClass()));
            }
        }
    }

    /**
     * Returns the package id if this is a base package, or 0 if not a base package.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the package id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the string pool that contains the names of the resources in this package.
     */
    public StringPoolChunk getKeyStringPool() {
        Chunk chunk = Preconditions.checkNotNull(getChunks().get(keyStringsOffset + offset));
        Preconditions.checkState(chunk instanceof StringPoolChunk, "Key string pool not found.");
        return (StringPoolChunk) chunk;
    }

    /**
     * Get the type string for a specific id, e.g., (e.g. string, attr, id).
     *
     * @param id The id to get the type for.
     * @return The type string.
     */
    public String getTypeString(int id) {
        StringPoolChunk typePool = getTypeStringPool();
        Preconditions.checkNotNull(typePool, "Package has no type pool.");
        Preconditions.checkState(typePool.strings.size() >= id, "No type for id: " + id);
        return typePool.getString(id - 1); // - 1 here to convert to 0-based index
    }

    /**
     * Returns the string pool that contains the type strings for this package, such as "layout",
     * "string", "color".
     */
    public StringPoolChunk getTypeStringPool() {
        Chunk chunk = Preconditions.checkNotNull(getChunks().get(typeStringsOffset + offset));
        Preconditions.checkState(chunk instanceof StringPoolChunk, "Type string pool not found.");
        return (StringPoolChunk) chunk;
    }

    /**
     * Returns all {@link TypeChunk} in this package.
     */
    public Collection<TypeChunk> getTypeChunks() {
        Set<TypeChunk> typeChunks = new LinkedHashSet<>();
        for (Collection<TypeChunk> chunks : types.values()) {
            typeChunks.addAll(chunks);
        }
        return typeChunks;
    }

    /**
     * For a given type id, returns the {@link TypeChunk} objects that match that id. The type id is
     * the 1-based index of the type in the type string pool (returned by {@link #getTypeStringPool}).
     *
     * @param id The 1-based type id to return {@link TypeChunk} objects for.
     * @return The matching {@link TypeChunk} objects, or an empty collection if there are none.
     */
    public Collection<TypeChunk> getTypeChunks(int id) {
        Set<TypeChunk> chunks = types.get(id);
        return chunks != null ? chunks : Collections.<TypeChunk>emptySet();
    }

    /**
     * For a given type, returns the {@link TypeChunk} objects that match that type
     * (e.g. "attr", "id", "string", ...).
     *
     * @param type The type to return {@link TypeChunk} objects for.
     * @return The matching {@link TypeChunk} objects, or an empty collection if there are none.
     */
    public Collection<TypeChunk> getTypeChunks(String type) {
        StringPoolChunk typeStringPool = Preconditions.checkNotNull(getTypeStringPool());
        return getTypeChunks(typeStringPool.indexOf(type) + 1);  // Convert 0-based index to 1-based
    }

    /**
     * Returns all {@link TypeSpecChunk} in this package.
     */
    public Collection<TypeSpecChunk> getTypeSpecChunks() {
        return typeSpecs.values();
    }

    /**
     * For a given (1-based) type id, returns the {@link TypeSpecChunk} matching it.
     */
    public TypeSpecChunk getTypeSpecChunk(int id) {
        return Preconditions.checkNotNull(typeSpecs.get(id));
    }

    /**
     * For a given {@code type}, returns the {@link TypeSpecChunk} that matches it
     * (e.g. "attr", "id", "string", ...).
     */
    public TypeSpecChunk getTypeSpecChunk(String type) {
        StringPoolChunk typeStringPool = Preconditions.checkNotNull(getTypeStringPool());
        return getTypeSpecChunk(typeStringPool.indexOf(type) + 1);  // Convert 0-based index to 1-based
    }

    /**
     * Returns the name of this package.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Set the package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Removes the {@link TypeChunk} from this package and the collection inherited from the
     * super class.
     */
    public void remove(TypeChunk chunk) {
        super.remove(chunk);
        int chunkId = chunk.getId();
        if (!removeFromTypes(chunk)) {
            throw new IllegalStateException(
                    String.format("Can't remove %s from packageChunk.", chunk.getClass()));
        }

        if (!types.containsKey(chunkId)) {
            TypeSpecChunk specChunk = typeSpecs.remove(chunkId);
            Preconditions.checkNotNull(specChunk, "Typespec chunk not found for id: " + chunkId);
            super.remove(specChunk);
        }
    }

    @Override
    protected Type getType() {
        return Type.TABLE_PACKAGE;
    }

    /**
     * Delete the given keys from this package, rewrite references to these keys from |this|'s
     * TypeChunks, and remove empty TypeChunks. Returns number of TypeChunks deleted.
     */
    public int deleteKeyStrings(SortedSet<Integer> keyIndexesToDelete) {
        int[] remappedIndexes = getKeyStringPool().deleteStrings(keyIndexesToDelete);
        Collection<TypeChunk> typeChunksToDelete = new ArrayList<>();
        for (TypeChunk typeChunk : getTypeChunks()) {
            boolean shouldDeleteTypeChunk = true;
            TreeMap<Integer, TypeChunk.Entry> replacementEntries = new TreeMap<>();
            for (Map.Entry<Integer, TypeChunk.Entry> entry : typeChunk.getEntries().entrySet()) {
                int newIndex = remappedIndexes[entry.getValue().keyIndex()];
                shouldDeleteTypeChunk = shouldDeleteTypeChunk && newIndex == -1;

                replacementEntries.put(
                        entry.getKey(), newIndex == -1 ? null : entry.getValue().withKeyIndex(newIndex));
            }
            typeChunk.overrideEntries(replacementEntries);
            if (shouldDeleteTypeChunk) {
                typeChunksToDelete.add(typeChunk);
            }
        }
        for (TypeChunk typeChunk : typeChunksToDelete) {
            remove(typeChunk);
        }
        return typeChunksToDelete.size();
    }


    @Override
    protected void writeHeader(ByteBuffer output) {
        output.putInt(id);
        PackageUtils.writePackageName(output, packageName);
        output.putInt(0);  // typeStringsOffset. This value can't be computed here.
        output.putInt(lastPublicType);
        output.putInt(0);  // keyStringsOffset. This value can't be computed here.
        output.putInt(lastPublicKey);
        output.putInt(typeIdOffset);
    }

    @Override
    protected void writePayload(DataOutput output, ByteBuffer header, int options)
            throws IOException {
        int typeOffset = typeStringsOffset;
        int keyOffset = keyStringsOffset;
        int payloadOffset = 0;
        for (Chunk chunk : getChunks().values()) {
            if (chunk == getTypeStringPool()) {
                typeOffset = payloadOffset + getHeaderSize();
            } else if (chunk == getKeyStringPool()) {
                keyOffset = payloadOffset + getHeaderSize();
            }
            byte[] chunkBytes = chunk.toByteArray(options);
            output.write(chunkBytes);
            payloadOffset = writePad(output, chunkBytes.length);
        }
        header.putInt(TYPE_OFFSET_OFFSET, typeOffset);
        header.putInt(KEY_OFFSET_OFFSET, keyOffset);
    }

    /**
     * Using {@link types} as a {@code Multimap}, put a {@link TypeChunk} into it. The key is the id
     * of the {@code typeChunk}.
     */
    private void putIntoTypes(TypeChunk typeChunk) {
        Set<TypeChunk> chunks = types.get(typeChunk.getId());
        if (chunks == null) {
            // Some tools require that the default TypeChunk is first in the list. Use a LinkedHashSet
            // to make sure that when we return the chunks they are in original order (in cases
            // where we copy and edit them this is important).
            chunks = new LinkedHashSet<>();
            types.put(typeChunk.getId(), chunks);
        }
        chunks.add(typeChunk);
    }

    /**
     * Using {@link types} as a {@code Multimap}, remove a {@link TypeChunk} from it. The key is the
     * id of the {@code typeChunk}.
     */
    private boolean removeFromTypes(TypeChunk typeChunk) {
        int chunkId = typeChunk.getId();
        Set<TypeChunk> chunks = types.get(chunkId);
        if (chunks == null || !chunks.remove(typeChunk)) {
            return false;
        }
        if (chunks.isEmpty()) {
            types.remove(chunkId);
        }
        return true;
    }
}
