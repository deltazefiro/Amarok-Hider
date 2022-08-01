package deltazero.amarok;

import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.nio.charset.StandardCharsets;

public class FileEncoder {
    private final static String TAG = "FileProc";
    private final static String ENCODED_TAG = "[AMAROK]";

    private static void encode(DocumentFile f) {
        assert f.getName() != null;

        // Check if the file have already been encoded.
        try {
            if ((new String(Base85.getRfc1924Decoder().decodeToBytes(f.getName()), StandardCharsets.UTF_8))
                    .startsWith(ENCODED_TAG)) {
                Log.d(TAG, "Found encoded filename: " + f.getUri().getLastPathSegment() + ", skip...");
                return;
            }
        } catch (IllegalArgumentException e) {
            // Decode attempt failed, process encoding
        }

        String encodedName = Base85.getRfc1924Encoder().encodeToString((ENCODED_TAG + f.getName()).getBytes(StandardCharsets.UTF_8));

        Log.d(TAG, "Encode: " + f.getUri().getLastPathSegment() + " -> " + encodedName);
        f.renameTo(encodedName);

    }

    private static void decode(DocumentFile f) {
        assert f.getName() != null;
        try {
            String decodedName = new String(Base85.getRfc1924Decoder().decodeToBytes(f.getName()), StandardCharsets.UTF_8);
            if (!decodedName.startsWith(ENCODED_TAG)) {
                Log.w(TAG, "Found plain filename: " + f.getUri().getLastPathSegment() + ", skip...");
                return;
            }
            decodedName = decodedName.replace(ENCODED_TAG, "");

            Log.d(TAG, "Decode: " + f.getUri().getLastPathSegment() + " -> " + decodedName);
            f.renameTo(decodedName);

        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to decode: " + f.getUri().getLastPathSegment());
        }
    }

    public static void processFileTree(DocumentFile targetFile, ProcessMode processMode, Boolean isTopDir) {
        if (targetFile.isDirectory()) {
            for (DocumentFile f : targetFile.listFiles()) {
                processFileTree(f, processMode, false);
            }
        }

        // Do not process the top dir
        if (isTopDir) {
            return;
        }

        // Visit file + Post process the dir
        if (processMode == ProcessMode.ENCODE) {
            encode(targetFile);
        } else {
            decode(targetFile);
        }
    }

    enum ProcessMode {
        ENCODE, DECODE
    }
}
