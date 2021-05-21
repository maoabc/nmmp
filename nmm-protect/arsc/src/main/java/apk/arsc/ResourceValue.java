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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import apk.base.Preconditions;

/** Represents a single typed resource value. */
@AutoValue
public abstract class ResourceValue implements SerializableResource {

  /** Resource type codes. */
  public enum Type {
    /** {@code data} is either 0 (undefined) or 1 (empty). */
    NULL(0x00),
    /** {@code data} holds a {@link ResourceTableChunk} entry reference. */
    REFERENCE(0x01),
    /** {@code data} holds an attribute resource identifier. */
    ATTRIBUTE(0x02),
    /** {@code data} holds an index into the containing resource table's string pool. */
    STRING(0x03),
    /** {@code data} holds a single-precision floating point number. */
    FLOAT(0x04),
    /** {@code data} holds a complex number encoding a dimension value, such as "100in". */
    DIMENSION(0x05),
    /** {@code data} holds a complex number encoding a fraction of a container. */
    FRACTION(0x06),
    /** {@code data} holds a dynamic {@link ResourceTableChunk} entry reference. */
    DYNAMIC_REFERENCE(0x07),
    /** {@code data} holds a dynamic attribute resource identifier. */
    DYNAMIC_ATTRIBUTE(0x08),
    /** {@code data} is a raw integer value of the form n..n. */
    INT_DEC(0x10),
    /** {@code data} is a raw integer value of the form 0xn..n. */
    INT_HEX(0x11),
    /** {@code data} is either 0 (false) or 1 (true). */
    INT_BOOLEAN(0x12),
    /** {@code data} is a raw integer value of the form #aarrggbb. */
    INT_COLOR_ARGB8(0x1c),
    /** {@code data} is a raw integer value of the form #rrggbb. */
    INT_COLOR_RGB8(0x1d),
    /** {@code data} is a raw integer value of the form #argb. */
    INT_COLOR_ARGB4(0x1e),
    /** {@code data} is a raw integer value of the form #rgb. */
    INT_COLOR_RGB4(0x1f);

    private final byte code;

    private static final Map<Byte, Type> FROM_BYTE;

    static {
      Map<Byte, Type> map = new HashMap<>();
      for (Type type : values()) {
        map.put(type.code(), type);
      }
      FROM_BYTE = Collections.unmodifiableMap(map);
    }

    Type(int code) {
      this.code = (byte) code;
    }

    public byte code() {
      return code;
    }

    public static Type fromCode(byte code) {
      return Preconditions.checkNotNull(FROM_BYTE.get(code), "Unknown resource type: %s", code);
    }
  }

  /** The serialized size in bytes of a {@link ResourceValue}. */
  public static final int SIZE = 8;

  /** The length in bytes of this value. */
  public abstract int size();

  /** The raw data type of this value. */
  public abstract Type type();

  /** The actual 4-byte value; interpretation of the value depends on {@code dataType}. */
  public abstract int data();

  /** A builder for {@link ResourceValue} instances. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder size(int s);

    public abstract Builder type(Type t);

    public abstract Builder data(int d);

    public abstract ResourceValue build();
  }

  /** Returns a new, empty builder for {@link ResourceValue} instances. */
  public static Builder builder() {
    return new AutoValue_ResourceValue.Builder();
  }

  abstract Builder toBuilder();

  ResourceValue withData(int d) {
    return toBuilder().data(d).build();
  }

  public static ResourceValue create(ByteBuffer buffer) {
    int size = (buffer.getShort() & 0xFFFF);
    buffer.get();  // Unused
    Type type = Type.fromCode(buffer.get());
    int data = buffer.getInt();
    return builder().size(size).type(type).data(data).build();
  }

  @Override
  public byte[] toByteArray() {
    return toByteArray(SerializableResource.NONE);
  }

  @Override
  public byte[] toByteArray(int options) {
    ByteBuffer buffer = ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort((short) size());
    buffer.put((byte) 0);  // Unused
    buffer.put(type().code());
    buffer.putInt(data());
    return buffer.array();
  }

  private String dataHexString() {
    return String.format("0x%08x", data());
  }

  @Override
  public String toString() {
    switch (type()) {
      case NULL:
        return data() == 0 ? "null" : "empty";
      case REFERENCE:
        return "ref(" + dataHexString() + ")";
      case ATTRIBUTE:
        return "attr(" + dataHexString() + ")";
      case STRING:
        return "string(" + dataHexString() + ")";
      case FLOAT:
        return "float(" + data() + ")";
      case DIMENSION:
        return "dimen(" + data() + ")";
      case FRACTION:
        return "frac(" + data() + ")";
      case DYNAMIC_REFERENCE:
        return "dynref(" + dataHexString() + ")";
      case DYNAMIC_ATTRIBUTE:
        return "dynattr(" + dataHexString() + ")";
      case INT_DEC:
        return "dec(" + data() + ")";
      case INT_HEX:
        return "hex(" + dataHexString() + ")";
      case INT_BOOLEAN:
        return "bool(" + data() + ")";
      case INT_COLOR_ARGB8:
        return "argb8(" + dataHexString() + ")";
      case INT_COLOR_RGB8:
        return "rgb8(" + dataHexString() + ")";
      case INT_COLOR_ARGB4:
        return "argb4(" + dataHexString() + ")";
      case INT_COLOR_RGB4:
        return "rgb4(" + dataHexString() + ")";
      default:
        return "<invalid value>";
    }
  }
}
