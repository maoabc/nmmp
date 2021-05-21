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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

import apk.base.Preconditions;
import apk.io.LittleEndianDataOutputStream;

/** Represents a string pool structure. */
public class StringPoolChunk extends Chunk {

  // These are the defined flags for the "flags" field of ResourceStringPoolHeader
  private static final int SORTED_FLAG = 1 << 0;
  private static final int UTF8_FLAG   = 1 << 8;

  /** The offset from the start of the header that the stylesStart field is at. */
  private static final int STYLE_START_OFFSET = 24;

  /** Flags. */
  private final int flags;

  /** Index from header of the string data. */
  private final int stringsStart;

  /** Index from header of the style data. */
  private final int stylesStart;

  /**
   * Number of strings in the original buffer. This is not necessarily the number of strings in
   * {@code strings}.
   */
  protected final int stringCount;

  /**
   * Number of styles in the original buffer. This is not necessarily the number of styles in {@code
   * styles}.
   */
  protected final int styleCount;

  /**
   * The strings ordered as they appear in the arsc file. e.g. strings.get(1234) gets the 1235th
   * string in the arsc file.
   */
  protected List<String> strings = new ArrayList<>();

  /**
   * These styles have a 1:1 relationship with the strings. For example, styles.get(3) refers to the
   * string at location strings.get(3). There are never more styles than strings (though there may
   * be less). Inside of that are all of the styles referenced by that string.
   */
  protected List<StringPoolStyle> styles = new ArrayList<>();

  protected boolean alwaysDedup = false;

  protected StringPoolChunk(ByteBuffer buffer, @Nullable Chunk parent) {
    super(buffer, parent);
    stringCount = buffer.getInt();
    styleCount = buffer.getInt();
    flags        = buffer.getInt();
    stringsStart = buffer.getInt();
    stylesStart  = buffer.getInt();
  }

  @Override
  protected void init(ByteBuffer buffer) {
    super.init(buffer);
    strings.addAll(readStrings(buffer, offset + stringsStart, stringCount));
    styles.addAll(readStyles(buffer, offset + stylesStart, styleCount));
  }

  /**
   * Returns the 0-based index of the first occurrence of the given string, or -1 if the string is
   * not in the pool. This runs in O(n) time.
   *
   * @param string The string to check the pool for.
   * @return Index of the string, or -1 if not found.
   */
  public int indexOf(String string) {
    return strings.indexOf(string);
  }

  /**
   * Returns a string at the given (0-based) index.
   *
   * @param index The (0-based) index of the string to return.
   * @throws IndexOutOfBoundsException If the index is out of range (index < 0 || index >= size()).
   */
  public String getString(int index) {
    return strings.get(index);
  }

  /**
   * Sets the string at the specific index.
   * @param index The index of the string to update.
   * @param value The new value.
   */
  public void setString(int index, String value) {
    strings.set(index, value);
  }

  /**
   * Adds a string to this string pool.
   * @param value The string to add.
   * @return The (0-based) index of the string.
   */
  public int addString(String value) {
    strings.add(value);
    return strings.size() - 1;
  }

  /** Returns the number of strings in this pool. */
  public int getStringCount() {
    return strings.size();
  }

  /**
   * Remove from the input list any indexes of strings that are referenced by styles not in the
   * input.  This is required because string A's style's span may refer to string B, and removing
   * string B in this scenario would leave a dangling reference from A.
   */
  private void removeIndexesOfStringsWithNameIndexReferencesOutstanding(
      Set<Integer> indexesToDelete) {
    Set<Integer> indexesToSave = new HashSet<>();
    for (int i = 0; i < styles.size(); ++i) {
      if (indexesToDelete.contains(i)) {
        // Style isn't going to survive deletion, so we don't care what its spans' nameIndex()es are
        // pointing at.
        continue;
      }
      StringPoolStyle style = styles.get(i);
      for (StringPoolSpan span : style.spans()) {
        // Ensure we don't delete strings references from surviving styles.
        if (indexesToDelete.contains(span.nameIndex())) {
          indexesToSave.add(span.nameIndex());
        }
      }
    }
    indexesToDelete.removeAll(indexesToSave);
  }

  /**
   * Returns a list of {@code styles} with spans containing remapped string indexes by {@code
   * remappedIndexes}.
   */
  protected static List<StringPoolStyle> fixUpStyles(
      List<StringPoolStyle> styles, int[] remappedIndexes) {
    List<StringPoolStyle> result = new ArrayList<>(styles.size());
    for (StringPoolStyle style : styles) {
      List<StringPoolSpan> newSpans = new ArrayList<>(style.spans().size());
      for (StringPoolSpan span : style.spans()) {
        int newIndex = remappedIndexes[span.nameIndex()];
        Preconditions.checkArgument(newIndex >= 0);
        newSpans.add(span.withNameIndex(newIndex));
      }
      result.add(StringPoolStyle.create(Collections.unmodifiableList(newSpans)));
    }
    return result;
  }

  /**
   * Delete from this pool strings whose (0-based) indexes are given.  Styles (if any) are deleted
   * alongside their strings.  Return an array whose i'th element is the new index of the string
   * that previously lived at index |i|, or -1 if that string was deleted.
   */
  public int[] deleteStrings(SortedSet<Integer> indexesToDelete) {
    final int previousStringCount = strings.size();
    final int previousStyleCount = styles.size();
    removeIndexesOfStringsWithNameIndexReferencesOutstanding(indexesToDelete);
    int[] result = new int[previousStringCount];
    int resultIndex = -1;  // The index of the last value added to result.
    int offset = 0;  // The offset shift the result by (number of deleted strings so far).
    List<String> newStrings = new ArrayList<>();
    List<StringPoolStyle> newStyles = new ArrayList<>();
    for (int index : indexesToDelete) {
      for (int i = resultIndex + 1; i < index; ++i) {
        result[i] = i - offset;
        newStrings.add(strings.get(i));
        if (i < previousStyleCount) {
          newStyles.add(styles.get(i));
        }
      }
      result[index] = -1;
      ++offset;
      resultIndex = index;
    }
    // Fill in the rest of the offsets
    for (int i = resultIndex + 1; i < previousStringCount; ++i) {
      result[i] = i - offset;
      newStrings.add(strings.get(i));
      if (i < previousStyleCount) {
        newStyles.add(styles.get(i));
      }
    }
    strings = newStrings;
    styles = fixUpStyles(newStyles, result);
    return result;
  }

  /**
   * Returns a style at the given (0-based) index.
   *
   * @param index The (0-based) index of the style to return.
   * @throws IndexOutOfBoundsException If the index is out of range (index < 0 || index >= size()).
   */
  public StringPoolStyle getStyle(int index) {
    return styles.get(index);
  }

  /** Returns the number of styles in this pool. */
  public int getStyleCount() {
    return styles.size();
  }

  /** Returns the type of strings in this pool. */
  public ResourceString.Type getStringType() {
    return isUTF8() ? ResourceString.Type.UTF8 : ResourceString.Type.UTF16;
  }

  @Override
  protected Type getType() {
    return Type.STRING_POOL;
  }

  /** Returns the number of bytes needed for offsets based on {@code strings} and {@code styles}. */
  private int getOffsetSize() {
    return (strings.size() + styles.size()) * 4;
  }

  /**
   * True if this string pool contains strings in UTF-8 format. Otherwise, strings are in UTF-16.
   *
   * @return true if @{code strings} are in UTF-8; false if they're in UTF-16.
   */
  public boolean isUTF8() {
    return (flags & UTF8_FLAG) != 0;
  }

  /**
   * True if this string pool contains already-sorted strings.
   *
   * @return true if @{code strings} are sorted.
   */
  public boolean isSorted() {
    return (flags & SORTED_FLAG) != 0;
  }

  private List<String> readStrings(ByteBuffer buffer, int offset, int count) {
    List<String> result = new ArrayList<>();
    int previousOffset = -1;
    // After the header, we now have an array of offsets for the strings in this pool.
    for (int i = 0; i < count; ++i) {
      int stringOffset = offset + buffer.getInt();
      result.add(ResourceString.decodeString(buffer, stringOffset, getStringType()));
      if (stringOffset <= previousOffset) {
        alwaysDedup = true;
      }
      previousOffset = stringOffset;
    }
    return result;
  }

  private List<StringPoolStyle> readStyles(ByteBuffer buffer, int offset, int count) {
    List<StringPoolStyle> result = new ArrayList<>();
    // After the array of offsets for the strings in the pool, we have an offset for the styles
    // in this pool.
    for (int i = 0; i < count; ++i) {
      int styleOffset = offset + buffer.getInt();
      result.add(StringPoolStyle.create(buffer, styleOffset, this));
    }
    return result;
  }

  protected int writeStrings(DataOutput payload, ByteBuffer offsets, int options)
      throws IOException {
    int stringOffset = 0;
    Map<String, Integer> used = new HashMap<>();  // Keeps track of strings already written
    boolean shouldShrink = ((options & SerializableResource.SHRINK) != 0) || alwaysDedup;
    for (String string : strings) {
      // Dedupe everything except stylized strings, unless shrink is true (then dedupe everything)
      if (shouldShrink && used.containsKey(string)) {
        Integer offset = used.get(string);
        offsets.putInt(offset == null ? 0 : offset);
      } else {
        byte[] encodedString = ResourceString.encodeString(string, getStringType());
        payload.write(encodedString);
        used.put(string, stringOffset);
        offsets.putInt(stringOffset);
        stringOffset += encodedString.length;
      }
    }

    // ARSC files pad to a 4-byte boundary. We should do so too.
    stringOffset = writePad(payload, stringOffset);
    return stringOffset;
  }

  protected int writeStyles(DataOutput payload, ByteBuffer offsets, int options)
      throws IOException {
    int styleOffset = 0;
    boolean shouldShrink = ((options & SerializableResource.SHRINK) != 0) || alwaysDedup;
    if (styles.size() > 0) {
      Map<StringPoolStyle, Integer> used = new HashMap<>();  // Keeps track of bytes already written
      for (StringPoolStyle style : styles) {
        if (shouldShrink && used.containsKey(style)) {
          Integer offset = used.get(style);
          offsets.putInt(offset == null ? 0 : offset);
        } else {
          byte[] encodedStyle = style.toByteArray(options);
          payload.write(encodedStyle);
          used.put(style, styleOffset);
          offsets.putInt(styleOffset);
          styleOffset += encodedStyle.length;
        }
      }
      // The end of the spans are terminated with another sentinel value
      payload.writeInt(StringPoolStyle.RES_STRING_POOL_SPAN_END);
      styleOffset += 4;
      // TODO(acornwall): There appears to be an extra SPAN_END here... why?
      payload.writeInt(StringPoolStyle.RES_STRING_POOL_SPAN_END);
      styleOffset += 4;

      styleOffset = writePad(payload, styleOffset);
    }
    return styleOffset;
  }

  @Override
  protected void writeHeader(ByteBuffer output) {
    output.putInt(strings.size());
    output.putInt(styles.size());
    output.putInt(flags);
    // Aapt and aapt2 specify the stringsStart as header + offset size (b/31435539). It's non-zero
    // even if there are 0 strings in the string pool (unlike the style offset).
    output.putInt(getHeaderSize() + getOffsetSize());
    output.putInt(0);  // Placeholder. The styles starting offset cannot be computed at this point.
  }

  @Override
  protected void writePayload(DataOutput output, ByteBuffer header, int options)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int stringOffset = 0;
    ByteBuffer offsets = ByteBuffer.allocate(getOffsetSize());
    offsets.order(ByteOrder.LITTLE_ENDIAN);

    // Write to a temporary payload so we can rearrange this and put the offsets first
    try (LittleEndianDataOutputStream payload = new LittleEndianDataOutputStream(baos)) {
      stringOffset = writeStrings(payload, offsets, options);
      writeStyles(payload, offsets, options);
    }

    output.write(offsets.array());
    output.write(baos.toByteArray());
    if (!styles.isEmpty()) {
      header.putInt(STYLE_START_OFFSET, getHeaderSize() + getOffsetSize() + stringOffset);
    }
  }

  /** Sets the flag if we should dedup strings even when shrink is set to true */
  public void setAlwaysDedup(boolean alwaysDedup) {
    this.alwaysDedup = alwaysDedup;
  }

  /**
   * Represents all of the styles for a particular string. The string is determined by its index
   * in {@link StringPoolChunk}.
   */
  @AutoValue
  protected abstract static class StringPoolStyle implements SerializableResource {

    // Styles are a list of integers with 0xFFFFFFFF serving as a sentinel value.
    static final int RES_STRING_POOL_SPAN_END = 0xFFFFFFFF;

    public abstract List<StringPoolSpan> spans();

    static StringPoolStyle create(ByteBuffer buffer, int offset, StringPoolChunk parent) {
      List<StringPoolSpan> spans = new ArrayList<>();
      int nameIndex = buffer.getInt(offset);
      while (nameIndex != RES_STRING_POOL_SPAN_END) {
        spans.add(StringPoolSpan.create(buffer, offset, parent));
        offset += StringPoolSpan.SPAN_LENGTH;
        nameIndex = buffer.getInt(offset);
      }
      return create(Collections.unmodifiableList(spans));
    }

    static StringPoolStyle create(ByteBuffer buffer, StringPoolChunk parent) {
      List<StringPoolSpan> spans = new ArrayList<>();
      int nameIndex = buffer.getInt();
      while (nameIndex != RES_STRING_POOL_SPAN_END) {
        spans.add(StringPoolSpan.create(buffer, parent, nameIndex));
        nameIndex = buffer.getInt();
      }
      return create(Collections.unmodifiableList(spans));
    }

    static StringPoolStyle create(List<StringPoolSpan> spans) {
      return new AutoValue_StringPoolChunk_StringPoolStyle(spans);
    }

    @Override
    public byte[] toByteArray() throws IOException {
      return toByteArray(SerializableResource.NONE);
    }

    @Override
    public byte[] toByteArray(int options) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try (LittleEndianDataOutputStream payload = new LittleEndianDataOutputStream(baos)) {
        for (StringPoolSpan span : spans()) {
          byte[] encodedSpan = span.toByteArray(options);
          if (encodedSpan.length != StringPoolSpan.SPAN_LENGTH) {
            throw new IllegalStateException("Encountered a span of invalid length.");
          }
          payload.write(encodedSpan);
        }
        payload.writeInt(RES_STRING_POOL_SPAN_END);
      }

      return baos.toByteArray();
    }

    /**
     * Returns a brief description of the contents of this style. The representation of this
     * information is subject to change, but below is a typical example:
     *
     * <pre>"StringPoolStyle{spans=[StringPoolSpan{foo, start=0, stop=5}, ...]}"</pre>
     */
    @Override
    public String toString() {
      return String.format(Locale.US, "StringPoolStyle{spans=%s}", spans());
    }
  }

  /** Represents a styled span associated with a specific string. */
  @AutoValue
  protected abstract static class StringPoolSpan implements SerializableResource {

    static final int SPAN_LENGTH = 12;

    public abstract int nameIndex();

    final StringPoolSpan withNameIndex(int nameIndex) {
      return new AutoValue_StringPoolChunk_StringPoolSpan(nameIndex, start(), stop(), parent());
    }

    public abstract int start();
    public abstract int stop();
    public abstract StringPoolChunk parent();

    static StringPoolSpan create(ByteBuffer buffer, StringPoolChunk parent, int nameIndex) {
      int start = buffer.getInt();
      int stop = buffer.getInt();
      return new AutoValue_StringPoolChunk_StringPoolSpan(nameIndex, start, stop, parent);
    }

    static StringPoolSpan create(ByteBuffer buffer, int offset, StringPoolChunk parent) {
      int nameIndex = buffer.getInt(offset);
      int start = buffer.getInt(offset + 4);
      int stop = buffer.getInt(offset + 8);
      return new AutoValue_StringPoolChunk_StringPoolSpan(nameIndex, start, stop, parent);
    }

    @Override
    public final byte[] toByteArray() {
      return toByteArray(SerializableResource.NONE);
    }

    @Override
    public final byte[] toByteArray(int options) {
      ByteBuffer buffer = ByteBuffer.allocate(SPAN_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(nameIndex());
      buffer.putInt(start());
      buffer.putInt(stop());
      return buffer.array();
    }

    /**
     * Returns a brief description of this span. The representation of this information is subject
     * to change, but below is a typical example:
     *
     * <pre>"StringPoolSpan{foo, start=0, stop=5}"</pre>
     */
    @Override
    public String toString() {
      return String.format(Locale.US, "StringPoolSpan{%s, start=%d, stop=%d}",
          parent().getString(nameIndex()), start(), stop());
    }
  }
}
