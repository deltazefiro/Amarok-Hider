package deltazero.amarok.benchmark;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.filehider.ObfuscateFileHider;
import deltazero.amarok.utils.MediaStoreHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ObfuscateFileHiderBenchmarkTest {
    private static final String TAG = "ObfuscateBench";
    private static final int DEFAULT_DEPTH = 4;
    private static final int DEFAULT_BRANCHING = 3;
    private static final int DEFAULT_FILES_PER_DIR = 10;
    private static final int BASE64_TAG = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;

    @Test
    public void benchmarkRenameOnly() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (!PrefMgr.initialized) {
            PrefMgr.init(context);
        }

        PrefMgr.setFileHiderMode(ObfuscateFileHider.class);
        PrefMgr.setEnableObfuscateFileHeader(false);
        PrefMgr.setEnableObfuscateTextFile(false);
        PrefMgr.setEnableObfuscateTextFileEnhanced(false);

        Bundle args = InstrumentationRegistry.getArguments();
        int depth = Math.max(1, getIntArg(args, "depth", DEFAULT_DEPTH));
        int branching = Math.max(0, getIntArg(args, "branching", DEFAULT_BRANCHING));
        int filesPerDir = Math.max(1, getIntArg(args, "filesPerDir", DEFAULT_FILES_PER_DIR));

        File baseDir = context.getExternalFilesDir(null);
        if (baseDir == null) {
            baseDir = context.getFilesDir();
        }
        File rootDir = new File(baseDir, "bench_obfuscate");
        deleteRecursively(rootDir);
        if (!rootDir.mkdirs() && !rootDir.isDirectory()) {
            throw new IOException("Failed to create benchmark root: " + rootDir);
        }

        TreeStats stats = new TreeStats();
        createTree(rootDir, depth, branching, filesPerDir, stats);

        List<String> samples = buildSampleRelativePaths(depth, branching);
        for (String sample : samples) {
            File file = new File(rootDir, sample);
            assertTrue("Sample file missing before hide: " + file, file.exists());
        }

        ObfuscateFileHider hider = new ObfuscateFileHider(context);
        Set<String> targetDirs = Collections.singleton(rootDir.getAbsolutePath());

        long hideDuration;
        long unhideDuration;
        MediaStoreHelper.disableScanForTesting = true;
        try {
            long startHide = SystemClock.elapsedRealtimeNanos();
            hider.hide(targetDirs);
            hideDuration = SystemClock.elapsedRealtimeNanos() - startHide;

            for (String sample : samples) {
                assertHidden(rootDir, sample);
            }

            long startUnhide = SystemClock.elapsedRealtimeNanos();
            hider.unhide(targetDirs);
            unhideDuration = SystemClock.elapsedRealtimeNanos() - startUnhide;

            for (String sample : samples) {
                assertUnhidden(rootDir, sample);
            }
        } finally {
            MediaStoreHelper.disableScanForTesting = false;
        }

        Log.i(TAG, "obfuscate_bench {" +
                "\"depth\":" + depth +
                ",\"branching\":" + branching +
                ",\"filesPerDir\":" + filesPerDir +
                ",\"totalDirs\":" + stats.dirCount +
                ",\"totalFiles\":" + stats.fileCount +
                ",\"hideNs\":" + hideDuration +
                ",\"unhideNs\":" + unhideDuration +
                "}");
    }

    private static int getIntArg(Bundle args, String key, int defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        String value = args.getString(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void createTree(File dir, int depth, int branching, int filesPerDir,
                                   TreeStats stats) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create dir: " + dir);
        }
        stats.dirCount++;

        for (int i = 0; i < filesPerDir; i++) {
            File file = new File(dir, "file_" + i + ".txt");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                String content = "bench-" + dir.getName() + "-" + i;
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            }
            stats.fileCount++;
        }

        if (depth <= 1 || branching <= 0) {
            return;
        }

        for (int i = 0; i < branching; i++) {
            File child = new File(dir, "dir_" + i);
            createTree(child, depth - 1, branching, filesPerDir, stats);
        }
    }

    private static List<String> buildSampleRelativePaths(int depth, int branching) {
        List<String> samples = new ArrayList<>();
        samples.add("file_0.txt");
        if (depth > 1 && branching > 0) {
            StringBuilder current = new StringBuilder("dir_0");
            samples.add(current + File.separator + "file_0.txt");
            if (depth > 2) {
                current.append(File.separator).append("dir_0");
                samples.add(current + File.separator + "file_0.txt");
            }
        }
        return samples;
    }

    private static void assertHidden(File rootDir, String relativePath) {
        File original = new File(rootDir, relativePath);
        File hidden = hiddenPathFor(rootDir, relativePath);
        assertFalse("Original should be hidden: " + original, original.exists());
        assertTrue("Hidden file missing: " + hidden, hidden.exists());
        assertTrue("Hidden name missing prefix: " + hidden.getName(), hidden.getName().startsWith("."));
        assertTrue("Hidden name missing suffix: " + hidden.getName(),
                hidden.getName().endsWith(ObfuscateFileHider.FILENAME_NO_PROCESS_MARK));
    }

    private static void assertUnhidden(File rootDir, String relativePath) {
        File original = new File(rootDir, relativePath);
        File hidden = hiddenPathFor(rootDir, relativePath);
        assertTrue("Original should be restored: " + original, original.exists());
        assertFalse("Hidden file should be gone: " + hidden, hidden.exists());
    }

    private static File hiddenPathFor(File rootDir, String relativePath) {
        Path relative = Paths.get(relativePath);
        Path current = rootDir.toPath();
        for (Path part : relative) {
            current = current.resolve(encodeName(part.toString()));
        }
        return current.toFile();
    }

    private static String encodeName(String name) {
        String encoded = Base64.encodeToString(name.getBytes(StandardCharsets.UTF_8), BASE64_TAG);
        return "." + encoded + ObfuscateFileHider.FILENAME_NO_PROCESS_MARK;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private static class TreeStats {
        long dirCount = 0;
        long fileCount = 0;
    }
}
