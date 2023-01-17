package deltazero.amarok.utils;

import android.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import kotlin.reflect.KType;

public class FileHiderUtil {


    /**
     * Remove leading-dot & full obfuscation mark form the filename.
     * @param filename Original filename.
     * @return Striped filename.
     */
    public static String stripFilenameExtras(String filename) {

        // Strip leading dot
        if (filename.startsWith("."))
            filename = filename.substring(1);

        // Strip

        return filename;
    }

    /**
     * Get file size.
     *
     * @param p Path to the file.
     * @return File size, in MB.
     */
    public static int getFileSize(Path p) {
        return Integer.parseInt(String.valueOf(p.toFile().length() / 1024 / 1024));
    }

    private final static List<String> COMMON_TEXT_EXTENSION = List.of(".txt", ".md");
    public static boolean checkIsTextFile(String filename) {
        String extension = filename.substring(filename.lastIndexOf("."));
        return COMMON_TEXT_EXTENSION.contains(extension);
    }

    public static boolean checkIsTextFileEnhanced(Path path) {
        String type;
        try {
            type = Files.probeContentType(path);
        } catch (IOException e) {
            Log.w("FileHider", "Failed to check is text: ", e);
            return false;
        }

        return type.startsWith("text/")
                || type.contains("json")
                || type.contains("xml")
                || type.contains("html");
    }
}
