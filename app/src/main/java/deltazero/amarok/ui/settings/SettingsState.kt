package deltazero.amarok.ui.settings

import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.XHidePrefBridge

private fun obfuscateLevelFromPrefs(): Int =
  when {
    PrefMgr.getEnableObfuscateTextFileEnhanced() -> 3
    PrefMgr.getEnableObfuscateTextFile() -> 2
    PrefMgr.getEnableObfuscateFileHeader() -> 1
    else -> 0
  }

data class WorkmodeSettingsState(
  val appHiderMode: AppHiderMode = Hider.appHiderMode.value,
  val fileHiderMode: FileHiderMode = Hider.fileHiderMode.value,
  val appHiderName: String = "",
  val fileHiderName: String = "",
  val appHiderErrorResId: Int = 0,
  val fileHiderErrorResId: Int = 0,
  val obfuscateLevel: Int = obfuscateLevelFromPrefs(),
)

data class XHideSettingsState(
  val isAvailable: Boolean = XHidePrefBridge.isAvailable,
  val xposedVersion: Int = XHidePrefBridge.xposedVersion,
  val enabled: Boolean = PrefMgr.isXHideEnabled(),
  val disableOnlyWithXHide: Boolean = PrefMgr.getDisableOnlyWithXHide(),
)

data class PrivacySettingsState(
  val hasPassword: Boolean = PrefMgr.getAmarokPassword() != null,
  val biometricAuth: Boolean = PrefMgr.getEnableAmarokBiometricAuth(),
  val disguise: Boolean = PrefMgr.getEnableDisguise(),
  val hideIcon: Boolean = PrefMgr.getHideAmarokIcon(),
  val hideFromRecents: Boolean = PrefMgr.getHideFromRecents(),
  val blockScreenshots: Boolean = PrefMgr.getBlockScreenshots(),
  val disableSecurityWhenUnhidden: Boolean = PrefMgr.getDisableSecurityWhenUnhidden(),
  val disableToasts: Boolean = PrefMgr.getDisableToasts(),
)

data class QuickHideSettingsState(
  val quickHideService: Boolean = PrefMgr.getEnableQuickHideService(),
  val panicButton: Boolean = PrefMgr.getEnablePanicButton(),
  val autoHide: Boolean = PrefMgr.getEnableAutoHide(),
  val autoHideDelay: Float = PrefMgr.getAutoHideDelay().toFloat(),
)

data class AppearanceSettingsState(
  val dynamicColor: Boolean = PrefMgr.getEnableDynamicColor(),
  val darkThemeMode: Int = PrefMgr.getDarkTheme(),
  val invertTileColor: Boolean = PrefMgr.getInvertTileColor(),
)

data class UpdateSettingsState(
  val updateChannel: String = PrefMgr.getUpdateChannel().name,
  val autoUpdate: Boolean = PrefMgr.getEnableAutoUpdate(),
  val appVersionName: String = "",
)

data class AboutSettingsState(
  val analyticsEnabled: Boolean = AppCenterUtil.isAnalyticsEnabled(),
  val analyticsAvailable: Boolean = AppCenterUtil.isAvailable(),
)

data class SettingsUiState(
  val workmode: WorkmodeSettingsState = WorkmodeSettingsState(),
  val xHide: XHideSettingsState = XHideSettingsState(),
  val privacy: PrivacySettingsState = PrivacySettingsState(),
  val quickHide: QuickHideSettingsState = QuickHideSettingsState(),
  val appearance: AppearanceSettingsState = AppearanceSettingsState(),
  val updates: UpdateSettingsState = UpdateSettingsState(),
  val about: AboutSettingsState = AboutSettingsState(),
)
