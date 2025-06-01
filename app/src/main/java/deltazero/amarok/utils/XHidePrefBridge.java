package deltazero.amarok.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import deltazero.amarok.BuildConfig;
import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.xposed.utils.XPref;

@SuppressLint("SdCardPath")
public class XHidePrefBridge {
    private static final String TAG = "XHidePrefBridge";
    private static final String LOCAL_PREF_DIR = String.format("/data/data/%s/shared_prefs",
            BuildConfig.APPLICATION_ID);
    private static final String MAIN_PREF_FILENAME = PrefMgr.MAIN_PREF_FILENAME + ".xml";

    /**
     * @noinspection FieldCanBeLocal
     */
    private static SharedPreferences.OnSharedPreferenceChangeListener hidePkgNamesChangeListener;
    private static SharedPreferences.Editor xprefEditor;

    public static boolean isModuleActive = false; /* Hooked by module */
    public static int xposedVersion = 0; /* Hooked by module */
    public static String xPrefDir = ""; /* Hooked by module */

    public static boolean isAvailable = false;

    /**
     * Enabling the module in Xposed framework will cause lose of original preference, due to
     * the new XSharedPreferences redirection. This function will copy the preferences in Amarok's
     * local shared_prefs directory to the new XPref directory provided by the framework.
     */
    public static void migratePrefsIfNeeded(Context context) {
        var localPrefFile = new File(LOCAL_PREF_DIR, MAIN_PREF_FILENAME);
        var xPrefFile = new File(xPrefDir, MAIN_PREF_FILENAME);

        if (!isModuleActive || xPrefFile.exists() || !localPrefFile.exists())
            return;

        Log.w(TAG, String.format("Try to migrate ordinary preferences to XPref directory: " +
                "%s -> %s", LOCAL_PREF_DIR, xPrefDir));
        try {
            Files.copy(localPrefFile.toPath(), xPrefFile.toPath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to migrate preferences", e);
            return;
        }
        Log.i(TAG, "Preferences migrated successfully");
    }

    @SuppressLint("WorldReadableFiles")
    public static void init(Context context) {

        if (!isModuleActive) {
            Log.i(TAG, "Xposed module not active");
            return;
        } else {
            Log.i(TAG, "Xposed module active, version = " + xposedVersion);
        }

        SharedPreferences xPref;
        try {
            xPref = context.getSharedPreferences(XPref.XPREF_PATH, Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            // The new XSharedPreferences is not enabled or module's not loading
            Log.w(TAG, "Unsupported Xposed framework. Disabling XHide");
            return;
        }

        xprefEditor = xPref.edit();

        // Initialize the XPref with the current values
        commitNewValues();

        // Setup listeners for changes in the preferences
        hidePkgNamesChangeListener = (sharedPreferences, key) -> {
            // Avoid listening to HIDE_PKG_NAMES changes. SharedPrefs trends to write Set<String>
            // in memory first, then write to disk asynchronously.
            if (Objects.equals(key, PrefMgr.ENABLE_X_HIDE)) {
                commitNewValues();
            }
        };
        PrefMgr.getPrefs().registerOnSharedPreferenceChangeListener(hidePkgNamesChangeListener);

        Hider.state.observeForever(state -> commitNewValues());

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
