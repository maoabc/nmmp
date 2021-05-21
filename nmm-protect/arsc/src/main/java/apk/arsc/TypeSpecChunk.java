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

import javax.annotation.Nullable;

import apk.base.Preconditions;

/** A chunk that contains a collection of resource entries for a particular resource data type. */
public class TypeSpecChunk extends Chunk {

  /** Flag indicating that a resource entry is public. */
  private static final int SPEC_PUBLIC = 0x40000000;

  /** The id of the resource type that this type spec refers to. */
  private int id;

  /** Flags for entries at a given index. */
  private int[] resources;

  protected TypeSpecChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);
    id = buffer.get() & 0xFF;
    buffer.position(buffer.position() + 3);  // Skip 3 bytes for packing
    int resourceCount = buffer.getInt();
    resources = new int[resourceCount];
    for (int i = 0; i < resourceCount; ++i) {
      resources[i] = buffer.getInt();
    }
  }

  /**
   * Returns the (1-based) type id of the resources that this {@link TypeSpecChunk} has
   * configuration masks for.
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
    id = newId;
  }

  /** Returns the number of resource entries that this chunk has configuration masks for. */
  public int getResourceCount() {
    return getResources().length;
  }

  @Override
  protected Type getType() {
    return Type.TABLE_TYPE_SPEC;
  }

  @Override
  protected void writeHeader(ByteBuffer output) {
    super.writeHeader(output);
    // id is an unsigned byte in the range [0-255]. It is guaranteed to be non-negative.
    // Because our output is in little-endian, we are making use of the 4 byte packing here
    output.putInt(id);
    output.putInt(resources.length);
  }

  @Override
  protected void writePayload(DataOutput output, ByteBuffer header, int options)
      throws IOException {
    final int resourceMask =
        ((options & SerializableResource.PRIVATE_RESOURCES) != 0) ? ~SPEC_PUBLIC : ~0;
    for (int resource : getResources()) {
      output.writeInt(resource & resourceMask);
    }
  }

  /** Resource configuration masks. */
  public int[] getResources() {
    return resources;
  }

  public void setResources(int[] resources) {
    this.resources = resources;
  }
}
