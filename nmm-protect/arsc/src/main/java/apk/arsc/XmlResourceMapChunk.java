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
import java.util.List;

import javax.annotation.Nullable;

import apk.base.Preconditions;

/**
 * Represents an XML resource map chunk.
 *
 * <p>This chunk maps attribute ids to the resource ids of the attribute resource that defines the
 * attribute (e.g. type, enum values, etc.).
 */
public class XmlResourceMapChunk extends Chunk {

  /** The size of a resource reference for {@code resources} in bytes. */
  private static final int RESOURCE_SIZE = 4;

  /**
   * Contains a mapping of attributeID to resourceID. For example, the attributeID 2 refers to the
   * resourceID returned by {@code resources.get(2)}.
   */
  private final List<Integer> resources = new ArrayList<>();

  protected XmlResourceMapChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);
  }

  @Override
  protected void init(ByteBuffer buffer) {
    super.init(buffer);
    resources.addAll(enumerateResources(buffer));
  }

  private List<Integer> enumerateResources(ByteBuffer buffer) {
    int resourceCount = (getOriginalChunkSize() - getHeaderSize()) / RESOURCE_SIZE;
    List<Integer> result = new ArrayList<>(resourceCount);
    int offset = this.offset + getHeaderSize();
    buffer.mark();
    buffer.position(offset);

    for (int i = 0; i < resourceCount; ++i) {
      result.add(buffer.getInt());
    }

    buffer.reset();
    return result;
  }

  /**
   * Returns the resource ID that {@code attributeId} maps to iff {@link #hasResourceId} returns
   * true for the given {@code attributeId}.
   */
  public ResourceIdentifier getResourceId(int attributeId) {
    Preconditions.checkArgument(hasResourceId(attributeId), "Attribute ID is not a valid index.");
    return ResourceIdentifier.create(resources.get(attributeId));
  }

  /** Returns true if a resource ID exists for the given {@code attributeId}. */
  public boolean hasResourceId(int attributeId) {
    return attributeId >= 0 && resources.size() > attributeId;
  }

  @Override
  protected Type getType() {
    return Type.XML_RESOURCE_MAP;
  }

  @Override
  protected void writePayload(DataOutput output, ByteBuffer header, int options)
      throws IOException {
    super.writePayload(output, header, options);
    for (Integer resource : resources) {
      output.writeInt(resource);
    }
  }
}
