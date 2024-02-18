package deltazero.amarok.xposed.utils;

import java.util.Collections;

import de.robv.android.xposed.XSharedPreferences;
import deltazero.amarok.BuildConfig;

public class XPref {
    public static final String XPREF_PATH = "deltazero.amarok.xposed.prefs";

    public static final String HIDE_PKG_NAMES = "hidePkgNames";
    public static final String ENABLE_X_HIDE = "enableXHide";

    private static XSharedPreferences pref;

    public static void init() {
        pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH);
        if (!pref.getFile().canRead()) pref = null;
    }

    public static boolean isXHideEnabled() {
        if (pref == null) return false;
        return pref.getBoolean(ENABLE_X_HIDE, false);
    }

    public static boolean shouldHide(String pkgName) {
        if (pref == null) return false;
        return pref.getBoolean(ENABLE_X_HIDE, false)
                && pref.getStringSet(HIDE_PKG_NAMES, Collections.emptySet()).contains(pkgName);
    }
}
