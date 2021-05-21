package apk.arsc;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Utilities for working with apk files.
 */
public final class ApkUtils {

    private ApkUtils() {
    } // Prevent instantiation

    /**
     * Returns true if there exists a file whose name matches {@code filename} in {@code apkFile}.
     */
    public static boolean hasFile(File apkFile, String filename) throws IOException {
        try (ZipFile apkZip = new ZipFile(apkFile)) {
            return apkZip.getEntry(filename) != null;
        }
    }

    /**
     * Returns a file whose name matches {@code filename}, or null if no file was found.
     *
     * @param apkFile  The file containing the apk zip archive.
     * @param filename The full filename (e.g. res/raw/foo.bar).
     * @return A byte array containing the contents of the matching file, or null if not found.
     * @throws IOException Thrown if there's a matching file, but it cannot be read from the apk.
     */
    public static byte[] getFile(File apkFile, String filename) throws IOException {
        try (ZipFile apkZip = new ZipFile(apkFile)) {
            ZipEntry zipEntry = apkZip.getEntry(filename);
            if (zipEntry == null) {
                return null;
            }
            try (InputStream in = apkZip.getInputStream(zipEntry)) {
                return toByteArray(in);
            }
        }
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        byte[] buf = new byte[4 * 1024];
        int len;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = in.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toByteArray();
    }


    /**
     * Returns a file whose name matches {@code filename}, or null if no file was found.
     *
     * @param inputStream The input stream containing the apk zip archive.
     * @param filename    The full filename (e.g. res/raw/foo.bar).
     * @return A byte array containing the contents of the matching file, or null if not found.
     * @throws IOException Thrown if there's a matching file, but it cannot be read from the apk.
     */

    public static byte[] getFile(InputStream inputStream, String filename)
            throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (filename.equals(entry.getName())) {
                    return toByteArray(zipInputStream);
                }
                zipInputStream.closeEntry();
            }
        }
        return null;
    }

    public static List<byte[]> getTwoFile(InputStream inputStream, String filename1, String filename2)
            throws IOException {
        byte[] data1 = null;
        byte[] data2 = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (filename1.equals(name)) {
                    data1 = toByteArray(zipInputStream);
                } else if (filename2.equals(name)) {
                    data2 = toByteArray(zipInputStream);
                }
                zipInputStream.closeEntry();
                if (data1 != null && data2 != null) {
                    break;
                }
            }
        }
        return Arrays.asList(data1, data2);
    }

    /**
     * Returns all files in an apk that match a given regular expression.
     *
     * @param apkFile The file containing the apk zip archive.
     * @param regex   A regular expression to match the requested filenames.
     * @return A mapping of the matched filenames to their byte contents.
     * @throws IOException Thrown if a matching file cannot be read from the apk.
     */
    public static Map<String, byte[]> getFiles(File apkFile, String regex) throws IOException {
        return getFiles(apkFile, Pattern.compile(regex));
    }

    /**
     * Returns all files in an apk that match a given regular expression.
     *
     * @param apkFile The file containing the apk zip archive.
     * @param regex   A regular expression to match the requested filenames.
     * @return A mapping of the matched filenames to their byte contents.
     * @throws IOException Thrown if a matching file cannot be read from the apk.
     */
    public static Map<String, byte[]> getFiles(File apkFile, Pattern regex) throws IOException {
        try (ZipFile apkZip = new ZipFile(apkFile)) {
            Map<String, byte[]> result = new LinkedHashMap<>();
            final Enumeration<? extends ZipEntry> entries = apkZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (regex.matcher(entry.getName()).matches()) {
                    try (InputStream in = apkZip.getInputStream(entry)) {
                        result.put(entry.getName(), toByteArray(in));
                    }
                }
            }
            return result;
        }
    }


    /**
     * Reads all files from an input stream that is reading from a zip file.
     */
    public static Map<String, byte[]> getFiles(InputStream inputStream, Pattern regex)
            throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream);
             BufferedInputStream bis = new BufferedInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (regex.matcher(entry.getName()).matches()) {
                    files.put(entry.getName(), toByteArray(bis));
                }
                zipInputStream.closeEntry();
            }
        }
        return files;
    }

}
