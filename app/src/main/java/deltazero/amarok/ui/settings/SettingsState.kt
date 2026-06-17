package deltazero.amarok.ui.settings

import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.core.LockTrigger
import deltazero.amarok.core.SettingsSnapshot
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.XHideModuleBridge
import deltazero.amarok.utils.XHideStatus

data class WorkmodeSettingsState(
  val appHiderMode: AppHiderMode = AppHiderMode.NONE,
  val fileHiderMode: FileHiderMode = FileHiderMode.NONE,
  val appHiderName: String = "",
  val fileHiderName: String = "",
  val appHiderErrorResId: Int = 0,
  val fileHiderErrorResId: Int = 0,
  val obfuscateLevel: Int = 0,
)

data class XHideSettingsState(
  val status: XHideStatus = XHideStatus.NotInstalled,
  val enabled: Boolean = false,
  val disableOnlyWithXHide: Boolean = false,
) {
  val isActive: Boolean
    get() = status is XHideStatus.Active

  constructor(
    settings: SettingsSnapshot,
    status: XHideStatus = XHideModuleBridge.status.value,
  ) : this(
    status = status,
    enabled = settings.xHideEnabled,
    disableOnlyWithXHide = settings.disableOnlyWithXHide,
  )
}

data class PrivacySettingsState(
  val hasPassword: Boolean = false,
  val biometricAuth: Boolean = false,
  val lockTrigger: LockTrigger = LockTrigger.SCREEN_OFF,
  val disguise: Boolean = false,
  val hideIcon: Boolean = false,
  val hideFromRecents: Boolean = false,
  val blockScreenshots: Boolean = false,
  val disableSecurityWhenUnhidden: Boolean = false,
) {
  constructor(
    settings: SettingsSnapshot
  ) : this(
    hasPassword = settings.password != null,
    biometricAuth = settings.biometricAuth,
    lockTrigger = settings.lockTrigger,
    disguise = settings.disguise,
    hideIcon = settings.hideAmarokIcon,
    hideFromRecents = settings.hideFromRecents,
    blockScreenshots = settings.blockScreenshots,
    disableSecurityWhenUnhidden = settings.disableSecurityWhenUnhidden,
  )
}

data class QuickHideSettingsState(
  val quickHideService: Boolean = false,
  val panicButton: Boolean = false,
  val autoHide: Boolean = false,
  val autoHideDelay: Float = 0f,
) {
  constructor(
    settings: SettingsSnapshot
  ) : this(
    quickHideService = settings.quickHideService,
    panicButton = settings.panicButton,
    autoHide = settings.autoHide,
    autoHideDelay = settings.autoHideDelay.toFloat(),
  )
}

data class AppearanceSettingsState(
  val dynamicColor: Boolean = false,
  val darkThemeMode: Int = -1,
  val invertTileColor: Boolean = false,
  val disableToasts: Boolean = false,
) {
  constructor(
    settings: SettingsSnapshot
  ) : this(
    dynamicColor = settings.dynamicColor,
    darkThemeMode = settings.darkTheme,
    invertTileColor = settings.invertTileColor,
    disableToasts = settings.disableToasts,
  )
}

data class UpdateSettingsState(
  val updateChannel: String = "",
  val autoUpdate: Boolean = true,
  val appVersionName: String = "",
) {
  constructor(
    settings: SettingsSnapshot,
    appVersionName: String,
  ) : this(
    updateChannel = settings.updateChannel.name,
    autoUpdate = settings.autoUpdate,
    appVersionName = appVersionName,
  )
}

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
