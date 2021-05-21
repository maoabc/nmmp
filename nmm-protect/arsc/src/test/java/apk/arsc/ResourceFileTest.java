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
import org.junit.runners.JUnit4;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

@RunWith(JUnit4.class)
/** Tests {@link ResourceFile}. */
public final class ResourceFileTest {

    private static final String APK_FILE_PATH =
            "/test.apk";
    private static final File apkFile = new File(APK_FILE_PATH);

    private static final String APK_FILE_PATH2 =
            "/test2.apk";

    /**
     * Tests that resource files, when reassembled, are identical.
     */
    @Test
    public void testToByteArray() throws Exception {
        // Get all .arsc and encoded .xml files
        String regex = "AndroidManifest\\.xml";
        Pattern pattern = Pattern.compile(regex);
        InputStream input = getClass().getResourceAsStream(APK_FILE_PATH);
        Map<String, byte[]> resourceFiles = ApkUtils.getFiles(input, pattern);
        for (Entry<String, byte[]> entry : resourceFiles.entrySet()) {
            String name = entry.getKey();
            byte[] fileBytes = entry.getValue();
            if (!name.startsWith("res/raw/")) {  // xml files in res/raw/ are not compact XML
                ResourceFile file = new ResourceFile(fileBytes);
                assert Arrays.equals(file.toByteArray(), fileBytes);
            }
        }
    }

    @Test
    public void testArsc() throws Exception {
        // Get all .arsc and encoded .xml files
        String regex = "(.*?\\.arsc)|(AndroidManifest\\.xml)|(res/.*?\\.xml)";
        Pattern pattern = Pattern.compile("(AndroidManifest\\.xml)|(.*?\\.arsc)");
        InputStream input = getClass().getResourceAsStream("/test.apk");
        Map<String, byte[]> resourceFiles = ApkUtils.getFiles(input, pattern);
        HashMap<String, ResourceFile> map = new HashMap<>();
        ResourceTableChunk arsc = null;
        for (Entry<String, byte[]> entry : resourceFiles.entrySet()) {
            String name = entry.getKey();
            ResourceFile file = new ResourceFile(entry.getValue());
            map.put(name, file);

            for (Chunk chunk : file.getChunks()) {
                if (chunk.getType() == Chunk.Type.TABLE) {
                    arsc = (ResourceTableChunk) chunk;
                }
            }
        }
        for (Entry<String, ResourceFile> entry : map.entrySet()) {
            ResourceFile file = entry.getValue();
            List<Chunk> chunks = file.getChunks();
            for (Chunk chunk : chunks) {
                if (chunk instanceof XmlChunk) {
//                    XmlChunk xmlChunk = (XmlChunk) chunk;
//                    MyPackageInfo myPackageInfo =
//                            InfoUtils.parsePackageInfo(arsc, xmlChunk, 480, "zh", "", 2);
//                    System.out.println(myPackageInfo);

                } else if (chunk instanceof ResourceTableChunk) {
                    for (PackageChunk packageChunk : ((ResourceTableChunk) chunk).getPackages()) {
                        System.out.println(packageChunk.getId());
                        for (TypeChunk typeChunk : packageChunk.getTypeChunks()) {
//                            if (typeChunk.getId() == 0x8) {
                            System.out.println(typeChunk.getId());
                            System.out.println(typeChunk.getTypeName());
                            System.out.println(typeChunk.getConfiguration());
//                                for (Entry<Integer, TypeChunk.Entry> entryEntry : typeChunk.getEntries().entrySet()) {
//                                    ResourceValue value = entryEntry.getValue().value();
//                                    if (value != null && value.type() == ResourceValue.Type.STRING) {
//                                        String string = typeChunk.getString(value.data());
//                                        if (string.endsWith("icon.png")) {
//                                            System.out.println(entryEntry);
//                                            System.out.println(string);
//                                        }
//                                    }
//                                }

//                                    if (entry1.value().type() == ResourceValue.Type.STRING) {
//                                        int index = entry1.value().data();
//                                        System.out.println(typeChunk.getString(index));
//                                    }
//                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * Tests that resource files, when reassembled, are identical.
     */
    @Test
    public void testModifyXmlAttr() throws Exception {
        byte[] fileBytes = ApkUtils.getFile(getClass().getResourceAsStream(APK_FILE_PATH), "AndroidManifest.xml");
        ResourceFile file = new ResourceFile(fileBytes);
        for (Chunk chunk : file.getChunks()) {
            if (chunk instanceof XmlChunk) {
                handleXmlChunk((XmlChunk) chunk);
            }
        }
        byte[] bytes = file.toByteArray();
        FileOutputStream outputStream = new FileOutputStream("AndroidManifest.xml");
        outputStream.write(bytes);
        outputStream.close();
    }

    private void handleXmlChunk(XmlChunk xmlChunk) {
        StringPoolChunk stringPoolChunk = getStringPoolChunk(xmlChunk);
        if (stringPoolChunk == null) {
            return;
        }
        for (Chunk chunk : xmlChunk.getChunks().values()) {
            if (chunk instanceof XmlStartElementChunk) {
                XmlStartElementChunk startElementChunk = (XmlStartElementChunk) chunk;
                modifyApplicationName(stringPoolChunk, startElementChunk, "mao.myapp.app");
            }
        }
    }

    private String modifyApplicationName(@Nonnull StringPoolChunk stringPoolChunk, XmlStartElementChunk startElement, String newAppName) {
        String oldAppName = "";
        if (startElement.getName().equals("application")) {
            List<XmlAttribute> attributes = startElement.getAttributes();
            for (int i = 0; i < attributes.size(); i++) {
                XmlAttribute attribute = attributes.get(i);
                if (attribute.name().equals("name") &&
                        attribute.typedValue().type() == ResourceValue.Type.STRING) {
                    int strIdx = stringPoolChunk.addString(newAppName);
                    XmlAttribute newAttr = XmlAttribute.create(
                            attribute.namespaceIndex(),
                            attribute.nameIndex(),
                            strIdx,
                            attribute.typedValue().withData(strIdx),
                            attribute.parent()
                    );

                    System.out.println("attr " + attribute.typedValue() + "   " + attribute.rawValue()
                            + "  " + attribute.typedValue().data());
                    System.out.println(attribute.typedValue().type());
                    System.out.println("new Attr " + newAttr + "  " + newAttr.rawValue());

                    startElement.setAttribute(i, newAttr);
                    oldAppName = attribute.rawValue();
                    break;
                }
            }
        }
        return oldAppName;

    }

    private static StringPoolChunk getStringPoolChunk(XmlChunk xmlChunk) {
        for (Chunk chunk : xmlChunk.getChunks().values()) {
            if (chunk instanceof StringPoolChunk) {
                return (StringPoolChunk) chunk;
            }
        }
        return null;
    }
}
