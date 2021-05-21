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

/**
 * A chunk whose contents are unknown. This is a placeholder until we add a proper chunk for the
 * unknown type.
 */
public final class UnknownChunk extends Chunk {

  private final Type type;

  private final byte[] header;

  private final byte[] payload;

  protected UnknownChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);

    type = Type.fromCode(buffer.getShort(offset));
    header = new byte[headerSize - Chunk.METADATA_SIZE];
    payload = new byte[chunkSize - headerSize];
    buffer.get(header);
    buffer.get(payload);
  }

  @Override
  protected void writeHeader(ByteBuffer output) {
    output.put(header);
  }

  @Override
  protected void writePayload(DataOutput output, ByteBuffer header, int options)
      throws IOException {
    output.write(payload);
  }

  @Override
  protected Type getType() {
    return type;
  }
}
