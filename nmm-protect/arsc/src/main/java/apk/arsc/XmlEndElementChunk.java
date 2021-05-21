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
import java.util.Locale;
import javax.annotation.Nullable;

/** Represents the end of an XML node. */
public final class XmlEndElementChunk extends XmlNodeChunk {

  /** A string reference to the namespace URI, or -1 if not present. */
  private final int namespace;

  /** A string reference to the attribute name. */
  private final int name;

  protected XmlEndElementChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);
    namespace = buffer.getInt();
    name = buffer.getInt();
  }

  /** Returns the namespace URI, or the empty string if no namespace is present. */
  public String getNamespace() {
    return getString(namespace);
  }

  /** Returns the attribute name. */
  public String getName() {
    return getString(name);
  }

  @Override
  protected Type getType() {
    return Type.XML_END_ELEMENT;
  }

  @Override
  protected void writePayload(DataOutput output, ByteBuffer header, int options)
      throws IOException {
    super.writePayload(output, header, options);
    output.writeInt(namespace);
    output.writeInt(name);
  }

  /**
   * Returns a brief description of this XML node. The representation of this information is
   * subject to change, but below is a typical example:
   *
   * <pre>
   * "XmlEndElementChunk{line=1234, comment=My awesome comment., namespace=foo, name=bar}"
   * </pre>
   */
  @Override
  public String toString() {
    return String.format(
        Locale.US,
        "XmlEndElementChunk{line=%d, comment=%s, namespace=%s, name=%s}",
        getLineNumber(),
        getComment(),
        getNamespace(),
        getName());
  }
}
