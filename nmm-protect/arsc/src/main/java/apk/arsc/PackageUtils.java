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
import java.nio.charset.Charset;

/** Provides utility methods for package names. */
public final class PackageUtils {

  public static final int PACKAGE_NAME_SIZE = 256;

  private PackageUtils() {}  // Prevent instantiation

  /**
   * Reads the package name from the buffer and repositions the buffer to point directly after
   * the package name.
   * @param buffer The buffer containing the package name.
   * @param offset The offset in the buffer to read from.
   * @return The package name.
   */
  public static String readPackageName(ByteBuffer buffer, int offset) {
    byte[] data = buffer.array();
    int length = 0;
    // Look for the null terminator for the string instead of using the entire buffer.
    // It's UTF-16 so check 2 bytes at a time to see if its double 0.
    for (int i = offset; i < data.length && i < PACKAGE_NAME_SIZE + offset; i += 2) {
      if (data[i] == 0 && data[i + 1] == 0) {
        length = i - offset;
        break;
      }
    }
    Charset utf16 = Charset.forName("UTF-16LE");
    String str = new String(data, offset, length, utf16);
    buffer.position(offset + PACKAGE_NAME_SIZE);
    return str;
  }

  /**
   * Writes the provided package name to the buffer in UTF-16.
   * @param buffer The buffer that will be written to.
   * @param packageName The package name that will be written to the buffer.
   */
  public static void writePackageName(ByteBuffer buffer, String packageName) {
    byte[] nameBytes = packageName.getBytes(Charset.forName("UTF-16LE"));
    buffer.put(nameBytes, 0, Math.min(nameBytes.length, PACKAGE_NAME_SIZE));
    if (nameBytes.length < PACKAGE_NAME_SIZE) {
      // pad out the remaining space with an empty array.
      buffer.put(new byte[PACKAGE_NAME_SIZE - nameBytes.length]);
    }
  }
}
