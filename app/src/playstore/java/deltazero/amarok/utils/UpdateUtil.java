package deltazero.amarok.utils;

import android.content.Context;

import androidx.annotation.NonNull;

public class UpdateUtil {

    public enum UpdateChannel {
        RELEASE, BETA;

        public static UpdateChannel fromString(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (Exception e) {
                return RELEASE;
            }
        }
    }

    public record Release(String version, String url) {
    }

    public static void checkAndNotify(@NonNull Context context, boolean silent) {
        // No-op: Play Store manages updates for this build
    }

    public static boolean isAvailable() {
        return false;
    }

    public static String getStorePageUrl() {
        // Play store is not ready yet
        return "https://github.com/deltazefiro/Amarok-Hider/releases";
    }
}
