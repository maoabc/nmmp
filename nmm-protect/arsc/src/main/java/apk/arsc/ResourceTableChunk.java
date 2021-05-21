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


import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.annotation.Nullable;

import apk.base.Preconditions;

/**
 * Represents a resource table structure. Its sub-chunks contain:
 *
 * <ul>
 *   <li>A {@link StringPoolChunk} containing all string values in the entire resource table. It
 *       does not, however, contain the names of entries or type identifiers.
 *   <li>One or more {@link PackageChunk}.
 * </ul>
 */
public class ResourceTableChunk extends ChunkWithChunks {

  /** A string pool containing all string resource values in the entire resource table. */
  private StringPoolChunk stringPool;

  protected static final int HEADER_SIZE = Chunk.METADATA_SIZE + 4; // +4 = package count

  /** The packages contained in this resource table. */
  private final Map<String, PackageChunk> packages = new HashMap<>();

  protected ResourceTableChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);
    // packageCount. We ignore this, because we already know how many chunks we have.
    Preconditions.checkState(buffer.getInt() >= 1, "ResourceTableChunk package count was < 1.");
  }

  @Override
  protected void init(ByteBuffer buffer) {
    super.init(buffer);
    setChildChunks();
  }

  protected void setChildChunks() {
    packages.clear();
    for (Chunk chunk : getChunks().values()) {
      if (chunk instanceof PackageChunk) {
        PackageChunk packageChunk = (PackageChunk) chunk;
        packages.put(packageChunk.getPackageName(), packageChunk);
      } else  if (chunk instanceof StringPoolChunk) {
        stringPool = (StringPoolChunk) chunk;
      }
    }
    Preconditions.checkNotNull(stringPool, "ResourceTableChunk must have a string pool.");
  }


  /** Returns the string pool containing all string resource values in the resource table. */
  public StringPoolChunk getStringPool() {
    return stringPool;
  }

  /** Adds the {@link PackageChunk} to this table. */
  public void addPackageChunk(PackageChunk packageChunk) {
    super.add(packageChunk);
    this.packages.put(packageChunk.getPackageName(), packageChunk);
  }

  /**
   * Deletes from the string pool all indices in the passed in collection and remaps the references
   * in every {@link TypeChunk}.
   */
  public void deleteStrings(SortedSet<Integer> indexesToDelete) {
    int[] remappedIndexes = stringPool.deleteStrings(indexesToDelete);
    for (PackageChunk packageChunk : getPackages()) {
      for (TypeChunk typeChunk : packageChunk.getTypeChunks()) {
        TreeMap<Integer, TypeChunk.Entry> replacementEntries = new TreeMap<>();
        for (Map.Entry<Integer, TypeChunk.Entry> entry : typeChunk.getEntries().entrySet()) {
          final Integer index = entry.getKey();
          final TypeChunk.Entry chunkEntry = entry.getValue();
          if (chunkEntry.isComplex()) {
            // An isComplex() Entry can have some values of type STRING and others of other types
            // (e.g. a <style> can have one sub-<item> be a DIMENSION and another sub-<item> be a
            // STRING) so we need to rewrite such Entry's sub-values independently.
            TreeMap<Integer, ResourceValue> newValues = new TreeMap<>();
            for (Map.Entry<Integer, ResourceValue> valuesEntry : chunkEntry.values().entrySet()) {
              Integer key = valuesEntry.getKey();
              ResourceValue value = valuesEntry.getValue();
              if (isString(value)) {
                int newIndex = remappedIndexes[value.data()];
                Preconditions.checkArgument(newIndex >= 0);
                value = value.withData(newIndex);
              }
              newValues.put(key, value);
            }
            // Even if a chunkEntry's values are empty, it is still important and should not be
            // removed here. It's possible that the entry is overriding another entry's values
            // and/or has a different parentEntry.
            replacementEntries.put(index, chunkEntry.withValues(newValues));
          } else {
            ResourceValue value = Preconditions.checkNotNull(chunkEntry.value());
            if (isString(value)) {
              int newIndex = remappedIndexes[value.data()];
              replacementEntries.put(index,
                  newIndex == -1 ? null : chunkEntry.withValue(value.withData(newIndex)));
            }
          }
        }
        typeChunk.overrideEntries(replacementEntries);
      }
    }
  }

  private static boolean isString(ResourceValue value) {
    return value.type() == ResourceValue.Type.STRING;
  }

  /** Returns the package with the given {@code packageName}. Else, returns null. */
  @Nullable
  public PackageChunk getPackage(String packageName) {
    return packages.get(packageName);
  }

  /** Returns the package with the given {@code packageId}. Else, returns null */
  @Nullable
  public PackageChunk getPackage(int packageId) {
    for (PackageChunk chunk : packages.values()) {
      if (chunk.getId() == packageId) {
        return chunk;
      }
    }
    return null;
  }

  /** Returns the packages contained in this resource table. */
  public Collection<PackageChunk> getPackages() {
    return Collections.unmodifiableCollection(packages.values());
  }

  @Override
  protected Type getType() {
    return Type.TABLE;
  }

  @Override
  protected void writeHeader(ByteBuffer output) {
    super.writeHeader(output);
    output.putInt(packages.size());
  }
}
