package deltazero.amarok;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileHider {
    private final static String TAG = "FileHider";
    private final static String AMAROK_MARK = "[AMAROK]";
    private final static String ENCODED_AMAROK_MARK = "W0FNQVJPS1"; // The base64 encode of AMAROK_MARK
    private final static int BASE64_TAG = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    public enum ProcessMethod {
        HIDE, UNHIDE
    }

    private static String stripLeadingDot(String str) {
        if (str.startsWith("."))
            return str.substring(1);
        return str;
    }

    private static void process(Path path, ProcessMethod processMethod, boolean processHeader) {
        String filename = path.getFileName().toString();
        Path newPath = null;

        if (processMethod == ProcessMethod.HIDE) {

            // Check if the filename have already been encoded.
            try {
                if (stripLeadingDot(filename).startsWith(ENCODED_AMAROK_MARK)) {
                    Log.d(TAG, "Found encoded filename: " + filename + ", skip...");
                    return;
                }
            } catch (IllegalArgumentException e) {
                // Filename is not encoded
            }

            filename = "." + Base64.encodeToString((AMAROK_MARK + filename).getBytes(UTF_8), BASE64_TAG);
            newPath = Paths.get(path.getParent().toString(), filename);

            // Log.d(TAG, "Encode: " + path.toString() + " -> " + newPath.toString());

        } else if (processMethod == ProcessMethod.UNHIDE) {

            try {
                filename = new String(Base64.decode(stripLeadingDot(filename), BASE64_TAG), UTF_8);
                if (!filename.startsWith(AMAROK_MARK)) {
                    Log.w(TAG, "Found not coded text: " + path.getFileName().toString() + ", skip...");
                    return;
                }
                newPath = Paths.get(path.getParent().toString(), filename.replace(AMAROK_MARK, ""));
                // Log.d(TAG, "Decode: " + path.toString() + " -> " + newPath.toString());
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unable to decode: " + filename);
                return;
            }

        }

        assert newPath != null;
        boolean success = path.toFile().renameTo(newPath.toFile());

        if (success && processHeader) {
            processFileHeader(newPath);
        }

    }

    private static void processFileHeader(Path path) {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {

            byte[] bytes = new byte[8];
            int numBytesRead = file.read(bytes);
            int numBytesToReplace = Math.max(Math.min(numBytesRead, 8), 0);

            for (int i = 0; i < numBytesToReplace; i++) {
                bytes[i] = (byte) ~bytes[i];
            }

            file.seek(0);
            file.write(bytes, 0, numBytesToReplace);

        } catch (IOException e) {
            Log.w(TAG, e);
        }

    }

    public static void processFileTree(Path targetDir, ProcessMethod processMethod, boolean processHeader) {
        /*
        Use Base64 to encode the filename, making filename unreadable.
        TODO: 1.Handle long filename that invalid to android after Base64 encode
         */

        Log.i(TAG, "Applying hider to: " + targetDir);

        try {

            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    process(file, processMethod, processHeader);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (dir != targetDir)
                        process(dir, processMethod, processHeader);
                    return FileVisitResult.CONTINUE;
                }

            });

        } catch (IOException e) {
            Log.w(TAG, String.format("While processing '%s': %s", targetDir.getFileName(), e));
        }
    }

    public static void hide(Path targetDir, boolean processHeader) {
        processFileTree(targetDir, ProcessMethod.HIDE, processHeader);
    }

    public static void unhide(Path targetDir, boolean processHeader) {
        processFileTree(targetDir, ProcessMethod.UNHIDE, processHeader);
    }
}
