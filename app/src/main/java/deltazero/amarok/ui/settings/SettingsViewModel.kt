package deltazero.amarok.ui.settings

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import deltazero.amarok.QuickHideService
import deltazero.amarok.apphider.BaseAppHider
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.filehider.BaseFileHider
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.LauncherIconController
import deltazero.amarok.utils.SecurityUtil
import deltazero.amarok.utils.UpdateUtil
import deltazero.amarok.utils.XHidePrefBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
  // XHide
  val isXHideAvailable: Boolean = XHidePrefBridge.isAvailable,
  val xposedVersion: Int = XHidePrefBridge.xposedVersion,
  val enableXHide: Boolean = PrefMgr.isXHideEnabled(),
  val disableOnlyWithXHide: Boolean = PrefMgr.getDisableOnlyWithXHide(),
  // Privacy
  val hasPassword: Boolean = PrefMgr.getAmarokPassword() != null,
  val biometricAuth: Boolean = PrefMgr.getEnableAmarokBiometricAuth(),
  val disguise: Boolean = PrefMgr.getEnableDisguise(),
  val hideIcon: Boolean = PrefMgr.getHideAmarokIcon(),
  val hideFromRecents: Boolean = PrefMgr.getHideFromRecents(),
  val blockScreenshots: Boolean = PrefMgr.getBlockScreenshots(),
  val disableSecurityWhenUnhidden: Boolean = PrefMgr.getDisableSecurityWhenUnhidden(),
  val disableToasts: Boolean = PrefMgr.getDisableToasts(),
  // Quick Hide
  val quickHideService: Boolean = PrefMgr.getEnableQuickHideService(),
  val panicButton: Boolean = PrefMgr.getEnablePanicButton(),
  val autoHide: Boolean = PrefMgr.getEnableAutoHide(),
  val autoHideDelay: Float = PrefMgr.getAutoHideDelay().toFloat(),
  // Appearance
  val dynamicColor: Boolean = PrefMgr.getEnableDynamicColor(),
  val darkThemeMode: Int = PrefMgr.getDarkTheme(),
  val invertTileColor: Boolean = PrefMgr.getInvertTileColor(),
  // Workmode
  val appHiderName: String = "",
  val fileHiderName: String = "",
  // Update
  val updateChannel: String = PrefMgr.getUpdateChannel().name,
  val autoUpdate: Boolean = PrefMgr.getEnableAutoUpdate(),
  val appVersionName: String = "",
  // About
  val analyticsEnabled: Boolean = AppCenterUtil.isAnalyticsEnabled(),
  val analyticsAvailable: Boolean = AppCenterUtil.isAvailable(),
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
  private val _uiState =
    MutableStateFlow(
      SettingsUiState(
        appVersionName =
          try {
            application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "?"
          } catch (_: Exception) {
            "?"
          },
        appHiderName = BaseAppHider.fromMode(application, PrefMgr.getAppHiderMode()).name,
        fileHiderName = BaseFileHider.fromMode(application, PrefMgr.getFileHiderMode()).name,
      )
    )
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  fun refreshHiderNames() {
    val ctx = getApplication<Application>()
    _uiState.update {
      it.copy(
        appHiderName = BaseAppHider.fromMode(ctx, PrefMgr.getAppHiderMode()).name,
        fileHiderName = BaseFileHider.fromMode(ctx, PrefMgr.getFileHiderMode()).name,
      )
    }
  }

  // XHide
  fun setXHideEnabled(enabled: Boolean) {
    PrefMgr.setXHideEnabled(enabled)
    _uiState.update { it.copy(enableXHide = enabled) }
  }

  fun setDisableOnlyWithXHide(enabled: Boolean) {
    PrefMgr.setDisableOnlyWithXHide(enabled)
    _uiState.update { it.copy(disableOnlyWithXHide = enabled) }
  }

  // Privacy
  fun setPassword(hash: String?) {
    PrefMgr.setAmarokPassword(hash)
    if (hash != null) SecurityUtil.unlock()
    _uiState.update {
      it.copy(
        hasPassword = PrefMgr.getAmarokPassword() != null,
        biometricAuth = PrefMgr.getEnableAmarokBiometricAuth(),
      )
    }
  }

  fun setBiometricAuth(enabled: Boolean) {
    PrefMgr.setEnableAmarokBiometricAuth(enabled)
    _uiState.update { it.copy(biometricAuth = enabled) }
  }

  fun setDisguise(enabled: Boolean, activity: Activity?) {
    PrefMgr.setEnableDisguise(enabled)
    PrefMgr.setDoShowQuitDisguiseInstuct(true)
    if (enabled) SecurityUtil.lockAndDisguise()
    activity?.let {
      LauncherIconController.setIconState(
        it,
        if (enabled) LauncherIconController.IconState.DISGUISED
        else LauncherIconController.IconState.VISIBLE,
      )
    }
    _uiState.update { it.copy(disguise = enabled) }
  }

  fun confirmHideIcon(activity: Activity?) {
    PrefMgr.setEnableDisguise(false)
    activity?.let {
      LauncherIconController.setIconState(it, LauncherIconController.IconState.HIDDEN)
    }
    PrefMgr.setHideAmarokIcon(true)
    _uiState.update { it.copy(hideIcon = true, disguise = false) }
  }

  fun unhideIcon(activity: Activity?) {
    PrefMgr.setHideAmarokIcon(false)
    activity?.let {
      LauncherIconController.setIconState(it, LauncherIconController.IconState.VISIBLE)
    }
    _uiState.update { it.copy(hideIcon = false) }
  }

  fun setHideFromRecents(enabled: Boolean) {
    PrefMgr.setHideFromRecents(enabled)
    _uiState.update { it.copy(hideFromRecents = enabled) }
  }

  fun setBlockScreenshots(enabled: Boolean) {
    PrefMgr.setBlockScreenshots(enabled)
    _uiState.update { it.copy(blockScreenshots = enabled) }
  }

  fun setDisableSecurityWhenUnhidden(enabled: Boolean) {
    PrefMgr.setDisableSecurityWhenUnhidden(enabled)
    _uiState.update { it.copy(disableSecurityWhenUnhidden = enabled) }
  }

  fun setDisableToasts(enabled: Boolean) {
    PrefMgr.setDisableToasts(enabled)
    _uiState.update { it.copy(disableToasts = enabled) }
  }

  // Quick Hide
  fun setQuickHideService(enabled: Boolean) {
    val ctx = getApplication<Application>()
    PrefMgr.setEnableQuickHideService(enabled)
    if (enabled) {
      QuickHideService.startService(ctx)
    } else {
      PrefMgr.setEnablePanicButton(false)
      QuickHideService.stopService(ctx)
      _uiState.update { it.copy(panicButton = false) }
    }
    _uiState.update { it.copy(quickHideService = enabled) }
  }

  fun setPanicButton(enabled: Boolean) {
    val ctx = getApplication<Application>()
    PrefMgr.setEnablePanicButton(enabled)
    if (!enabled) PrefMgr.resetPanicButtonPosition()
    QuickHideService.stopService(ctx)
    QuickHideService.startService(ctx)
    _uiState.update { it.copy(panicButton = enabled) }
  }

  fun setAutoHide(enabled: Boolean) {
    PrefMgr.setEnableAutoHide(enabled)
    _uiState.update { it.copy(autoHide = enabled) }
  }

  fun setAutoHideDelay(delay: Float) {
    PrefMgr.setAutoHideDelay(delay.toInt())
    _uiState.update { it.copy(autoHideDelay = delay) }
  }

  // Appearance
  fun setDynamicColor(enabled: Boolean) {
    PrefMgr.setEnableDynamicColor(enabled)
    _uiState.update { it.copy(dynamicColor = enabled) }
  }

  fun setDarkTheme(mode: Int) {
    PrefMgr.setDarkTheme(mode)
    AppCompatDelegate.setDefaultNightMode(mode)
    _uiState.update { it.copy(darkThemeMode = mode) }
  }

  fun setInvertTileColor(enabled: Boolean) {
    PrefMgr.setInvertTileColor(enabled)
    _uiState.update { it.copy(invertTileColor = enabled) }
  }

  // Update
  fun setUpdateChannel(channel: UpdateUtil.UpdateChannel) {
    PrefMgr.setUpdateChannel(channel)
    _uiState.update { it.copy(updateChannel = channel.name) }
  }

  fun setAutoUpdate(enabled: Boolean) {
    PrefMgr.setEnableAutoUpdate(enabled)
    _uiState.update { it.copy(autoUpdate = enabled) }
  }

  // About
  fun setAnalyticsEnabled(enabled: Boolean) {
    AppCenterUtil.setAnalyticsEnabled(enabled)
    _uiState.update { it.copy(analyticsEnabled = enabled) }
  }

  fun forceUnhide() {
    Hider.forceUnhide(getApplication())
  }
}
