package deltazero.amarok;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileProcessor {
    private final static String TAG = "FileProc";
    private final static String ENCODED_TAG = "[HIDER]";

    private static void processPathName(Path path, ProcessMethod processMethod) throws IOException {
        String name = path.getFileName().toString();
        Path newPath = null;

        if (processMethod == ProcessMethod.ENCODE) {

            // Check if the file have been encoded.
            try {
                if ((new String(Base85.getRfc1924Decoder().decodeToBytes(name), StandardCharsets.UTF_8))
                        .startsWith(ENCODED_TAG)) {
//                    Log.d(TAG, "Found encoded text: "+ name + ", skip...");
                    return;
                }
            } catch (IllegalArgumentException e) {
                // pass
            }

            name = Base85.getRfc1924Encoder().encodeToString((ENCODED_TAG + name).getBytes(StandardCharsets.UTF_8));
            newPath = Paths.get(path.getParent().toString(), name);

//            Log.d(TAG, "Encoding: " + path.toString() + " -> " + newPath.toString());

        } else if (processMethod == ProcessMethod.DECODE) {

            try {
                name = new String(Base85.getRfc1924Decoder().decodeToBytes(name), StandardCharsets.UTF_8);
                if (!name.startsWith(ENCODED_TAG)) {
//                    Log.w(TAG, "Found not coded text: "+ path.getFileName().toString() + ", skip...");
                    return;
                }
                newPath = Paths.get(path.getParent().toString(), name.replace(ENCODED_TAG, ""));
//                Log.d(TAG, "Decoding: " + path.toString() + " -> " + newPath.toString());
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Unable to decode: " + name);
                return;
            }

        }

        Files.move(path, newPath);

    }

    public static void process(Path targetDir, ProcessMethod processMethod) {
        Log.d(TAG, "Applying hider to: " + targetDir);

        try {
            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    processPathName(file, processMethod);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException {
                    if (dir != targetDir)
                        processPathName(dir, processMethod);
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
