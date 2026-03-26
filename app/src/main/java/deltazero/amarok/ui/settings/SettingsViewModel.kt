package deltazero.amarok.ui.settings

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.QuickHideService
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.filehider.FileHider
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.LauncherIconController
import deltazero.amarok.utils.SecurityUtil
import deltazero.amarok.utils.UpdateUtil
import deltazero.amarok.utils.XHidePrefBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun obfuscateLevelFromPrefs(): Int =
  when {
    PrefMgr.getEnableObfuscateTextFileEnhanced() -> 3
    PrefMgr.getEnableObfuscateTextFile() -> 2
    PrefMgr.getEnableObfuscateFileHeader() -> 1
    else -> 0
  }

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
  val appHiderMode: AppHiderMode = Hider.getAppHiderMode(),
  val fileHiderMode: FileHiderMode = Hider.getFileHiderMode(),
  val appHiderName: String = "",
  val fileHiderName: String = "",
  val appHiderErrorResId: Int = 0,
  val fileHiderErrorResId: Int = 0,
  val obfuscateLevel: Int = obfuscateLevelFromPrefs(),
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
          }
      )
    )
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  val hasHiddenFiles: StateFlow<Boolean> =
    Hider.folderStates
      .map { states -> states.values.any { it == Hider.FolderStatus.HIDDEN } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  init {
    val ctx = getApplication<Application>()

    viewModelScope.launch {
      Hider.appHiderMode
        .map { mode -> Pair(mode, AppHider.fromMode(ctx, mode).name) }
        .collect { (mode, name) ->
          _uiState.update { it.copy(appHiderMode = mode, appHiderName = name) }
        }
    }

    viewModelScope.launch {
      Hider.fileHiderMode
        .map { mode -> Pair(mode, FileHider.fromMode(ctx, mode).name) }
        .collect { (mode, name) ->
          _uiState.update { it.copy(fileHiderMode = mode, fileHiderName = name) }
        }
    }

    viewModelScope.launch {
      Hider.appHiderError.collect { errorResId ->
        _uiState.update { it.copy(appHiderErrorResId = errorResId) }
      }
    }

    viewModelScope.launch {
      Hider.fileHiderError.collect { errorResId ->
        _uiState.update { it.copy(fileHiderErrorResId = errorResId) }
      }
    }
  }

  fun setAppHiderMode(mode: AppHiderMode) {
    val ctx = getApplication<Application>()
    viewModelScope.launch {
      val result = AppHider.fromMode(ctx, mode).activate()
      Hider.setAppHiderMode(mode)
      Hider.setAppHiderError(if (result.success) 0 else result.msgResId)
    }
  }

  fun setFileHiderMode(mode: FileHiderMode) {
    val ctx = getApplication<Application>()
    viewModelScope.launch {
      val result = FileHider.fromMode(ctx, mode).activate()
      Hider.setFileHiderMode(mode)
      Hider.setFileHiderError(if (result.success) 0 else result.msgResId)
    }
  }

  fun setObfuscateLevel(level: Int) {
    PrefMgr.setEnableObfuscateFileHeader(level >= 1)
    PrefMgr.setEnableObfuscateTextFile(level >= 2)
    PrefMgr.setEnableObfuscateTextFileEnhanced(level >= 3)
    _uiState.update { it.copy(obfuscateLevel = level) }
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
    if (!enabled) {
      PrefMgr.setEnablePanicButton(false)
      _uiState.update { it.copy(panicButton = false) }
    }
    QuickHideService.sync(ctx)
    _uiState.update { it.copy(quickHideService = enabled) }
  }

  fun setPanicButton(enabled: Boolean) {
    val ctx = getApplication<Application>()
    PrefMgr.setEnablePanicButton(enabled)
    if (!enabled) PrefMgr.resetPanicButtonPosition()
    QuickHideService.refresh(ctx)
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
