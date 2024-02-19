package deltazero.amarok.xposed.utils;

import com.github.kyuubiran.ezxhelper.Log;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import deltazero.amarok.BuildConfig;

public class XPref {
    public static final String XPREF_PATH = "deltazero.amarok.xposed.prefs";
    private static XSharedPreferences pref;

    public static final String HIDE_PKG_NAMES = "hidePkgNames";
    public static final String IS_HIDDEN = "isHidden";
    public static final String ENABLE_X_HIDE = "enableXHide";

    private static Set<String> hidePkgNamesCache = Collections.emptySet();
    private static boolean isHiddenCache = false;
    private static boolean enableXHideCache = false;

    public static void init() {
        pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH);
        if (!pref.getFile().canRead()) {
            Log.w("No XPref found. Launch Amarok once and reboot to activate XHide.", null);
            pref = null;
            return;
        }

        assert pref != null;
        //noinspection deprecation /* Supported since API 93 by LSPosed & EdXposed */
        pref.registerOnSharedPreferenceChangeListener(
                (sharedPreferences, ignore) -> {
                    Log.i("XPref changed. Refreshing cache.", null);
                    // Note that by design it is not possible to determine which particular preference changed
                    // and thus preference key in listener's callback invocation will always be null.
                    pref.reload();
                    enableXHideCache = pref.getBoolean(ENABLE_X_HIDE, false);
                    isHiddenCache = pref.getBoolean(IS_HIDDEN, false);
                    hidePkgNamesCache = pref.getStringSet(HIDE_PKG_NAMES, Collections.emptySet());
                    Log.i("XPref cache refreshed. isXHideActive = " + isXHideActive(), null);
                }
        );
    }

    public static boolean isXHideActive() {
        return isHiddenCache /*&& enableXHideCache*/;
    }

    public static boolean shouldHide(String pkgName) {
        return isHiddenCache /*&& enableXHideCache*/ && hidePkgNamesCache.contains(pkgName);
    }
}
