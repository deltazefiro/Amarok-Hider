package deltazero.amarok.core

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.utils.UpdateUtil
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Snapshot of all persisted settings, with sensible defaults. Used for synchronous reads via
 * [SettingsRepository.settings].value.
 */
data class SettingsSnapshot(
  val appHiderMode: AppHiderMode = AppHiderMode.NONE,
  val fileHiderMode: FileHiderMode = FileHiderMode.NONE,
  val obfuscateFileHeader: Boolean = false,
  val obfuscateTextFile: Boolean = false,
  val obfuscateTextFileEnhanced: Boolean = false,
  val xHideEnabled: Boolean = false,
  val disableOnlyWithXHide: Boolean = false,
  val password: String? = null,
  val biometricAuth: Boolean = false,
  val lockTrigger: LockTrigger = LockTrigger.SCREEN_OFF,
  val disguise: Boolean = false,
  val hideAmarokIcon: Boolean = false,
  val doShowQuitDisguiseInstruct: Boolean = true,
  val showWelcome: Boolean = true,
  val hideFromRecents: Boolean = false,
  val blockScreenshots: Boolean = false,
  val disableSecurityWhenUnhidden: Boolean = false,
  val disableToasts: Boolean = false,
  val quickHideService: Boolean = false,
  val panicButton: Boolean = false,
  val panicButtonColor: Int = 0xFFD1D1D1.toInt(),
  val panicButtonY: Int = 300,
  val panicButtonLeftEdge: Boolean = false,
  val autoHide: Boolean = false,
  val autoHideDelay: Int = 0,
  val dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
  val darkTheme: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
  val invertTileColor: Boolean = false,
  val updateChannel: UpdateUtil.UpdateChannel = UpdateUtil.UpdateChannel.RELEASE,
  val autoUpdate: Boolean = true,
) {
  val obfuscateLevel: Int
    get() =
      when {
        obfuscateTextFileEnhanced -> 3
        obfuscateTextFile -> 2
        obfuscateFileHeader -> 1
        else -> 0
      }
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {

  private val dataStore = context.applicationContext.settingsDataStore
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val settings: StateFlow<SettingsSnapshot> =
    dataStore.data
      .map { prefs -> prefs.toSnapshot() }
      .stateIn(scope, SharingStarted.Eagerly, SettingsSnapshot())

  // Called once at startup to ensure disk data is loaded before synchronous callers use
  // settings.value. Without this, callers would briefly see defaults.
  suspend fun awaitLoaded() {
    dataStore.data.first()
  }

  // Setters

  suspend fun setAppHiderMode(mode: AppHiderMode) =
    dataStore.edit { it[Keys.APP_HIDER_MODE] = mode.key }

  suspend fun setFileHiderMode(mode: FileHiderMode) =
    dataStore.edit { it[Keys.FILE_HIDER_MODE] = mode.key }

  suspend fun setObfuscateLevel(level: Int) =
    dataStore.edit {
      it[Keys.OBFUSCATE_FILE_HEADER] = level >= 1
      it[Keys.OBFUSCATE_TEXT_FILE] = level >= 2
      it[Keys.OBFUSCATE_TEXT_FILE_ENHANCED] = level >= 3
    }

  suspend fun setXHideEnabled(enabled: Boolean) =
    dataStore.edit { it[Keys.X_HIDE_ENABLED] = enabled }

  suspend fun setDisableOnlyWithXHide(enabled: Boolean) =
    dataStore.edit { it[Keys.DISABLE_ONLY_WITH_X_HIDE] = enabled }

  suspend fun setPassword(hash: String?) =
    dataStore.edit { if (hash != null) it[Keys.PASSWORD] = hash else it.remove(Keys.PASSWORD) }

  suspend fun setBiometricAuth(enabled: Boolean) =
    dataStore.edit { it[Keys.BIOMETRIC_AUTH] = enabled }

  suspend fun setLockTrigger(trigger: LockTrigger) =
    dataStore.edit { it[Keys.LOCK_TRIGGER] = trigger.key }

  suspend fun setDisguise(enabled: Boolean) = dataStore.edit { it[Keys.DISGUISE] = enabled }

  suspend fun setDoShowQuitDisguiseInstruct(show: Boolean) =
    dataStore.edit { it[Keys.DO_SHOW_QUIT_DISGUISE_INSTRUCT] = show }

  suspend fun setShowWelcome(show: Boolean) = dataStore.edit { it[Keys.SHOW_WELCOME] = show }

  suspend fun setHideAmarokIcon(hide: Boolean) = dataStore.edit { it[Keys.HIDE_AMAROK_ICON] = hide }

  suspend fun setHideFromRecents(enabled: Boolean) =
    dataStore.edit { it[Keys.HIDE_FROM_RECENTS] = enabled }

  suspend fun setBlockScreenshots(enabled: Boolean) =
    dataStore.edit { it[Keys.BLOCK_SCREENSHOTS] = enabled }

  suspend fun setDisableSecurityWhenUnhidden(enabled: Boolean) =
    dataStore.edit { it[Keys.DISABLE_SECURITY_WHEN_UNHIDDEN] = enabled }

  suspend fun setDisableToasts(enabled: Boolean) =
    dataStore.edit { it[Keys.DISABLE_TOASTS] = enabled }

  suspend fun setQuickHideService(enabled: Boolean) =
    dataStore.edit { it[Keys.QUICK_HIDE_SERVICE] = enabled }

  suspend fun setPanicButton(enabled: Boolean) = dataStore.edit { it[Keys.PANIC_BUTTON] = enabled }

  suspend fun setPanicButtonColor(color: Int) =
    dataStore.edit { it[Keys.PANIC_BUTTON_COLOR] = color }

  suspend fun setPanicButtonPosition(y: Int, leftEdge: Boolean) =
    dataStore.edit {
      it[Keys.PANIC_BUTTON_Y] = y
      it[Keys.PANIC_BUTTON_LEFT_EDGE] = leftEdge
    }

  suspend fun resetPanicButtonPosition() =
    dataStore.edit {
      it.remove(Keys.PANIC_BUTTON_Y)
      it.remove(Keys.PANIC_BUTTON_LEFT_EDGE)
    }

  suspend fun setAutoHide(enabled: Boolean) = dataStore.edit { it[Keys.AUTO_HIDE] = enabled }

  suspend fun setAutoHideDelay(delay: Int) = dataStore.edit { it[Keys.AUTO_HIDE_DELAY] = delay }

  suspend fun setDynamicColor(enabled: Boolean) =
    dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

  suspend fun setDarkTheme(mode: Int) = dataStore.edit { it[Keys.DARK_THEME] = mode }

  suspend fun setInvertTileColor(enabled: Boolean) =
    dataStore.edit { it[Keys.INVERT_TILE_COLOR] = enabled }

  suspend fun setUpdateChannel(channel: UpdateUtil.UpdateChannel) =
    dataStore.edit { it[Keys.UPDATE_CHANNEL] = channel.name }

  suspend fun setAutoUpdate(enabled: Boolean) = dataStore.edit { it[Keys.AUTO_UPDATE] = enabled }

  // Preference keys — names kept compatible with legacy SharedPreferences for easy cross-reference

  private object Keys {
    val APP_HIDER_MODE = stringPreferencesKey("appHiderMode")
    val FILE_HIDER_MODE = stringPreferencesKey("fileHiderMode")
    val OBFUSCATE_FILE_HEADER = booleanPreferencesKey("enableObfuscateFileHeader")
    val OBFUSCATE_TEXT_FILE = booleanPreferencesKey("enableObfuscateTextFile")
    val OBFUSCATE_TEXT_FILE_ENHANCED = booleanPreferencesKey("enableObfuscateTextFileEnhanced")
    val X_HIDE_ENABLED = booleanPreferencesKey("enableXHide")
    val DISABLE_ONLY_WITH_X_HIDE = booleanPreferencesKey("disableOnlyWithXHide")
    val PASSWORD = stringPreferencesKey("amarokPassword")
    val BIOMETRIC_AUTH = booleanPreferencesKey("enableAmarokBiometricAuth")
    val LOCK_TRIGGER = stringPreferencesKey("lockTrigger")
    val DISGUISE = booleanPreferencesKey("enableDisguise")
    val HIDE_AMAROK_ICON = booleanPreferencesKey("hideAmarokIcon")
    val DO_SHOW_QUIT_DISGUISE_INSTRUCT = booleanPreferencesKey("doShowQuitDisguiseInstuct")
    val SHOW_WELCOME = booleanPreferencesKey("showWelcome")
    val HIDE_FROM_RECENTS = booleanPreferencesKey("hideFromRecents")
    val BLOCK_SCREENSHOTS = booleanPreferencesKey("blockScreenshots")
    val DISABLE_SECURITY_WHEN_UNHIDDEN = booleanPreferencesKey("disableSecurityWhenUnhidden")
    val DISABLE_TOASTS = booleanPreferencesKey("disableToasts")
    val QUICK_HIDE_SERVICE = booleanPreferencesKey("enableQuickHideService")
    val PANIC_BUTTON = booleanPreferencesKey("enablePanicButton")
    val PANIC_BUTTON_COLOR = intPreferencesKey("panicButtonColor")
    val PANIC_BUTTON_Y = intPreferencesKey("panicButtonY")
    val PANIC_BUTTON_LEFT_EDGE = booleanPreferencesKey("panicButtonLeftEdge")
    val AUTO_HIDE = booleanPreferencesKey("enableAutoHide")
    val AUTO_HIDE_DELAY = intPreferencesKey("autoHideDelay")
    val DYNAMIC_COLOR = booleanPreferencesKey("enableDynamicColor")
    val DARK_THEME = intPreferencesKey("darkTheme")
    val INVERT_TILE_COLOR = booleanPreferencesKey("invertTileColor")
    val UPDATE_CHANNEL = stringPreferencesKey("updateChannel")
    val AUTO_UPDATE = booleanPreferencesKey("isEnableAutoUpdate")
  }

  private fun Preferences.toSnapshot() =
    SettingsSnapshot(
      appHiderMode = AppHiderMode.fromKey(this[Keys.APP_HIDER_MODE] ?: "none"),
      fileHiderMode = FileHiderMode.fromKey(this[Keys.FILE_HIDER_MODE] ?: "none"),
      obfuscateFileHeader = this[Keys.OBFUSCATE_FILE_HEADER] ?: false,
      obfuscateTextFile = this[Keys.OBFUSCATE_TEXT_FILE] ?: false,
      obfuscateTextFileEnhanced = this[Keys.OBFUSCATE_TEXT_FILE_ENHANCED] ?: false,
      xHideEnabled = this[Keys.X_HIDE_ENABLED] ?: false,
      disableOnlyWithXHide = this[Keys.DISABLE_ONLY_WITH_X_HIDE] ?: false,
      password = this[Keys.PASSWORD],
      biometricAuth = this[Keys.BIOMETRIC_AUTH] ?: false,
      lockTrigger = LockTrigger.fromKey(this[Keys.LOCK_TRIGGER] ?: LockTrigger.SCREEN_OFF.key),
      disguise = this[Keys.DISGUISE] ?: false,
      hideAmarokIcon = this[Keys.HIDE_AMAROK_ICON] ?: false,
      doShowQuitDisguiseInstruct = this[Keys.DO_SHOW_QUIT_DISGUISE_INSTRUCT] ?: true,
      showWelcome = this[Keys.SHOW_WELCOME] ?: true,
      hideFromRecents = this[Keys.HIDE_FROM_RECENTS] ?: false,
      blockScreenshots = this[Keys.BLOCK_SCREENSHOTS] ?: false,
      disableSecurityWhenUnhidden = this[Keys.DISABLE_SECURITY_WHEN_UNHIDDEN] ?: false,
      disableToasts = this[Keys.DISABLE_TOASTS] ?: false,
      quickHideService = this[Keys.QUICK_HIDE_SERVICE] ?: false,
      panicButton = this[Keys.PANIC_BUTTON] ?: false,
      panicButtonColor = this[Keys.PANIC_BUTTON_COLOR] ?: 0xFFD1D1D1.toInt(),
      panicButtonY = this[Keys.PANIC_BUTTON_Y] ?: 300,
      panicButtonLeftEdge = this[Keys.PANIC_BUTTON_LEFT_EDGE] ?: false,
      autoHide = this[Keys.AUTO_HIDE] ?: false,
      autoHideDelay = this[Keys.AUTO_HIDE_DELAY] ?: 0,
      dynamicColor =
        this[Keys.DYNAMIC_COLOR] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU),
      darkTheme = this[Keys.DARK_THEME] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
      invertTileColor = this[Keys.INVERT_TILE_COLOR] ?: false,
      updateChannel =
        UpdateUtil.UpdateChannel.fromString(
          this[Keys.UPDATE_CHANNEL] ?: UpdateUtil.UpdateChannel.RELEASE.name
        ),
      autoUpdate = this[Keys.AUTO_UPDATE] ?: true,
    )
}
