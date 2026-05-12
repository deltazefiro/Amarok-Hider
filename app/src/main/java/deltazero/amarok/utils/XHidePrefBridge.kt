package deltazero.amarok.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import deltazero.amarok.AmarokApplication
import deltazero.amarok.BuildConfig
import deltazero.amarok.core.HiderStateRepository
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.xposed.utils.XPref
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@SuppressLint("SdCardPath")
object XHidePrefBridge {

  private const val TAG = "XHidePrefBridge"
  private val LOCAL_PREF_DIR = "/data/data/${BuildConfig.APPLICATION_ID}/shared_prefs"
  private const val MAIN_PREF_FILENAME = "deltazero.amarok.prefs.xml"

  private var xprefEditor: SharedPreferences.Editor? = null
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  @JvmField var isModuleActive = false /* Hooked by module */
  @JvmField var xposedVersion = 0 /* Hooked by module */
  @JvmField var xPrefDir = "" /* Hooked by module */

  @JvmField var isAvailable = false

  /**
   * Enabling the module in Xposed framework will cause loss of original preferences, due to the new
   * XSharedPreferences redirection. This function copies the preferences in Amarok's local
   * shared_prefs directory to the new XPref directory provided by the framework.
   */
  @JvmStatic
  fun migratePrefsIfNeeded(context: Context) {
    val localPrefFile = File(LOCAL_PREF_DIR, MAIN_PREF_FILENAME)
    val xPrefFile = File(xPrefDir, MAIN_PREF_FILENAME)

    if (!isModuleActive || xPrefFile.exists() || !localPrefFile.exists()) return

    Log.w(
      TAG,
      "Try to migrate ordinary preferences to XPref directory: $LOCAL_PREF_DIR -> $xPrefDir",
    )
    try {
      Files.copy(localPrefFile.toPath(), xPrefFile.toPath())
    } catch (e: IOException) {
      Log.e(TAG, "Failed to migrate preferences", e)
      return
    }
    Log.i(TAG, "Preferences migrated successfully")
  }

  @SuppressLint("WorldReadableFiles")
  @JvmStatic
  fun init(context: Context) {
    if (!isModuleActive) {
      Log.i(TAG, "Xposed module not active")
      return
    } else {
      Log.i(TAG, "Xposed module active, version = $xposedVersion")
    }
    if (isAvailable) return

    @Suppress("DEPRECATION")
    // Xposed reads this preferences file from outside the app process.
    val xPref: SharedPreferences =
      try {
        context.getSharedPreferences(XPref.XPREF_PATH, Context.MODE_WORLD_READABLE)
      } catch (_: SecurityException) {
        // The new XSharedPreferences is not enabled or module's not loading
        Log.w(TAG, "Unsupported Xposed framework. Disabling XHide")
        return
      }

    xprefEditor = xPref.edit()

    val app = context.applicationContext as AmarokApplication
    val settingsRepo: SettingsRepository = app.settingsRepo
    val hiderStateRepo: HiderStateRepository = app.hiderStateRepo

    // Keep XPref synchronized with the DataStore-backed source of truth.
    scope.launch {
      combine(
          hiderStateRepo.hiddenApps,
          settingsRepo.settings.map { it.xHideEnabled }.distinctUntilChanged(),
        ) { hiddenApps, xHideEnabled ->
          hiddenApps to xHideEnabled
        }
        .distinctUntilChanged()
        .collect { (hiddenApps, xHideEnabled) -> commitNewValues(hiddenApps, xHideEnabled) }
    }

    Log.i(TAG, "XHide initialized.")
    isAvailable = true
  }

  private fun commitNewValues(hiddenApps: Set<String>, xHideEnabled: Boolean) {
    Log.d(TAG, "Committing new values to XPref")
    val editor = xprefEditor ?: return
    editor.putStringSet(XPref.HIDE_PKG_NAMES, hiddenApps)
    editor.putBoolean(XPref.IS_ACTIVE, xHideEnabled && hiddenApps.isNotEmpty())
    editor.commit()
  }
}
