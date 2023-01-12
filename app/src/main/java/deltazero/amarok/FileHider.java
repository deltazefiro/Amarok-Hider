package deltazero.amarok;

import static java.nio.charset.StandardCharsets.UTF_8;
import static deltazero.amarok.FileHider.ProcessConfig.ProcessMethod.HIDE;
import static deltazero.amarok.FileHider.ProcessConfig.ProcessMethod.UNHIDE;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class FileHider {
    private final static String TAG = "FileHider";
    private final static String AMAROK_MARK = "[AMAROK]";
    private final static String ENCODED_AMAROK_MARK = "W0FNQVJPS1"; // The base64 encode of AMAROK_MARK
    private final static List<String> COMMON_TEXT_EXTENSION = List.of(".txt", ".md");
    private final static int MAX_PROCESS_WHOLE_FILE_SIZE = 10; // In MB.
    private final static int BASE64_TAG = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    public static class ProcessConfig {
        public ProcessConfig(PrefMgr prefMgr) {
            processMethod = prefMgr.getIsHidden() ? UNHIDE : HIDE;
            processHeader = prefMgr.getEnableCorruptFileHeader();
            processTextFile = prefMgr.getEnableCorruptTextFile();
            processTextFileEnhanced = prefMgr.getEnableTextFileEnhanced();
        }

        public enum ProcessMethod {
            HIDE, UNHIDE
        }

        public ProcessMethod processMethod;
        public boolean processHeader;
        public boolean processTextFile;
        public boolean processTextFileEnhanced;
    }

    private static String stripLeadingDot(String str) {
        if (str.startsWith("."))
            return str.substring(1);
        return str;
    }

    /**
     * Get file size.
     *
     * @param p Path to the file.
     * @return File size, in MB.
     */
    private static int getFileSize(Path p) {
        return Integer.parseInt(String.valueOf(p.toFile().length() / 1024 / 1024));
    }


    /**
     * Process the Filename and check if the process succeeds.
     *
     * @param path          The path to be processed.
     * @param processMethod Process method.
     * @return If the process succeeds, return the new path. Otherwise, return null.
     */
    @Nullable
    private static Path processFilename(Path path, ProcessConfig.ProcessMethod processMethod) {
        String filename = path.getFileName().toString();
        Path newPath = null;

        if (processMethod == HIDE) {

            // Check if the filename have already been encoded.
            try {
                if (stripLeadingDot(filename).startsWith(ENCODED_AMAROK_MARK)) {
                    Log.d(TAG, "Found encoded filename: " + filename + ", skip...");
                    return null;
                }
            } catch (IllegalArgumentException e) {
                // Filename is not encoded
            }

            filename = "." + Base64.encodeToString((AMAROK_MARK + filename).getBytes(UTF_8), BASE64_TAG);
            newPath = Paths.get(path.getParent().toString(), filename);

            Log.d(TAG, "Encode: " + path + " -> " + newPath.toString());

        } else if (processMethod == UNHIDE) {

            try {
                filename = new String(Base64.decode(stripLeadingDot(filename), BASE64_TAG), UTF_8);
                if (!filename.startsWith(AMAROK_MARK)) {
                    Log.w(TAG, "Found not coded text: " + path.getFileName().toString() + ", skip...");
                    return null;
                }
                newPath = Paths.get(path.getParent().toString(), filename.replace(AMAROK_MARK, ""));
                Log.d(TAG, "Decode: " + path + " -> " + newPath.toString());
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unable to decode: " + filename);
                return null;
            }

        }

        assert newPath != null;
        boolean success = path.toFile().renameTo(newPath.toFile());

        if (!success) {
            Log.w(TAG, "Error when renaming file: " + path + " -> " + newPath);
            return null;
        }

        return newPath;
    }

    private static void processFileHeader(Path path) {
        Log.d(TAG, "Processing file header: " + path);

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

    private static void processWholeFile(Path path) {
        Log.d(TAG, "Processing whole file: " + path);

        try (FileInputStream fis = new FileInputStream(path.toFile());
             FileOutputStream fos = new FileOutputStream(path.toFile())) {

            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < numRead; i++) {
                    buffer[i] = (byte) ~buffer[i];
                }
                fos.write(buffer, 0, numRead);
            }
        } catch (IOException e) {
            // handle exception
        }
    }

    /**
     * TODO: 2023/1/9 Handle long filename that invalid to android after Base64 encode
     */
    public static void process(Path targetDir, ProcessConfig processConfig) {

        Log.i(TAG, "Applying hider to: " + targetDir);

        try {

            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    Path newPath = processFilename(path, processConfig.processMethod);

                    if (newPath != null && processConfig.processHeader) {
                        String extension = path.toString().substring(path.toString().lastIndexOf("."));

                        if (processConfig.processTextFile
                                && COMMON_TEXT_EXTENSION.contains(extension)
                                && getFileSize(newPath) <= MAX_PROCESS_WHOLE_FILE_SIZE) {
                            processWholeFile(newPath);
                        } else {
                            processFileHeader(newPath);
                        }

                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (dir != targetDir)
                        processFilename(dir, processConfig.processMethod);
                    return FileVisitResult.CONTINUE;
                }

            });

        } catch (IOException e) {
            Log.w(TAG, String.format("While processing '%s': %s", targetDir.getFileName(), e));
        }
    }
}
