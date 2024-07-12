package deltazero.amarok.filehider;

import static java.nio.charset.StandardCharsets.UTF_8;
import static deltazero.amarok.filehider.BaseFileHider.ProcessMethod.HIDE;
import static deltazero.amarok.filehider.BaseFileHider.ProcessMethod.UNHIDE;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.utils.FileHiderUtil;
import deltazero.amarok.utils.MediaStoreHelper;

public class ObfuscateFileHider extends BaseFileHider {
    private final static String TAG = "FileHider";

    private final static int MAX_PROCESS_WHOLE_FILE_SIZE_KB = 10 * 1024; // In KB.
    private final static int MAX_PROCESS_ENHANCED_WHOLE_FILE_SIZE_KB = 30 * 1024;
    private final static int BASE64_TAG = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    public final static String FILENAME_NO_PROCESS_MARK = "!amk";
    public final static String FILENAME_FULL_PROCESS_MARK = "!amk1";
    public final static String FILENAME_HEADER_PROCESS_MARK = "!amk2";

    public boolean processHeader;
    public boolean processTextFile;
    public boolean processTextFileEnhanced;

    public ObfuscateFileHider(Context context) {
        super(context);
        processHeader = PrefMgr.getEnableObfuscateFileHeader();
        processTextFile = PrefMgr.getEnableObfuscateTextFile();
        processTextFileEnhanced = PrefMgr.getEnableObfuscateTextFileEnhanced();
    }

    @Override
    protected void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException {
        for (var dir : targetDirs) {
            try {
                processTree(Paths.get(dir), method);
            } catch (InterruptedException e) {
                throw new InterruptedException();
            } catch (Exception e) {
                Log.w(TAG, String.format("Failed to process %s: ", dir), e);
            }
        }
        MediaStoreHelper.scan(context, targetDirs);
    }

    @Override
    public void tryToActive(ActivationCallbackListener activationCallbackListener) {
        activationCallbackListener.onActivateCallback(this.getClass(), true, 0);
    }

    @Override
    public String getName() {
        return "Obfuscate";
    }

    private void processTree(Path targetDir, ProcessMethod method) throws InterruptedException {

        Log.i(TAG, "Start to process file tree: " + targetDir);

        try {

            Files.walkFileTree(targetDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {

                    // Handle interruption
                    if (Thread.currentThread().isInterrupted()) {
                        Log.w(TAG, "File process interrupted.");
                        return FileVisitResult.TERMINATE;
                    }

                    // Skip .nomedia
                    if (path.getFileName().toString().equals(".nomedia"))
                        return FileVisitResult.CONTINUE;

                    // Check whether the whole file should be processed before renaming (processFilename).
                    boolean shouldProcessHeader = checkShouldProcessHeader(path, method);
                    boolean shouldProcessWhole = checkShouldProcessWhole(path, method);

                    // Choose filename ending mark
                    String endingMark = FILENAME_NO_PROCESS_MARK;
                    if (shouldProcessHeader)
                        endingMark = FILENAME_HEADER_PROCESS_MARK;
                    if (shouldProcessWhole)
                        endingMark = FILENAME_FULL_PROCESS_MARK;

                    // Process filename
                    // TODO: 2023/1/9 Handle long filename that invalid to android after Base64 encode
                    Path newPath = processFilename(path, method, endingMark);

                    // Process file content
                    if (newPath != null) {
                        if (shouldProcessWhole) { // Check shouldProcessWhole first.
                            processWholeFile(newPath);
                        } else if (shouldProcessHeader) { // Then check shouldProcessHeader.
                            processFileHeader(newPath);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (dir != targetDir)
                        processFilename(dir, method, FILENAME_NO_PROCESS_MARK);
                    return FileVisitResult.CONTINUE;
                }

            });

        } catch (IOException e) {
            Log.w(TAG, String.format("While processing '%s': %s", targetDir.getFileName(), e));
        }

        // Clear interrupted flag & throw InterruptedException
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    /**
     * Process the Filename and check if the process succeeds.
     *
     * @param path            The path to be processed.
     * @param method          Process method.
     * @param extraEndingMark (only effective when `HIDE`) Extra mark to be append to the end of the filename.
     * @return If the process succeeds, return the new path. Otherwise, return null.
     */
    @Nullable
    private Path processFilename(Path path, ProcessMethod method, String extraEndingMark) {
        String filename = path.getFileName().toString();
        String newFilename = null;
        Path newPath;

        boolean hasEncoded = FileHiderUtil.checkIsMarkInFilename(filename);

        if (method == HIDE) {
            if (hasEncoded) {
                Log.d(TAG, "Found encoded name: " + filename + ", skip...");
                return null;
            }

            newFilename = "." + Base64.encodeToString(filename.getBytes(UTF_8), BASE64_TAG) + extraEndingMark;
            Log.d(TAG, "Encode: " + path + " -> " + newFilename);

        } else if (method == UNHIDE) {
            if (!hasEncoded) {
                Log.w(TAG, "Found not coded name: " + filename + ", skip...");
                return null;
            }

            try {
                newFilename = new String(
                        Base64.decode(FileHiderUtil.stripFilenameExtras(filename), BASE64_TAG), UTF_8
                );
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

    private void processFileHeader(Path path) {
        Log.d(TAG, "Processing file header: " + path);

        try {

            File file = path.toFile();

            // Preserve original lastModified time
            var lastModified = path.toFile().lastModified();

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {

                byte[] bytes = new byte[8];
                int numBytesRead = randomAccessFile.read(bytes);
                int numBytesToReplace = Math.max(Math.min(numBytesRead, 8), 0);

                for (int i = 0; i < numBytesToReplace; i++) {
                    bytes[i] = (byte) ~bytes[i];
                }

                randomAccessFile.seek(0);
                randomAccessFile.write(bytes, 0, numBytesToReplace);

            } catch (IOException e) {
                Log.w(TAG, "processFileHeader failed: ", e);
            }

            // noinspection ResultOfMethodCallIgnored
            file.setLastModified(lastModified);

        } catch (SecurityException e) {
            Log.w(TAG, "processFileHeader failed: ", e);
        }
    }

    private void processWholeFile(Path path) {
        Log.d(TAG, "Processing whole file: " + path);

        byte[] buffer = new byte[1024];
        long numReadLoops = 0;
        int numBytesRead, numBytesToReplace;

        try {

            File file = path.toFile();

            // Preserve original lastModified time
            var lastModified = path.toFile().lastModified();

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                while ((numBytesRead = randomAccessFile.read(buffer)) != -1) {

                    numBytesToReplace = Math.max(Math.min(numBytesRead, buffer.length), 0);

                    for (int i = 0; i < numBytesToReplace; i++) {
                        buffer[i] = (byte) ~buffer[i];
                    }

                    randomAccessFile.seek(numReadLoops * buffer.length);
                    randomAccessFile.write(buffer, 0, numBytesToReplace);

                    numReadLoops++;
                }
            } catch (IOException e) {
                Log.w(TAG, "processWholeFile failed: ", e);
            }

            // noinspection ResultOfMethodCallIgnored
            file.setLastModified(lastModified);

        } catch (SecurityException e) {
            Log.w(TAG, "processWholeFile failed: ", e);
        }
    }

    private boolean checkShouldProcessWhole(Path path, ProcessMethod method) {

        if ((!processHeader) || (!processTextFile)) {
            return false;
        }

        String filename = path.getFileName().toString();
        if (method == HIDE) {

            if (processTextFileEnhanced) {
                return FileHiderUtil.checkIsTextFileEnhanced(path)
                        && FileHiderUtil.getFileSizeKB(path) <= MAX_PROCESS_WHOLE_FILE_SIZE_KB;
            } else { // Not enhanced
                return FileHiderUtil.checkIsTextFile(filename)
                        && FileHiderUtil.getFileSizeKB(path) <= MAX_PROCESS_ENHANCED_WHOLE_FILE_SIZE_KB;
            }

        } else { // UNHIDE
            return filename.endsWith(FILENAME_FULL_PROCESS_MARK);
        }
    }

    private boolean checkShouldProcessHeader(Path path, ProcessMethod method) {
        String filename = path.getFileName().toString();
        if (method == HIDE) return processHeader;
        else return filename.endsWith(FILENAME_HEADER_PROCESS_MARK);
    }
}
