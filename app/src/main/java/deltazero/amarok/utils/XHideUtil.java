package deltazero.amarok.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Objects;

import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.xposed.utils.XPref;

public class XHideUtil {
    private static final String TAG = "XHideUtil";

    private static SharedPreferences xpref;
    private static SharedPreferences.Editor xprefEditor;
    private static SharedPreferences.OnSharedPreferenceChangeListener hidePkgNamesChangeListener;

    public static boolean isModuleActive = false; /* Hooked by module */
    public static boolean isAvailable = false;
    public static int xposedVersion = 0; /* Hooked by module */

    @SuppressLint("WorldReadableFiles")
    public static void init(Context context) {

        if (!isModuleActive) {
            Log.i(TAG, "Xposed module not active");
            return;
        } else {
            Log.i(TAG, "Xposed module active, version = " + xposedVersion);
        }

        try {
            xpref = context.getSharedPreferences(XPref.XPREF_PATH, Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            // The new XSharedPreferences is not enabled or module's not loading
            Log.w(TAG, "Unsupported Xposed framework. Disabling XHide");
            return;
        }

        xprefEditor = xpref.edit();

        // Initialize the XPref with the current values
        xprefEditor.putStringSet(XPref.HIDE_PKG_NAMES, PrefMgr.getHideApps());
        xprefEditor.putBoolean(XPref.IS_ACTIVE, Hider.getState() == Hider.State.HIDDEN && PrefMgr.isXHideEnabled());
        xprefEditor.apply();

        // Setup listeners for changes in the preferences
        hidePkgNamesChangeListener = (sharedPreferences, key) -> {
            // Avoid listening to HIDE_PKG_NAMES changes. SharedPrefs trends to write Set<String>
            // in memory first, then write to disk asynchronously.
            if (Objects.equals(key, PrefMgr.ENABLE_X_HIDE)) {
                commitNewValues();
            }
        };
        PrefMgr.getPrefs().registerOnSharedPreferenceChangeListener(hidePkgNamesChangeListener);

        Hider.state.observeForever(state -> {
            if (state == Hider.State.PROCESSING) return;
            commitNewValues();
        });

        Log.i(TAG, "XHide initialized.");
        isAvailable = true;
    }

    private static void commitNewValues() {
        Log.d(TAG, "Committing new values to XPref");
        xprefEditor.putStringSet(XPref.HIDE_PKG_NAMES, PrefMgr.getHideApps());
        xprefEditor.putBoolean(XPref.IS_ACTIVE, Hider.getState() == Hider.State.HIDDEN && PrefMgr.isXHideEnabled());
        xprefEditor.commit();
    }
}
