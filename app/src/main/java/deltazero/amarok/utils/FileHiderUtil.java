package deltazero.amarok.utils;

import static deltazero.amarok.filehider.ObfuscateFileHider.FILENAME_FULL_PROCESS_MARK;
import static deltazero.amarok.filehider.ObfuscateFileHider.FILENAME_HEADER_PROCESS_MARK;
import static deltazero.amarok.filehider.ObfuscateFileHider.FILENAME_NO_PROCESS_MARK;

import android.util.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileHiderUtil {

    private final static List<String> COMMON_TEXT_EXTENSION = List.of(".txt", ".md", ".lrc");
    private final static int ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB = 5 * 1024;

    public static String stripEnding(String str, String suffix) {
        if (str.isEmpty() || suffix.isEmpty()) {
            return str;
        }
        int indexOfLast = str.lastIndexOf(suffix);
        if (indexOfLast >= 0) {
            str = str.substring(0, indexOfLast);
        }
        return str;
    }

    public static String stripStart(String str, String prefix) {
        if (str.isEmpty() || prefix.isEmpty()) {
            return str;
        }
        if (str.startsWith(prefix)){
            return str.substring(prefix.length());
        }
        return str;
    }

    public static boolean checkIsMarkInFilename(String filename) {
        // Strip leading dot
        if (filename.startsWith("."))
            filename = filename.substring(1);

        return filename.endsWith(FILENAME_NO_PROCESS_MARK)
                || filename.endsWith(FILENAME_HEADER_PROCESS_MARK)
                || filename.endsWith(FILENAME_FULL_PROCESS_MARK);
    }


    /**
     * Remove leading-dot & full obfuscation mark form the filename.
     *
     * @param filename Original filename.
     * @return Striped filename.
     */
    public static String stripFilenameExtras(String filename) {

        // Strip leading dot
        if (filename.startsWith("."))
            filename = filename.substring(1);

        // Strip ending mark
        filename = stripEnding(filename, FILENAME_NO_PROCESS_MARK);
        filename = stripEnding(filename, FILENAME_HEADER_PROCESS_MARK);
        filename = stripEnding(filename, FILENAME_FULL_PROCESS_MARK);

        return filename;
    }

    /**
     * Get file size.
     *
     * @param p Path to the file.
     * @return File size, in KB.
     */
    public static int getFileSizeKB(Path p) {
        return Integer.parseInt(String.valueOf(p.toFile().length() / 1024));
    }

    public static boolean checkIsTextFile(String filename) {
        int idx = filename.lastIndexOf(".");
        if (idx == -1)
            return false;
        return COMMON_TEXT_EXTENSION.contains(filename.substring(idx));
    }

    public static boolean checkIsTextFileEnhanced(Path path) {
        String type;
        try {
            type = Files.probeContentType(path);
        } catch (Exception e) {
            Log.w("FileHider", String.format("%s: Failed to check is text: ", path.getFileName().toString()), e);
            return false;
        }

        if (type == null) {
            // `Files` failed to probe context type. If the files is smaller than
            // ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB, assume it is a text file.
            Log.d("FileHider", String.format("%s: Failed to probe MIME: ", path.getFileName().toString()));
            return getFileSizeKB(path) <= ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB;
        }

        Log.d("FileHider", String.format("%s: MIME type: %s", path.getFileName().toString(), type));
        return type.startsWith("text/")
                || type.contains("lrc")
                || type.contains("json")
                || type.contains("xml")
                || type.contains("html");
    }
}
