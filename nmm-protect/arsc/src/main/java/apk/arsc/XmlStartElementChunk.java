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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import apk.base.Preconditions;

/** Represents the beginning of an XML node. */
public final class XmlStartElementChunk extends XmlNodeChunk {

  /** A string reference to the namespace URI, or -1 if not present. */
  private final int namespace;

  /** A string reference to the element name that this chunk represents. */
  private final int name;

  /** The offset to the start of the attributes payload. */
  private final int attributeStart;

  /** The number of attributes in the original buffer. */
  private final int attributeCount;

  /** The (0-based) index of the id attribute, or -1 if not present. */
  private final int idIndex;

  /** The (0-based) index of the class attribute, or -1 if not present. */
  private final int classIndex;

  /** The (0-based) index of the style attribute, or -1 if not present. */
  private final int styleIndex;

  /** The XML attributes associated with this element.  */
  private final List<XmlAttribute> attributes = new ArrayList<>();

  protected XmlStartElementChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);
    namespace = buffer.getInt();
    name = buffer.getInt();
    attributeStart = (buffer.getShort() & 0xFFFF);
    int attributeSize = (buffer.getShort() & 0xFFFF);
    Preconditions.checkState(attributeSize == XmlAttribute.SIZE,
        "attributeSize is wrong size. Got %s, want %s", attributeSize, XmlAttribute.SIZE);
    attributeCount = (buffer.getShort() & 0xFFFF);

    // The following indices are 1-based and need to be adjusted.
    idIndex = (buffer.getShort() & 0xFFFF) - 1;
    classIndex = (buffer.getShort() & 0xFFFF) - 1;
    styleIndex = (buffer.getShort() & 0xFFFF) - 1;
  }

  @Override
  protected void init(ByteBuffer buffer) {
    super.init(buffer);
    attributes.addAll(enumerateAttributes(buffer));
  }

  private List<XmlAttribute> enumerateAttributes(ByteBuffer buffer) {
    List<XmlAttribute> result = new ArrayList<>(attributeCount);
    int offset = this.offset + getHeaderSize() + attributeStart;
    int endOffset = offset + XmlAttribute.SIZE * attributeCount;
    buffer.mark();
    buffer.position(offset);

    while (offset < endOffset) {
      result.add(XmlAttribute.create(buffer, this));
      offset += XmlAttribute.SIZE;
    }

    buffer.reset();
    return result;
  }

  /**
   * Remaps all the attribute references using the supplied remapping. If an attribute has a
   * reference to a resourceid that is in the remapping keys, it will be updated with the
   * corresponding value from the remapping. All attributes that do not have reference to
   * a value in the remapping are left as is.
   * @param remapping The original and new resource ids.
   */
  public void remapReferences(Map<Integer, Integer> remapping) {
    Map<Integer, XmlAttribute> newEntries = new HashMap<>();
    int count = 0;
    for (XmlAttribute attribute : attributes) {
      ResourceValue value = attribute.typedValue();
      if (value.type() == ResourceValue.Type.REFERENCE) {
        int valueData = value.data();
        if (ResourceIdentifier.create(valueData).packageId() != 0x1) {
          if (remapping.containsKey(valueData)) {
            int data = Preconditions.checkNotNull(remapping.get(valueData));
            XmlAttribute newAttribute = XmlAttribute.create(
                    attribute.namespaceIndex(),
                    attribute.nameIndex(),
                    attribute.rawValueIndex(),
                    attribute.typedValue().withData(data),
                    attribute.parent());
            newEntries.put(count, newAttribute);
          }
        }
      }
      count++;
    }
    for (Entry<Integer, XmlAttribute> entry : newEntries.entrySet()) {
      attributes.set(entry.getKey(), entry.getValue());
    }
  }

  public void setAttribute(int index, XmlAttribute attribute) {
    attributes.set(index, attribute);
  }

  /** Returns the namespace URI, or the empty string if not present. */
  public String getNamespace() {
    return getString(namespace);
  }

  /** Returns the element name that this chunk represents. */
  public String getName() {
    return getString(name);
  }

  /** Returns an unmodifiable list of this XML element's attributes. */
  public List<XmlAttribute> getAttributes() {
    return Collections.unmodifiableList(attributes);
  }

  @Override
  protected Type getType() {
    return Type.XML_START_ELEMENT;
  }

  @Override
  protected void writePayload(DataOutput output, ByteBuffer header, int options)
      throws IOException {
    super.writePayload(output, header, options);
    output.writeInt(namespace);
    output.writeInt(name);
    output.writeShort((short) XmlAttribute.SIZE);  // attribute start
    output.writeShort((short) XmlAttribute.SIZE);
    output.writeShort((short) attributes.size());
    output.writeShort((short) (idIndex + 1));
    output.writeShort((short) (classIndex + 1));
    output.writeShort((short) (styleIndex + 1));
    for (XmlAttribute attribute : attributes) {
      output.write(attribute.toByteArray(options));
    }
  }

  /**
   * Returns a brief description of this XML node. The representation of this information is
   * subject to change, but below is a typical example:
   *
   * <pre>
   * "XmlStartElementChunk{line=1234, comment=My awesome comment., namespace=foo, name=bar, ...}"
   * </pre>
   */
  @Override
  public String toString() {
    return String.format(
        Locale.US,
        "XmlStartElementChunk{line=%d, comment=%s, namespace=%s, name=%s, attributes=%s}",
        getLineNumber(),
        getComment(),
        getNamespace(),
        getName(),
        attributes.toString());
  }
}
