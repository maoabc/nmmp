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

import java.io.IOException;

/**
 * A resource, typically a @{link Chunk}, that can be converted to an array of bytes.
 */
public interface SerializableResource {

  /** Indicates that no serialization options should be applied in {@link #toByteArray(int)}. */
  public static final int NONE = 0;
  /** Enables some safe size optimizations, such as string pool string deduping. */
  public static final int SHRINK = 1 << 0;
  /** Strips public flags from {@link TypeSpecChunk}s and resource entries. */
  public static final int PRIVATE_RESOURCES = 1 << 1;

  /**
   * Converts this resource into an array of bytes representation.
   * @return An array of bytes representing this resource.
   * @throws IOException
   */
  byte[] toByteArray() throws IOException;

  /**
   * Converts this resource into an array of bytes representation.
   * @param options The serialization options to be applied to the result.
   * @return An array of bytes representing this resource.
   * @throws IOException
   */
  byte[] toByteArray(int options) throws IOException;
}
