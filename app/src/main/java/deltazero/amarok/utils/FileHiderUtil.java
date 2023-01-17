package deltazero.amarok.utils;

import static deltazero.amarok.FileHider.FILENAME_ENDING_FULL_PROCESS_MARK;

import android.util.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileHiderUtil {

    private final static List<String> COMMON_TEXT_EXTENSION = List.of(".txt", ".md", ".lrc");
    private final static int ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB = 5 * 1024;

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
        if (filename.endsWith(FILENAME_ENDING_FULL_PROCESS_MARK))
            filename = filename.replace(FILENAME_ENDING_FULL_PROCESS_MARK, "");

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
