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

import static apk.base.Preconditions.checkState;

/**
 * Represents a resource id in a {@link ResourceTableChunk} of the form 0xpptteeee, where pp is the
 * {@link PackageChunk} id, tt is the {@link TypeChunk} id, and eeee is the index of the entry in
 * the {@link TypeChunk}.
 */
@AutoValue
public abstract class ResourceIdentifier {

  /** The {@link PackageChunk} id mask for a packed resource id of the form 0xpptteeee. */
  private static final int PACKAGE_ID_MASK = 0xFF000000;
  private static final int PACKAGE_ID_SHIFT = 24;

  /** The {@link TypeChunk} id mask for a packed resource id of the form 0xpptteeee. */
  private static final int TYPE_ID_MASK = 0x00FF0000;
  private static final int TYPE_ID_SHIFT = 16;

  /** The {@link TypeChunk.Entry} id mask for a packed resource id of the form 0xpptteeee. */
  private static final int ENTRY_ID_MASK = 0xFFFF;
  private static final int ENTRY_ID_SHIFT = 0;

  /** The (1-based) id of the {@link PackageChunk} containing this resource. */
  public abstract int packageId();

  /** The (1-based) id of the {@link TypeChunk} containing this resource. */
  public abstract int typeId();

  /** The (0-based) index of the entry in a {@link TypeChunk} containing this resource. */
  public abstract int entryId();

  /** Returns a {@link ResourceIdentifier} from a {@code resourceId} of the form 0xpptteeee. */
  public static ResourceIdentifier create(int resourceId) {
    int packageId = (resourceId & PACKAGE_ID_MASK) >>> PACKAGE_ID_SHIFT;
    int typeId = (resourceId & TYPE_ID_MASK) >>> TYPE_ID_SHIFT;
    int entryId = (resourceId & ENTRY_ID_MASK) >>> ENTRY_ID_SHIFT;
    return create(packageId, typeId, entryId);
  }

  /** Returns a {@link ResourceIdentifier} with the given identifiers. */
  public static ResourceIdentifier create(int packageId, int typeId, int entryId) {
    checkState((packageId & 0xFF) == packageId, "packageId must be <= 0xFF.");
    checkState((typeId & 0xFF) == typeId, "typeId must be <= 0xFF.");
    checkState((entryId & 0xFFFF) == entryId, "entryId must be <= 0xFFFF.");
    return new AutoValue_ResourceIdentifier(packageId, typeId, entryId);
  }

  /** Returns the resource id from the integer representation. */
  public static int entryIdFromResourceId(int resourceId) {
    return resourceId & ENTRY_ID_MASK;
  }

  /** Returns the resource id as an integer with an alternative entryId. */
  public static int asInt(int packageId, int typeId, int entryId) {
    checkState((packageId & 0xFF) == packageId, "packageId must be <= 0xFF.");
    checkState((typeId & 0xFF) == typeId, "typeId must be <= 0xFF.");
    checkState((entryId & 0xFFFF) == entryId, "entryId must be <= 0xFFFF.");
    return packageId << PACKAGE_ID_SHIFT | typeId << TYPE_ID_SHIFT | entryId;
  }

}
