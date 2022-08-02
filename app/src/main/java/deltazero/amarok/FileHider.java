package deltazero.amarok;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileHider {
    private final static String TAG = "FileHider";
    private final static String ENCODED_TAG = "[AMAROK]";
    private final static int BASE64_TAG = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    private static void processFilename(Path path, ProcessMethod processMethod) throws IOException {
        String filename = path.getFileName().toString();
        Path newPath = null;

        if (processMethod == ProcessMethod.ENCODE) {

            // Check if the filename have already been encoded.
            try {
                if (new String(Base64.decode(filename, BASE64_TAG), UTF_8)
                        .startsWith(ENCODED_TAG)) {
                    Log.d(TAG, "Found encoded filename: " + filename + ", skip...");
                    return;
                }
            } catch (IllegalArgumentException e) {
                // Filename is not encoded
            }

            filename = Base64.encodeToString((ENCODED_TAG + filename).getBytes(UTF_8), BASE64_TAG);
            newPath = Paths.get(path.getParent().toString(), filename);

            Log.d(TAG, "Encode: " + path.toString() + " -> " + newPath.toString());

        } else if (processMethod == ProcessMethod.DECODE) {

            try {
                filename = new String(Base64.decode(filename, BASE64_TAG), UTF_8);
                if (!filename.startsWith(ENCODED_TAG)) {
                    Log.w(TAG, "Found not coded text: " + path.getFileName().toString() + ", skip...");
                    return;
                }
                newPath = Paths.get(path.getParent().toString(), filename.replace(ENCODED_TAG, ""));
                Log.d(TAG, "Decode: " + path.toString() + " -> " + newPath.toString());
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unable to decode: " + filename);
                return;
            }

        }

        Files.move(path, newPath);

    }

    public static void process(Path targetDir, ProcessMethod processMethod) {
        /*
        Use Base64 to encode the filename, making filename unreadable.
        TODO: 1.Handle long filename that invalid to android after Base64 encode
              2.Insert random binary to the head of files to make them unreadable
         */

        Log.d(TAG, "Applying hider to: " + targetDir);

        try {
            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    processFilename(file, processMethod);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException {
                    if (dir != targetDir)
                        processFilename(dir, processMethod);
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public enum ProcessMethod {
        ENCODE, DECODE
    }
}
