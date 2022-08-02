package deltazero.amarok;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Base64;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

public class FileEncoder {
    private final static String TAG = "FileProc";
    private final static String ENCODED_TAG = "[AMAROK]";

    private static void encode(DocumentFile f) {
        assert f.getName() != null;

        // Check if the file have already been encoded.
        try {
            if (new String(Base64.decode(f.getName(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING), UTF_8).startsWith(ENCODED_TAG)) {
                Log.d(TAG, "Found encoded filename: " + f.getUri().getLastPathSegment() + ", skip...");
                return;
            }
        } catch (IllegalArgumentException e) {
            // Decode attempt failed, process encoding
        }

        String encodedName = Base64.encodeToString((ENCODED_TAG + f.getName()).getBytes(UTF_8), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);

        Log.d(TAG, "Encode: " + f.getUri().getLastPathSegment() + " -> " + encodedName);
//        f.renameTo(encodedName);

    }

    private static void decode(DocumentFile f) {
        assert f.getName() != null;
        try {
            String decodedName = new String(Base64.decode(f.getName(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING), UTF_8);
            if (!decodedName.startsWith(ENCODED_TAG)) {
                Log.w(TAG, "Found plain filename: " + f.getUri().getLastPathSegment() + ", skip...");
                return;
            }
            decodedName = decodedName.replace(ENCODED_TAG, "");

            Log.d(TAG, "Decode: " + f.getUri().getLastPathSegment() + " -> " + decodedName);
//            f.renameTo(decodedName);

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
