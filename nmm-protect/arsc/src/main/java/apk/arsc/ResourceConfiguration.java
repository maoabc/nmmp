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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import apk.base.Bytes;
import apk.base.Preconditions;

import static apk.base.Charsets.US_ASCII;

/**
 * Describes a particular resource configuration.
 */
@AutoValue
public abstract class ResourceConfiguration implements SerializableResource {

    /**
     * The different types of configs that can be present in a {@link ResourceConfiguration}.
     *
     * <p>The ordering of these types is roughly the same as {@code #isBetterThan}, but is not
     * guaranteed to be the same.
     */
    public enum Type {
        MCC,
        MNC,
        LANGUAGE_STRING,
        LOCALE_SCRIPT_STRING,
        REGION_STRING,
        LOCALE_VARIANT_STRING,
        SCREEN_LAYOUT_DIRECTION,
        SMALLEST_SCREEN_WIDTH_DP,
        SCREEN_WIDTH_DP,
        SCREEN_HEIGHT_DP,
        SCREEN_LAYOUT_SIZE,
        SCREEN_LAYOUT_LONG,
        SCREEN_LAYOUT_ROUND,
        COLOR_MODE_WIDE_COLOR_GAMUT, // NB: COLOR_GAMUT takes priority over HDR in #isBetterThan.
        COLOR_MODE_HDR,
        ORIENTATION,
        UI_MODE_TYPE,
        UI_MODE_NIGHT,
        DENSITY_DPI,
        TOUCHSCREEN,
        KEYBOARD_HIDDEN,
        KEYBOARD,
        NAVIGATION_HIDDEN,
        NAVIGATION,
        SCREEN_SIZE,
        SDK_VERSION
    }

    private static final Builder DEFAULT_BUILDER = builder();

    /**
     * The default configuration. This configuration acts as a "catch-all" for looking up resources
     * when no better configuration can be found.
     */
    public static final ResourceConfiguration DEFAULT = DEFAULT_BUILDER.build();

    /**
     * The below constants are from android.content.res.Configuration.
     */
    static final int COLOR_MODE_WIDE_COLOR_GAMUT_MASK = 0x03;

    static final int COLOR_MODE_WIDE_COLOR_GAMUT_UNDEFINED = 0;
    static final int COLOR_MODE_WIDE_COLOR_GAMUT_NO = 0x01;
    static final int COLOR_MODE_WIDE_COLOR_GAMUT_YES = 0x02;

    private static final Map<Integer, String> COLOR_MODE_WIDE_COLOR_GAMUT_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(COLOR_MODE_WIDE_COLOR_GAMUT_UNDEFINED, "");
        map.put(COLOR_MODE_WIDE_COLOR_GAMUT_NO, "nowidecg");
        map.put(COLOR_MODE_WIDE_COLOR_GAMUT_YES, "widecg");
        COLOR_MODE_WIDE_COLOR_GAMUT_VALUES = Collections.unmodifiableMap(map);
    }

    static final int COLOR_MODE_HDR_MASK = 0x0C;
    static final int COLOR_MODE_HDR_UNDEFINED = 0;
    static final int COLOR_MODE_HDR_NO = 0x04;
    static final int COLOR_MODE_HDR_YES = 0x08;

    private static final Map<Integer, String> COLOR_MODE_HDR_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(COLOR_MODE_HDR_UNDEFINED, "");
        map.put(COLOR_MODE_HDR_NO, "lowdr");
        map.put(COLOR_MODE_HDR_YES, "highdr");
        COLOR_MODE_HDR_VALUES = Collections.unmodifiableMap(map);
    }

    static final int DENSITY_DPI_UNDEFINED = 0;
    static final int DENSITY_DPI_LDPI = 120;
    static final int DENSITY_DPI_MDPI = 160;
    static final int DENSITY_DPI_TVDPI = 213;
    static final int DENSITY_DPI_HDPI = 240;
    static final int DENSITY_DPI_XHDPI = 320;
    static final int DENSITY_DPI_XXHDPI = 480;
    static final int DENSITY_DPI_XXXHDPI = 640;
    static final int DENSITY_DPI_ANY = 0xFFFE;
    static final int DENSITY_DPI_NONE = 0xFFFF;

    private static final Map<Integer, String> DENSITY_DPI_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(DENSITY_DPI_UNDEFINED, "");
        map.put(DENSITY_DPI_LDPI, "ldpi");
        map.put(DENSITY_DPI_MDPI, "mdpi");
        map.put(DENSITY_DPI_TVDPI, "tvdpi");
        map.put(DENSITY_DPI_HDPI, "hdpi");
        map.put(DENSITY_DPI_XHDPI, "xhdpi");
        map.put(DENSITY_DPI_XXHDPI, "xxhdpi");
        map.put(DENSITY_DPI_XXXHDPI, "xxxhdpi");
        map.put(DENSITY_DPI_ANY, "anydpi");
        map.put(DENSITY_DPI_NONE, "nodpi");
        DENSITY_DPI_VALUES = Collections.unmodifiableMap(map);
    }

    static final int KEYBOARD_NOKEYS = 1;
    static final int KEYBOARD_QWERTY = 2;
    static final int KEYBOARD_12KEY = 3;

    private static final Map<Integer, String> KEYBOARD_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(KEYBOARD_NOKEYS, "nokeys");
        map.put(KEYBOARD_QWERTY, "qwerty");
        map.put(KEYBOARD_12KEY, "12key");
        KEYBOARD_VALUES = Collections.unmodifiableMap(map);
    }

    static final int KEYBOARDHIDDEN_MASK = 0x03;
    static final int KEYBOARDHIDDEN_NO = 1;
    static final int KEYBOARDHIDDEN_YES = 2;
    static final int KEYBOARDHIDDEN_SOFT = 3;

    private static final Map<Integer, String> KEYBOARDHIDDEN_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(KEYBOARDHIDDEN_NO, "keysexposed");
        map.put(KEYBOARDHIDDEN_YES, "keyshidden");
        map.put(KEYBOARDHIDDEN_SOFT, "keyssoft");
        KEYBOARDHIDDEN_VALUES = Collections.unmodifiableMap(map);
    }

    static final int NAVIGATION_NONAV = 1;
    static final int NAVIGATION_DPAD = 2;
    static final int NAVIGATION_TRACKBALL = 3;
    static final int NAVIGATION_WHEEL = 4;

    private static final Map<Integer, String> NAVIGATION_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(NAVIGATION_NONAV, "nonav");
        map.put(NAVIGATION_DPAD, "dpad");
        map.put(NAVIGATION_TRACKBALL, "trackball");
        map.put(NAVIGATION_WHEEL, "wheel");
        NAVIGATION_VALUES = Collections.unmodifiableMap(map);
    }

    static final int NAVIGATIONHIDDEN_MASK = 0x0C;
    static final int NAVIGATIONHIDDEN_NO = 0x04;
    static final int NAVIGATIONHIDDEN_YES = 0x08;

    private static final Map<Integer, String> NAVIGATIONHIDDEN_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(NAVIGATIONHIDDEN_NO, "navexposed");
        map.put(NAVIGATIONHIDDEN_YES, "navhidden");
        NAVIGATIONHIDDEN_VALUES = Collections.unmodifiableMap(map);
    }

    static final int ORIENTATION_PORTRAIT = 0x01;
    static final int ORIENTATION_LANDSCAPE = 0x02;

    private static final Map<Integer, String> ORIENTATION_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(ORIENTATION_PORTRAIT, "port");
        map.put(ORIENTATION_LANDSCAPE, "land");
        ORIENTATION_VALUES = Collections.unmodifiableMap(map);
    }

    static final int SCREENLAYOUT_LAYOUTDIR_MASK = 0xC0;
    static final int SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
    static final int SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;

    private static final Map<Integer, String> SCREENLAYOUT_LAYOUTDIR_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(SCREENLAYOUT_LAYOUTDIR_LTR, "ldltr");
        map.put(SCREENLAYOUT_LAYOUTDIR_RTL, "ldrtl");
        SCREENLAYOUT_LAYOUTDIR_VALUES = Collections.unmodifiableMap(map);
    }

    static final int SCREENLAYOUT_LONG_MASK = 0x30;
    static final int SCREENLAYOUT_LONG_NO = 0x10;
    static final int SCREENLAYOUT_LONG_YES = 0x20;

    private static final Map<Integer, String> SCREENLAYOUT_LONG_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(SCREENLAYOUT_LONG_NO, "notlong");
        map.put(SCREENLAYOUT_LONG_YES, "long");
        SCREENLAYOUT_LONG_VALUES = Collections.unmodifiableMap(map);
    }

    static final int SCREENLAYOUT_ROUND_MASK = 0x03;
    static final int SCREENLAYOUT_ROUND_NO = 0x01;
    static final int SCREENLAYOUT_ROUND_YES = 0x02;

    private static final Map<Integer, String> SCREENLAYOUT_ROUND_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(SCREENLAYOUT_ROUND_NO, "notround");
        map.put(SCREENLAYOUT_ROUND_YES, "round");
        SCREENLAYOUT_ROUND_VALUES = Collections.unmodifiableMap(map);
    }

    static final int SCREENLAYOUT_SIZE_MASK = 0x0F;
    static final int SCREENLAYOUT_SIZE_SMALL = 0x01;
    static final int SCREENLAYOUT_SIZE_NORMAL = 0x02;
    static final int SCREENLAYOUT_SIZE_LARGE = 0x03;
    static final int SCREENLAYOUT_SIZE_XLARGE = 0x04;

    private static final Map<Integer, String> SCREENLAYOUT_SIZE_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(SCREENLAYOUT_SIZE_SMALL, "small");
        map.put(SCREENLAYOUT_SIZE_NORMAL, "normal");
        map.put(SCREENLAYOUT_SIZE_LARGE, "large");
        map.put(SCREENLAYOUT_SIZE_XLARGE, "xlarge");
        SCREENLAYOUT_SIZE_VALUES = Collections.unmodifiableMap(map);
    }

    static final int TOUCHSCREEN_NOTOUCH = 1;
    static final int TOUCHSCREEN_FINGER = 3;

    private static final Map<Integer, String> TOUCHSCREEN_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(TOUCHSCREEN_NOTOUCH, "notouch");
        map.put(TOUCHSCREEN_FINGER, "finger");
        TOUCHSCREEN_VALUES = Collections.unmodifiableMap(map);
    }

    static final int UI_MODE_NIGHT_MASK = 0x30;
    static final int UI_MODE_NIGHT_NO = 0x10;
    static final int UI_MODE_NIGHT_YES = 0x20;

    private static final Map<Integer, String> UI_MODE_NIGHT_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(UI_MODE_NIGHT_NO, "notnight");
        map.put(UI_MODE_NIGHT_YES, "night");
        UI_MODE_NIGHT_VALUES = Collections.unmodifiableMap(map);
    }

    static final int UI_MODE_TYPE_MASK = 0x0F;
    static final int UI_MODE_TYPE_DESK = 0x02;
    static final int UI_MODE_TYPE_CAR = 0x03;
    static final int UI_MODE_TYPE_TELEVISION = 0x04;
    static final int UI_MODE_TYPE_APPLIANCE = 0x05;
    static final int UI_MODE_TYPE_WATCH = 0x06;
    static final int UI_MODE_TYPE_VR_HEADSET = 0x07;

    private static final Map<Integer, String> UI_MODE_TYPE_VALUES;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(UI_MODE_TYPE_DESK, "desk");
        map.put(UI_MODE_TYPE_CAR, "car");
        map.put(UI_MODE_TYPE_TELEVISION, "television");
        map.put(UI_MODE_TYPE_APPLIANCE, "appliance");
        map.put(UI_MODE_TYPE_WATCH, "watch");
        map.put(UI_MODE_TYPE_VR_HEADSET, "vrheadset");
        UI_MODE_TYPE_VALUES = Collections.unmodifiableMap(map);
    }

    /**
     * The minimum size in bytes that a {@link ResourceConfiguration} can be.
     */
    private static final int MIN_SIZE = 28;

    /**
     * The minimum size in bytes that this configuration must be to contain screen config info.
     */
    private static final int SCREEN_CONFIG_MIN_SIZE = 32;

    /**
     * The minimum size in bytes that this configuration must be to contain screen dp info.
     */
    private static final int SCREEN_DP_MIN_SIZE = 36;

    /**
     * The minimum size in bytes that this configuration must be to contain locale info.
     */
    private static final int LOCALE_MIN_SIZE = 48;

    /**
     * The minimum size in bytes that this config must be to contain the screenConfig extension.
     */
    private static final int SCREEN_CONFIG_EXTENSION_MIN_SIZE = 52;

    /**
     * The size of resource configurations in bytes for the latest version of Android resources.
     */
    public static final int SIZE = SCREEN_CONFIG_EXTENSION_MIN_SIZE;

    /**
     * The number of bytes that this resource configuration takes up.
     */
    public abstract int size();

    public abstract int mcc();

    public abstract int mnc();

    /**
     * Returns a packed 2-byte language code.
     */
    @SuppressWarnings("mutable")
    public abstract byte[] language();

    /**
     * Returns {@link #language} as an unpacked string representation.
     */
    public final String languageString() {
        return unpackLanguage();
    }

    /**
     * Returns a packed 2-byte region code.
     */
    @SuppressWarnings("mutable")
    public abstract byte[] region();

    /**
     * Returns {@link #region} as an unpacked string representation.
     */
    public final String regionString() {
        return unpackRegion();
    }

    public abstract int orientation();

    public abstract int touchscreen();

    public abstract int density();

    public abstract int keyboard();

    public abstract int navigation();

    public abstract int inputFlags();

    public final int keyboardHidden() {
        return inputFlags() & KEYBOARDHIDDEN_MASK;
    }

    public final int navigationHidden() {
        return inputFlags() & NAVIGATIONHIDDEN_MASK;
    }

    public abstract int screenWidth();

    public abstract int screenHeight();

    public abstract int sdkVersion();

    /**
     * Returns a copy of this resource configuration with a different {@link #sdkVersion}, or this
     * configuration if the {@code sdkVersion} is the same.
     *
     * @param sdkVersion The SDK version of the returned configuration.
     * @return A copy of this configuration with the only difference being #sdkVersion.
     */
    public final ResourceConfiguration withSdkVersion(int sdkVersion) {
        return toBuilder().sdkVersion(sdkVersion).build();
    }

    public abstract int minorVersion();

    public abstract int screenLayout();

    public final int screenLayoutDirection() {
        return screenLayout() & SCREENLAYOUT_LAYOUTDIR_MASK;
    }

    public final int screenLayoutSize() {
        return screenLayout() & SCREENLAYOUT_SIZE_MASK;
    }

    public final int screenLayoutLong() {
        return screenLayout() & SCREENLAYOUT_LONG_MASK;
    }

    public final int screenLayoutRound() {
        return screenLayout2() & SCREENLAYOUT_ROUND_MASK;
    }

    public abstract int uiMode();

    public final int uiModeType() {
        return uiMode() & UI_MODE_TYPE_MASK;
    }

    public final int uiModeNight() {
        return uiMode() & UI_MODE_NIGHT_MASK;
    }

    public abstract int smallestScreenWidthDp();

    public abstract int screenWidthDp();

    public abstract int screenHeightDp();

    /**
     * The ISO-15924 short name for the script corresponding to this configuration.
     */
    @SuppressWarnings("mutable")
    public abstract byte[] localeScript();

    /**
     * Returns the {@link #localeScript} as a string.
     */
    public final String localeScriptString() {
        return byteArrayToString(localeScript());
    }

    /**
     * A single BCP-47 variant subtag.
     */
    @SuppressWarnings("mutable")
    public abstract byte[] localeVariant();

    /**
     * Returns the {@link #localeVariant} as a string.
     */
    public final String localeVariantString() {
        return byteArrayToString(localeVariant());
    }

    /**
     * An extension to {@link #screenLayout}. Contains round/notround qualifier.
     */
    public abstract int screenLayout2();

    /**
     * Wide-gamut, HDR, etc.
     */
    public abstract int colorMode();

    /**
     * Returns the wide color gamut section of {@link #colorMode}.
     */
    public final int colorModeWideColorGamut() {
        return colorMode() & COLOR_MODE_WIDE_COLOR_GAMUT_MASK;
    }

    /**
     * Returns the HDR section of {@link #colorMode}.
     */
    public final int colorModeHdr() {
        return colorMode() & COLOR_MODE_HDR_MASK;
    }

    /**
     * Any remaining bytes in this resource configuration that are unaccounted for.
     */
    @SuppressWarnings("mutable")
    public abstract byte[] unknown();

    /**
     * Returns this {@link ResourceConfiguration} as a builder.
     */
    public abstract Builder toBuilder();

    /**
     * Returns a {@link Builder} with sane default properties.
     */
    public static Builder builder() {
        return new AutoValue_ResourceConfiguration.Builder()
                .size(SIZE)
                .mcc(0)
                .mnc(0)
                .language(new byte[2])
                .region(new byte[2])
                .orientation(0)
                .touchscreen(0)
                .density(0)
                .keyboard(0)
                .navigation(0)
                .inputFlags(0)
                .screenWidth(0)
                .screenHeight(0)
                .sdkVersion(0)
                .minorVersion(0)
                .screenLayout(0)
                .uiMode(0)
                .smallestScreenWidthDp(0)
                .screenWidthDp(0)
                .screenHeightDp(0)
                .localeScript(new byte[4])
                .localeVariant(new byte[8])
                .screenLayout2(0)
                .colorMode(0)
                .unknown(new byte[0]);
    }

    static ResourceConfiguration create(ByteBuffer buffer) {
        int startPosition = buffer.position();  // The starting buffer position to calculate bytes read.
        int size = buffer.getInt();
        Preconditions.checkArgument(size >= MIN_SIZE,
                "Expected minimum ResourceConfiguration size of %s, got %s", MIN_SIZE, size);
        // Builder order is important here. It's the same order as the data stored in the buffer.
        // The order of the builder's method calls, such as #mcc and #mnc, should not be changed.
        Builder configurationBuilder = builder()
                .size(size)
                .mcc(buffer.getShort() & 0xFFFF)
                .mnc(buffer.getShort() & 0xFFFF);
        byte[] language = new byte[2];
        buffer.get(language);
        byte[] region = new byte[2];
        buffer.get(region);
        configurationBuilder.language(language)
                .region(region)
                .orientation(buffer.get() & 0xFF)
                .touchscreen(buffer.get() & 0xFF)
                .density(buffer.getShort() & 0xFFFF)
                .keyboard(buffer.get() & 0xFF)
                .navigation(buffer.get() & 0xFF)
                .inputFlags(buffer.get() & 0xFF);
        buffer.get();  // 1 byte of padding
        configurationBuilder.screenWidth(buffer.getShort() & 0xFFFF)
                .screenHeight(buffer.getShort() & 0xFFFF)
                .sdkVersion(buffer.getShort() & 0xFFFF)
                .minorVersion(buffer.getShort() & 0xFFFF);

        // At this point, the configuration's size needs to be taken into account as not all
        // configurations have all values.
        if (size >= SCREEN_CONFIG_MIN_SIZE) {
            configurationBuilder.screenLayout(buffer.get() & 0xFF)
                    .uiMode(buffer.get() & 0xFF)
                    .smallestScreenWidthDp(buffer.getShort() & 0xFFFF);
        }

        if (size >= SCREEN_DP_MIN_SIZE) {
            configurationBuilder.screenWidthDp(buffer.getShort() & 0xFFFF)
                    .screenHeightDp(buffer.getShort() & 0xFFFF);
        }

        if (size >= LOCALE_MIN_SIZE) {
            byte[] localeScript = new byte[4];
            buffer.get(localeScript);
            byte[] localeVariant = new byte[8];
            buffer.get(localeVariant);
            configurationBuilder.localeScript(localeScript)
                    .localeVariant(localeVariant);
        }

        if (size >= SCREEN_CONFIG_EXTENSION_MIN_SIZE) {
            configurationBuilder.screenLayout2(buffer.get() & 0xFF);
            configurationBuilder.colorMode(buffer.get() & 0xFF);
            buffer.getShort();  // More reserved padding
        }

        // After parsing everything that's known, account for anything that's unknown.
        int bytesRead = buffer.position() - startPosition;
        byte[] unknown = new byte[size - bytesRead];
        buffer.get(unknown);
        configurationBuilder.unknown(unknown);

        return configurationBuilder.build();
    }

    private String unpackLanguage() {
        return unpackLanguage(language());
    }

    public static String unpackLanguage(byte[] language) {
        return unpackLanguageOrRegion(language, 0x61);
    }

    private String unpackRegion() {
        return unpackLanguageOrRegion(region(), 0x30);
    }

    private static String unpackLanguageOrRegion(byte[] value, int base) {
        Preconditions.checkState(value.length == 2, "Language or region value must be 2 bytes.");
        if (value[0] == 0 && value[1] == 0) {
            return "";
        }
        if ((value[0] & 0x80) != 0) {
            byte[] result = new byte[3];
            result[0] = (byte) (base + (value[1] & 0x1F));
            result[1] = (byte) (base + ((value[1] & 0xE0) >>> 5) + ((value[0] & 0x03) << 3));
            result[2] = (byte) (base + ((value[0] & 0x7C) >>> 2));
            return new String(result, US_ASCII);
        }
        return new String(value, US_ASCII);
    }

    /**
     * Packs a 2 or 3 character language string into two bytes. If this is a 2 character string the
     * returned bytes is simply the string bytes, if this is a 3 character string we use a packed
     * format where the two bytes are:
     *
     * <pre>
     *  +--+--+--+--+--+--+--+--+  +--+--+--+--+--+--+--+--+
     *  |B |2 |2 |2 |2 |2 |1 |1 |  |1 |1 |1 |0 |0 |0 |0 |0 |
     *  +--+--+--+--+--+--+--+--+  +--+--+--+--+--+--+--+--+
     * </pre>
     *
     * <p>B : if bit set indicates this is a 3 character string (languages are always old style 7 bit
     * ascii chars only, so this is never set for a two character language)
     *
     * <p>2: The third character - 0x61
     *
     * <p>1: The second character - 0x61
     *
     * <p>0: The first character - 0x61
     *
     * <p>Languages are always lower case chars, so max is within 5 bits (z = 11001)
     *
     * @param language The language to pack.
     * @return The two byte representation of the language
     */
    public static byte[] packLanguage(String language) {
        byte[] unpacked = language.getBytes(US_ASCII);
        if (unpacked.length == 2) {
            return unpacked;
        }
        int base = 0x61;
        byte[] result = new byte[2];
        Preconditions.checkState(unpacked.length == 3);
        for (byte value : unpacked) {
            Preconditions.checkState(value >= 'a' && value <= 'z');
        }
        result[0] = (byte) (((unpacked[2] - base) << 2) | ((unpacked[1] - base) >> 3) | 0x80);
        result[1] = (byte) ((unpacked[0] - base) | ((unpacked[1] - base) << 5));
        return result;
    }

    private String byteArrayToString(byte[] data) {
        int length = Bytes.indexOf(data, (byte) 0);
        return new String(data, 0, length >= 0 ? length : data.length, US_ASCII);
    }

    /**
     * Returns true if this is the default "any" configuration.
     */
    public final boolean isDefault() {
        // Ignore size and unknown when checking if this is the default configuration. It's possible
        // that we're comparing against a different version.
        return DEFAULT_BUILDER.size(size()).unknown(unknown()).build().equals(this)
                && Arrays.equals(unknown(), new byte[unknown().length]);
    }

    public final boolean isDensityCompatibleWith(int deviceDensityDpi) {
        int configDensity = density();
        switch (configDensity) {
            case DENSITY_DPI_UNDEFINED:
            case DENSITY_DPI_ANY:
            case DENSITY_DPI_NONE:
                return true;
            default:
                return configDensity <= deviceDensityDpi;
        }
    }

    @Override
    public final byte[] toByteArray() {
        return toByteArray(SerializableResource.NONE);
    }

    @Override
    public final byte[] toByteArray(int options) {
        ByteBuffer buffer = ByteBuffer.allocate(size()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(size());
        buffer.putShort((short) mcc());
        buffer.putShort((short) mnc());
        buffer.put(language());
        buffer.put(region());
        buffer.put((byte) orientation());
        buffer.put((byte) touchscreen());
        buffer.putShort((short) density());
        buffer.put((byte) keyboard());
        buffer.put((byte) navigation());
        buffer.put((byte) inputFlags());
        buffer.put((byte) 0);  // Padding
        buffer.putShort((short) screenWidth());
        buffer.putShort((short) screenHeight());
        buffer.putShort((short) sdkVersion());
        buffer.putShort((short) minorVersion());

        if (size() >= SCREEN_CONFIG_MIN_SIZE) {
            buffer.put((byte) screenLayout());
            buffer.put((byte) uiMode());
            buffer.putShort((short) smallestScreenWidthDp());
        }

        if (size() >= SCREEN_DP_MIN_SIZE) {
            buffer.putShort((short) screenWidthDp());
            buffer.putShort((short) screenHeightDp());
        }

        if (size() >= LOCALE_MIN_SIZE) {
            buffer.put(localeScript());
            buffer.put(localeVariant());
        }

        if (size() >= SCREEN_CONFIG_EXTENSION_MIN_SIZE) {
            buffer.put((byte) screenLayout2());
            buffer.put((byte) colorMode());
            buffer.putShort((short) 0); // Writing 2 bytes of padding
        }

        buffer.put(unknown());

        return buffer.array();
    }

    @Override
    public final String toString() {
        if (isDefault()) {  // Prevent the default configuration from returning the empty string
            return "default";
        }
        Map<Type, String> parts = toStringParts();
        mergeLocale(parts);
        Collection<String> values = parts.values();
        values.removeAll(Collections.singleton(""));
        return join(values, '-');
    }

    private static String join(Collection<String> values, char sep) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = values.iterator();
        boolean hasNext = iterator.hasNext();
        while (hasNext) {
            String next = iterator.next();
            sb.append(next);
            hasNext = iterator.hasNext();
            if (hasNext) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    /**
     * Merges the locale for {@code parts} if necessary.
     *
     * <p>Android supports a modified BCP 47 tag containing script and variant. If script or variant
     * are provided in the configuration, then the locale section should appear as:
     *
     * <p>{@code b+language+script+region+variant}
     */
    private void mergeLocale(Map<Type, String> parts) {
        String script = localeScriptString();
        String variant = localeVariantString();
        if (script.isEmpty() && variant.isEmpty()) {
            return;
        }
        StringBuilder locale = new StringBuilder("b+").append(languageString());
        if (!script.isEmpty()) {
            locale.append("+" + script);
        }
        String region = regionString();
        if (!region.isEmpty()) {
            locale.append("+" + region);
        }
        if (!variant.isEmpty()) {
            locale.append("+" + variant);
        }
        parts.put(Type.LANGUAGE_STRING, locale.toString());
        parts.remove(Type.LOCALE_SCRIPT_STRING);
        parts.remove(Type.REGION_STRING);
        parts.remove(Type.LOCALE_VARIANT_STRING);
    }

    /**
     * Returns a map of the configuration parts for {@link #toString}.
     *
     * <p>If a configuration part is not defined for this {@link ResourceConfiguration}, its value
     * will be the empty string.
     */
    public final Map<Type, String> toStringParts() {
        Map<Type, String> result = new LinkedHashMap<>();  // Preserve order for #toString().
        result.put(Type.MCC, mcc() != 0 ? "mcc" + mcc() : "");
        result.put(Type.MNC, mnc() != 0 ? "mnc" + mnc() : "");
        result.put(Type.LANGUAGE_STRING, languageString());
        result.put(Type.LOCALE_SCRIPT_STRING, localeScriptString());
        result.put(Type.REGION_STRING, !regionString().isEmpty() ? "r" + regionString() : "");
        result.put(Type.LOCALE_VARIANT_STRING, localeVariantString());
        result.put(Type.SCREEN_LAYOUT_DIRECTION,
                getOrDefault(SCREENLAYOUT_LAYOUTDIR_VALUES, screenLayoutDirection(), ""));
        result.put(Type.SMALLEST_SCREEN_WIDTH_DP,
                smallestScreenWidthDp() != 0 ? "sw" + smallestScreenWidthDp() + "dp" : "");
        result.put(Type.SCREEN_WIDTH_DP, screenWidthDp() != 0 ? "w" + screenWidthDp() + "dp" : "");
        result.put(Type.SCREEN_HEIGHT_DP, screenHeightDp() != 0 ? "h" + screenHeightDp() + "dp" : "");
        result.put(Type.SCREEN_LAYOUT_SIZE,
                getOrDefault(SCREENLAYOUT_SIZE_VALUES, screenLayoutSize(), ""));
        result.put(Type.SCREEN_LAYOUT_LONG,
                getOrDefault(SCREENLAYOUT_LONG_VALUES, screenLayoutLong(), ""));
        result.put(Type.SCREEN_LAYOUT_ROUND,
                getOrDefault(SCREENLAYOUT_ROUND_VALUES, screenLayoutRound(), ""));
        result.put(Type.COLOR_MODE_HDR, getOrDefault(COLOR_MODE_HDR_VALUES, colorModeHdr(), ""));
        result.put(
                Type.COLOR_MODE_WIDE_COLOR_GAMUT,
                getOrDefault(COLOR_MODE_WIDE_COLOR_GAMUT_VALUES, colorModeWideColorGamut(), ""));
        result.put(Type.ORIENTATION, getOrDefault(ORIENTATION_VALUES, orientation(), ""));
        result.put(Type.UI_MODE_TYPE, getOrDefault(UI_MODE_TYPE_VALUES, uiModeType(), ""));
        result.put(Type.UI_MODE_NIGHT, getOrDefault(UI_MODE_NIGHT_VALUES, uiModeNight(), ""));
        result.put(Type.DENSITY_DPI, getOrDefault(DENSITY_DPI_VALUES, density(), density() + "dpi"));
        result.put(Type.TOUCHSCREEN, getOrDefault(TOUCHSCREEN_VALUES, touchscreen(), ""));
        result.put(Type.KEYBOARD_HIDDEN, getOrDefault(KEYBOARDHIDDEN_VALUES, keyboardHidden(), ""));
        result.put(Type.KEYBOARD, getOrDefault(KEYBOARD_VALUES, keyboard(), ""));
        result.put(Type.NAVIGATION_HIDDEN,
                getOrDefault(NAVIGATIONHIDDEN_VALUES, navigationHidden(), ""));
        result.put(Type.NAVIGATION, getOrDefault(NAVIGATION_VALUES, navigation(), ""));
        result.put(Type.SCREEN_SIZE,
                screenWidth() != 0 || screenHeight() != 0 ? screenWidth() + "x" + screenHeight() : "");

        String sdkVersion = "";
        if (sdkVersion() != 0) {
            sdkVersion = "v" + sdkVersion();
            if (minorVersion() != 0) {
                sdkVersion += "." + minorVersion();
            }
        }
        result.put(Type.SDK_VERSION, sdkVersion);
        return result;
    }

    private <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        // TODO(acornwall): Remove this when Java 8's Map#getOrDefault is available.
        // Null is not returned, even if the map contains a key whose value is null. This is intended.
        V value = map.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Provides a builder for creating {@link ResourceConfiguration} instances.
     */
    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder size(int size);

        public abstract Builder mcc(int mcc);

        public abstract Builder mnc(int mnc);

        public abstract Builder language(byte[] language);

        public abstract Builder region(byte[] region);

        public abstract Builder orientation(int orientation);

        public abstract Builder touchscreen(int touchscreen);

        public abstract Builder density(int density);

        public abstract Builder keyboard(int keyboard);

        public abstract Builder navigation(int navigation);

        public abstract Builder inputFlags(int inputFlags);

        public abstract Builder screenWidth(int screenWidth);

        public abstract Builder screenHeight(int screenHeight);

        public abstract Builder sdkVersion(int sdkVersion);

        public abstract Builder minorVersion(int minorVersion);

        public abstract Builder screenLayout(int screenLayout);

        public abstract Builder uiMode(int uiMode);

        public abstract Builder smallestScreenWidthDp(int smallestScreenWidthDp);

        public abstract Builder screenWidthDp(int screenWidthDp);

        public abstract Builder screenHeightDp(int screenHeightDp);

        public abstract Builder localeScript(byte[] localeScript);

        public abstract Builder localeVariant(byte[] localeVariant);

        public abstract Builder screenLayout2(int screenLayout2);

        public abstract Builder colorMode(int colorMode);

        abstract Builder unknown(byte[] unknown);

        public abstract ResourceConfiguration build();
    }
}
