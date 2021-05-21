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


import com.google.common.base.Strings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static apk.arsc.ResourceString.Type.UTF16;
import static apk.arsc.ResourceString.Type.UTF8;

@RunWith(JUnit4.class)
public class ResourceStringTest {

    /**
     * A special string chosen because it contains a different encoding length compared to its
     * character length.
     */
    private static final String TEST_STRING = "ābĉ123";

    /**
     * Used to test length where length > 0x7F.
     */
    private static final String LENGTH_BYTE_STRING = Strings.repeat("a", 0xFF);

    /**
     * Used to test length where length > 0x7FFF.
     */
    private static final String LENGTH_WORD_STRING = Strings.repeat("a", 0xFFFF);

    /**
     * Contains the {@code TEST_STRING} in UTF-8 encoding.
     */
    private static final byte[] UTF8_STRING = {0x06, 0x08,  // 6 characters; 8 bytes
            (byte) 0xC4, (byte) 0x81, 0x62, (byte) 0xC4, (byte) 0x89, 0x31, 0x32, 0x33,  // ābĉ123
            0x00};  // Null-terminated

    /**
     * Contains the {@code TEST_STRING} in UTF-16 encoding.
     */
    private static final byte[] UTF16_STRING = {0x06, 0x00,  // Length of string in little-endian
            0x01, 0x01, 0x62, 0x00, 0x09, 0x01, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00,  // ābĉ123
            0x00, 0x00};  // Null-terminated

    /**
     * Contains the two length prefixes for {@code LENGTH_BYTE_STRING} in UTF-8 encoding.
     */
    private static final byte[] UTF8_LENGTH_BYTE =
            {(byte) 0x80, (byte) 0xFF, (byte) 0x80, (byte) 0xFF};

    /**
     * Contains the length prefix for {@code LENGTH_BYTE_STRING} in UTF-16 encoding.
     */
    private static final byte[] UTF16_LENGTH_BYTE = {(byte) 0xFF, 0x00};

    /**
     * Contains the length prefix for {@code LENGTH_BYTE_WORD} in UTF-16 encoding.
     */
    private static final byte[] UTF16_LENGTH_WORD = {0x00, (byte) 0x80, (byte) 0xFF, (byte) 0xFF};

    @Test
    public void decodeUtf8String() {
        ByteBuffer utf8 = ByteBuffer.wrap(UTF8_STRING).order(ByteOrder.LITTLE_ENDIAN);
        assert ResourceString.decodeString(utf8, 0, UTF8).equals(TEST_STRING);
    }

    @Test
    public void decodeUtf16String() {
        ByteBuffer utf16 = ByteBuffer.wrap(UTF16_STRING).order(ByteOrder.LITTLE_ENDIAN);
        assert ResourceString.decodeString(utf16, 0, UTF16).equals(TEST_STRING);
    }

    @Test
    public void encodeUtf8String() {
        byte[] utf8 = ResourceString.encodeString(TEST_STRING, UTF8);
        assert Arrays.equals(utf8, UTF8_STRING);
    }

    @Test
    public void encodeUtf16String() {
        byte[] utf16 = ResourceString.encodeString(TEST_STRING, UTF16);
        assert Arrays.equals((utf16), UTF16_STRING);
    }

    @Test
    public void parseLargeUtf8String() {
        parseString(LENGTH_BYTE_STRING, UTF8, UTF8_LENGTH_BYTE);
    }

    @Test
    public void parseMediumUtf16String() {
        parseString(LENGTH_BYTE_STRING, UTF16, UTF16_LENGTH_BYTE);
    }

    @Test
    public void parseLargeUtf16String() {
        parseString(LENGTH_WORD_STRING, UTF16, UTF16_LENGTH_WORD);
    }

    private void parseString(String str, ResourceString.Type type, byte[] prefix) {
        byte[] utf = ResourceString.encodeString(str, type);
        byte[] utfPrefix = Arrays.copyOf(utf, prefix.length);
        assert Arrays.equals((utfPrefix), prefix);
        ByteBuffer buffer = ByteBuffer.wrap(utf).order(ByteOrder.LITTLE_ENDIAN);
        assert ResourceString.decodeString(buffer, 0, type).equals(str);
    }
}
