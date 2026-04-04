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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun packageVersionName(application: Application): String =
  try {
    application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "?"
  } catch (_: Exception) {
    "?"
  }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
  private val _uiState =
    MutableStateFlow(
      SettingsUiState(
        updates = UpdateSettingsState(appVersionName = packageVersionName(application))
      )
    )
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  val hasHiddenFiles: StateFlow<Boolean> =
    Hider.folderStates
      .map { states -> states.values.any { it == Hider.State.HIDDEN } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  init {
    val ctx = getApplication<Application>()

    viewModelScope.launch {
      Hider.appHiderMode
        .map { mode -> mode to AppHider.build(ctx, mode).name }
        .collect { (mode, name) ->
          updateWorkmode { it.copy(appHiderMode = mode, appHiderName = name) }
        }
    }

    viewModelScope.launch {
      Hider.fileHiderMode
        .map { mode -> mode to FileHider.build(ctx, mode).name }
        .collect { (mode, name) ->
          updateWorkmode { it.copy(fileHiderMode = mode, fileHiderName = name) }
        }
    }

    viewModelScope.launch {
      Hider.appHiderError.collect { errorResId ->
        updateWorkmode { it.copy(appHiderErrorResId = errorResId) }
      }
    }

    viewModelScope.launch {
      Hider.fileHiderError.collect { errorResId ->
        updateWorkmode { it.copy(fileHiderErrorResId = errorResId) }
      }
    }
  }

  fun setAppHiderMode(mode: AppHiderMode) {
    val ctx = getApplication<Application>()
    viewModelScope.launch { Hider.switchAppHider(ctx, mode) }
  }

  fun setFileHiderMode(mode: FileHiderMode) {
    val ctx = getApplication<Application>()
    viewModelScope.launch { Hider.switchFileHider(ctx, mode) }
  }

  fun setObfuscateLevel(level: Int) {
    PrefMgr.setEnableObfuscateFileHeader(level >= 1)
    PrefMgr.setEnableObfuscateTextFile(level >= 2)
    PrefMgr.setEnableObfuscateTextFileEnhanced(level >= 3)
    updateWorkmode { it.copy(obfuscateLevel = level) }
  }

  fun setXHideEnabled(enabled: Boolean) {
    PrefMgr.setXHideEnabled(enabled)
    updateXHide { it.copy(enabled = enabled) }
  }

  fun setDisableOnlyWithXHide(enabled: Boolean) {
    PrefMgr.setDisableOnlyWithXHide(enabled)
    updateXHide { it.copy(disableOnlyWithXHide = enabled) }
  }

  fun setPassword(hash: String?) {
    PrefMgr.setAmarokPassword(hash)
    if (hash != null) SecurityUtil.unlock()
    updatePrivacy {
      it.copy(
        hasPassword = PrefMgr.getAmarokPassword() != null,
        biometricAuth = PrefMgr.getEnableAmarokBiometricAuth(),
      )
    }
  }

  fun setBiometricAuth(enabled: Boolean) {
    PrefMgr.setEnableAmarokBiometricAuth(enabled)
    updatePrivacy { it.copy(biometricAuth = enabled) }
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
    updatePrivacy { it.copy(disguise = enabled) }
  }

  fun confirmHideIcon(activity: Activity?) {
    PrefMgr.setEnableDisguise(false)
    activity?.let {
      LauncherIconController.setIconState(it, LauncherIconController.IconState.HIDDEN)
    }
    PrefMgr.setHideAmarokIcon(true)
    updatePrivacy { it.copy(hideIcon = true, disguise = false) }
  }

  fun unhideIcon(activity: Activity?) {
    PrefMgr.setHideAmarokIcon(false)
    activity?.let {
      LauncherIconController.setIconState(it, LauncherIconController.IconState.VISIBLE)
    }
    updatePrivacy { it.copy(hideIcon = false) }
  }

  fun setHideFromRecents(enabled: Boolean) {
    PrefMgr.setHideFromRecents(enabled)
    updatePrivacy { it.copy(hideFromRecents = enabled) }
  }

  fun setBlockScreenshots(enabled: Boolean) {
    PrefMgr.setBlockScreenshots(enabled)
    updatePrivacy { it.copy(blockScreenshots = enabled) }
  }

  fun setDisableSecurityWhenUnhidden(enabled: Boolean) {
    PrefMgr.setDisableSecurityWhenUnhidden(enabled)
    updatePrivacy { it.copy(disableSecurityWhenUnhidden = enabled) }
  }

  fun setDisableToasts(enabled: Boolean) {
    PrefMgr.setDisableToasts(enabled)
    updatePrivacy { it.copy(disableToasts = enabled) }
  }

  fun setQuickHideService(enabled: Boolean) {
    val ctx = getApplication<Application>()
    PrefMgr.setEnableQuickHideService(enabled)
    if (!enabled) {
      PrefMgr.setEnablePanicButton(false)
    }
    QuickHideService.sync(ctx)
    updateQuickHide {
      it.copy(quickHideService = enabled, panicButton = if (enabled) it.panicButton else false)
    }
  }

  fun setPanicButton(enabled: Boolean) {
    val ctx = getApplication<Application>()
    PrefMgr.setEnablePanicButton(enabled)
    if (!enabled) PrefMgr.resetPanicButtonPosition()
    QuickHideService.refresh(ctx)
    updateQuickHide { it.copy(panicButton = enabled) }
  }

  fun setAutoHide(enabled: Boolean) {
    PrefMgr.setEnableAutoHide(enabled)
    updateQuickHide { it.copy(autoHide = enabled) }
  }

  fun setAutoHideDelay(delay: Float) {
    PrefMgr.setAutoHideDelay(delay.toInt())
    updateQuickHide { it.copy(autoHideDelay = delay) }
  }

  fun setDynamicColor(enabled: Boolean) {
    PrefMgr.setEnableDynamicColor(enabled)
    updateAppearance { it.copy(dynamicColor = enabled) }
  }

  fun setDarkTheme(mode: Int) {
    PrefMgr.setDarkTheme(mode)
    AppCompatDelegate.setDefaultNightMode(mode)
    updateAppearance { it.copy(darkThemeMode = mode) }
  }

  fun setInvertTileColor(enabled: Boolean) {
    PrefMgr.setInvertTileColor(enabled)
    updateAppearance { it.copy(invertTileColor = enabled) }
  }

  fun setUpdateChannel(channel: UpdateUtil.UpdateChannel) {
    PrefMgr.setUpdateChannel(channel)
    updateUpdates { it.copy(updateChannel = channel.name) }
  }

  fun setAutoUpdate(enabled: Boolean) {
    PrefMgr.setEnableAutoUpdate(enabled)
    updateUpdates { it.copy(autoUpdate = enabled) }
  }

  fun setAnalyticsEnabled(enabled: Boolean) {
    AppCenterUtil.setAnalyticsEnabled(enabled)
    updateAbout { it.copy(analyticsEnabled = enabled) }
  }

  fun forceUnhide() {
    val ctx = getApplication<Application>()
    Hider.cancelProcess()
    Hider.processAll(ctx, Hider.Action.UNHIDE)
  }

  private fun updateWorkmode(transform: (WorkmodeSettingsState) -> WorkmodeSettingsState) {
    _uiState.update { it.copy(workmode = transform(it.workmode)) }
  }

  private fun updateXHide(transform: (XHideSettingsState) -> XHideSettingsState) {
    _uiState.update { it.copy(xHide = transform(it.xHide)) }
  }

  private fun updatePrivacy(transform: (PrivacySettingsState) -> PrivacySettingsState) {
    _uiState.update { it.copy(privacy = transform(it.privacy)) }
  }

  private fun updateQuickHide(transform: (QuickHideSettingsState) -> QuickHideSettingsState) {
    _uiState.update { it.copy(quickHide = transform(it.quickHide)) }
  }

  private fun updateAppearance(transform: (AppearanceSettingsState) -> AppearanceSettingsState) {
    _uiState.update { it.copy(appearance = transform(it.appearance)) }
  }

  private fun updateUpdates(transform: (UpdateSettingsState) -> UpdateSettingsState) {
    _uiState.update { it.copy(updates = transform(it.updates)) }
  }

  private fun updateAbout(transform: (AboutSettingsState) -> AboutSettingsState) {
    _uiState.update { it.copy(about = transform(it.about)) }
  }
}
