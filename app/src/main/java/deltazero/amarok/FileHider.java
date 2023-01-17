package deltazero.amarok;

import static java.nio.charset.StandardCharsets.UTF_8;
import static deltazero.amarok.FileHider.ProcessConfig.ProcessMethod.HIDE;
import static deltazero.amarok.FileHider.ProcessConfig.ProcessMethod.UNHIDE;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import deltazero.amarok.utils.FileHiderUtil;

public class FileHider {
    private final static String TAG = "FileHider";

    private final static int MAX_PROCESS_WHOLE_FILE_SIZE_KB = 10 * 1024; // In KB.
    private final static int MAX_PROCESS_ENHANCED_WHOLE_FILE_SIZE_KB = 30 * 1024;
    private final static int BASE64_TAG = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    public final static String FILENAME_START_MARK = "[AMAROK]";
    public final static String FILENAME_START_MARK_ENCODED = "W0FNQVJPS1";
    public final static String FILENAME_ENDING_FULL_PROCESS_MARK = ".amk1";

    public static class ProcessConfig {
        public ProcessConfig(PrefMgr prefMgr) {
            processMethod = prefMgr.getIsHidden() ? UNHIDE : HIDE;
            processHeader = prefMgr.getEnableCorruptFileHeader();
            processTextFile = prefMgr.getEnableCorruptTextFile();
            processTextFileEnhanced = prefMgr.getEnableCorruptTextFileEnhanced();
        }

        public enum ProcessMethod {
            HIDE, UNHIDE
        }

        public ProcessMethod processMethod;
        public boolean processHeader;
        public boolean processTextFile;
        public boolean processTextFileEnhanced;
    }


    /**
     * Process the Filename and check if the process succeeds.
     *
     * @param path            The path to be processed.
     * @param processMethod   Process method.
     * @param extraEndingMark Extra mark to be append to the end of the filename.
     * @return If the process succeeds, return the new path. Otherwise, return null.
     */
    @Nullable
    private static Path processFilename(Path path, ProcessConfig.ProcessMethod processMethod, String extraEndingMark) {
        String filename = path.getFileName().toString();
        String newFilename = null;
        Path newPath;

        boolean hasEncoded = FileHiderUtil.stripFilenameExtras(filename).startsWith(FILENAME_START_MARK_ENCODED);

        if (processMethod == HIDE) {
            if (hasEncoded) {
                Log.d(TAG, "Found encoded name: " + filename + ", skip...");
                return null;
            }

            newFilename = "." + Base64.encodeToString((FILENAME_START_MARK + filename).getBytes(UTF_8), BASE64_TAG) + extraEndingMark;
            Log.d(TAG, "Encode: " + path + " -> " + newFilename);

        } else if (processMethod == UNHIDE) {
            if (!hasEncoded) {
                Log.w(TAG, "Found not coded name: " + filename + ", skip...");
                return null;
            }

            try {
                newFilename = new String(
                        Base64.decode(FileHiderUtil.stripFilenameExtras(filename), BASE64_TAG), UTF_8
                ).replace(FILENAME_START_MARK, "");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unable to decode: " + filename);
                return null;
            }

            Log.d(TAG, "Decode: " + path + " -> " + newFilename);
        }

        // Try to rename.

        assert newFilename != null;
        newPath = Paths.get(path.getParent().toString(), newFilename);

        boolean is_succeeded = path.toFile().renameTo(newPath.toFile());

        if (!is_succeeded) {
            Log.w(TAG, "Error when renaming file: " + path + " -> " + newPath);
            return null;
        } else {
            return newPath;
        }
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
            Log.w(TAG, "processFileHeader failed: ", e);
        }
    }

    private static void processWholeFile(Path path) {
        Log.d(TAG, "Processing whole file: " + path);

        byte[] buffer = new byte[1024];
        long numReadLoops = 0;
        int numBytesRead, numBytesToReplace;

        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {

            while ((numBytesRead = file.read(buffer)) != -1) {

                numBytesToReplace = Math.max(Math.min(numBytesRead, buffer.length), 0);

                for (int i = 0; i < numBytesToReplace; i++) {
                    buffer[i] = (byte) ~buffer[i];
                }

                file.seek(numReadLoops * buffer.length);
                file.write(buffer, 0, numBytesToReplace);

                numReadLoops++;
            }

        } catch (IOException e) {
            Log.w(TAG, "processWholeFile failed: ", e);
        }
    }

    private static boolean checkShouldProcessWhole(Path path, ProcessConfig processConfig) {

        if ((!processConfig.processHeader) || (!processConfig.processTextFile)) {
            return false;
        }

        String filename = path.getFileName().toString();
        if (processConfig.processMethod == HIDE) {

            if (processConfig.processTextFileEnhanced) {
                return FileHiderUtil.checkIsTextFileEnhanced(path)
                        && FileHiderUtil.getFileSizeKB(path) <= MAX_PROCESS_WHOLE_FILE_SIZE_KB;
            } else { // Not enhanced
                return FileHiderUtil.checkIsTextFile(filename)
                        && FileHiderUtil.getFileSizeKB(path) <= MAX_PROCESS_ENHANCED_WHOLE_FILE_SIZE_KB;
            }

        } else {
            // UNHIDE
            return filename.endsWith(FILENAME_ENDING_FULL_PROCESS_MARK);
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

                    // Check whether the whole file should be processed before renaming (processFilename).
                    boolean shouldProcessWhole = checkShouldProcessWhole(path, processConfig);

                    Path newPath = processFilename(path, processConfig.processMethod,
                            (shouldProcessWhole ? FILENAME_ENDING_FULL_PROCESS_MARK : ""));

                    if (newPath != null && processConfig.processHeader) {
                        if (shouldProcessWhole) {
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
                        processFilename(dir, processConfig.processMethod, "");
                    return FileVisitResult.CONTINUE;
                }

            });

        } catch (IOException e) {
            Log.w(TAG, String.format("While processing '%s': %s", targetDir.getFileName(), e));
        }
    }
}
