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


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class ResourceIdentifierTest {

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0x01234567, 0x01, 0x23, 0x4567},
                {0xFEDCBA98, 0xFE, 0xDC, 0xBA98}
        });
    }

    private final int packageId;
    private final int typeId;
    private final int entryId;
    private ResourceIdentifier resourceIdentifier;

    public ResourceIdentifierTest(int resourceId, int packageId, int typeId, int entryId) {
        resourceIdentifier = ResourceIdentifier.create(resourceId);
        this.packageId = packageId;
        this.typeId = typeId;
        this.entryId = entryId;
    }

    @Test
    public void resourceIdentifier_comparePackage() {
        assert resourceIdentifier.packageId() == packageId;
    }

    @Test
    public void resourceIdentifier_compareType() {
        assert (resourceIdentifier.typeId()) == typeId;
    }

    @Test
    public void resourceIdentifier_compareEntry() {
        assert resourceIdentifier.entryId() == entryId;
    }
}

