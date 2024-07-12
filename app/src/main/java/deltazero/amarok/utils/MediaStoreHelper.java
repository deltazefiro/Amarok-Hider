package deltazero.amarok.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import java.util.Set;

public class MediaStoreHelper {
    private static final String TAG = "MediaStoreHelper";

    public static void scan(Context context, Set<String> dirs) {
        try {
            MediaScannerConnection.scanFile(context, dirs.toArray(new String[0]), null,
                    (ignore, ignore2) -> Log.d(TAG, "MediaStore cache refreshed"));
        } catch (Exception e) {
            // MediaScannerConnection.scanFile may throw ArrayIndexOutOfBoundsException on OxygenOS 11
            // See https://github.com/deltazefiro/Amarok-Hider/issues/171#issuecomment-2225104851
            Log.w(TAG, "Error while rescanning media store", e);
        }
    }
}
