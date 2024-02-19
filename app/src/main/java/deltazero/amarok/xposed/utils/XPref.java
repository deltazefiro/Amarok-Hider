package deltazero.amarok.xposed.utils;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.github.kyuubiran.ezxhelper.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import deltazero.amarok.BuildConfig;

public class XPref {
    public static final String XPREF_PATH = "deltazero.amarok.xposed.prefs";
    private static XSharedPreferences xPref;
    /** @noinspection FieldCanBeLocal*/
    private static SharedPreferences.OnSharedPreferenceChangeListener xPrefListener;

    public static final String HIDE_PKG_NAMES = "hidePkgNames";
    public static final String IS_HIDDEN = "isHidden";
    public static final String ENABLE_X_HIDE = "enableXHide";

    public static Set<String> hidePkgNamesCache = Collections.emptySet();
    public static boolean isHiddenCache = false;
    public static boolean enableXHideCache = false;

    public static void init() {
        Log.d("Initializing XPref...", null);

        try {
            xPref = new XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH);
            xPref.makeWorldReadable();
        } catch (Exception e) {
            Log.w("Failed to make XPref world readable.", e);
        }

        if (!xPref.getFile().canRead()) {
            Log.w("No XPref found. Launch Amarok once and reboot to activate XHide.", null);
            xPref = null;
            return;
        }

        xPrefListener = (sharedPreferences, ignored) -> {
            // Note that by design it is not possible to determine which particular preference changed
            // and thus preference key in listener's callback invocation will always be null.
            xPref.reload();
            enableXHideCache = xPref.getBoolean(ENABLE_X_HIDE, false);
            isHiddenCache = xPref.getBoolean(IS_HIDDEN, false);
            hidePkgNamesCache = xPref.getStringSet(HIDE_PKG_NAMES, Collections.emptySet());
            Log.i("XPref cache refreshed. isXHideActive = " + isXHideActive() +
                    ", hidePkgNames = " + Arrays.toString(hidePkgNamesCache.toArray()), null);
        };
        //noinspection deprecation /* Supported since API 93 by LSPosed & EdXposed */
        xPref.registerOnSharedPreferenceChangeListener(xPrefListener);

        Log.i("XPref initialized.", null);
    }

    public static boolean isXHideActive() {
        return isHiddenCache && enableXHideCache;
    }

    public static boolean shouldHide(String pkgName) {
        return isHiddenCache && enableXHideCache && hidePkgNamesCache.contains(pkgName);
    }
}
