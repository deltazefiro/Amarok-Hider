package deltazero.amarok.xposed.utils;

import com.github.kyuubiran.ezxhelper.Log;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import deltazero.amarok.BuildConfig;

public class XPref {
    private static XSharedPreferences xPref;

    public static final String XPREF_PATH = "deltazero.amarok.xposed.prefs";
    public static final String HIDE_PKG_NAMES = "hidePkgNames";
    public static final String IS_ACTIVE = "isActive";

    private static boolean isActiveCache = false;
    private static Set<String> hidePkgNamesCache = Collections.emptySet();

    public static void init() {
        Log.d("Initializing XPref...", null);

        xPref = new XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH);
        Log.d("xPref path: " + xPref.getFile().getAbsolutePath(), null);

        if (xPref.getFile().canRead()) {
            xPref.reload();
            Log.d("xPref content: " + xPref.getAll(), null);
        } else {
            Log.wx("No XPref found. Launch Amarok once to activate XHide.", null);
        }

        Log.ix("XPref initialized.", null);
    }

    public static void refreshCache() {
        xPref.reload();
        if (!xPref.getFile().canRead()) return;
        isActiveCache = xPref.getBoolean(IS_ACTIVE, false);
        hidePkgNamesCache = xPref.getStringSet(HIDE_PKG_NAMES, Collections.emptySet());
    }

    public static boolean isXHideActive() {
        return isActiveCache;
    }

    public static boolean shouldHide(String pkgName) {
        return hidePkgNamesCache.contains(pkgName);
    }

    public static String getXPrefDir() {
        // This function can be called without initializing XPref
        return new XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH)
                .getFile().getParentFile().getAbsolutePath();
    }
}
