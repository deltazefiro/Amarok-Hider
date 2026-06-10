package deltazero.amarok.ui.settings

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.core.Hider
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.filehider.FileHider
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.LauncherIconController
import deltazero.amarok.utils.SecurityUtil
import deltazero.amarok.utils.UpdateUtil
import deltazero.amarok.utils.XHideModuleBridge
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun packageVersionName(application: Context): String =
  try {
    application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "?"
  } catch (_: Exception) {
    "?"
  }

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  @param:ApplicationContext private val application: Context,
  private val settingsRepo: SettingsRepository,
) : ViewModel() {

  private val appVersionName = packageVersionName(application)

  // The workmode section also depends on Hider flows for names and errors.
  private val workmodeFlow =
    combine(
      settingsRepo.settings.map { it.appHiderMode },
      settingsRepo.settings.map { it.fileHiderMode },
      Hider.appHiderError,
      Hider.fileHiderError,
      settingsRepo.settings.map { it.obfuscateLevel },
    ) { appMode, fileMode, appErr, fileErr, obfuscateLevel ->
      WorkmodeSettingsState(
        appHiderMode = appMode,
        fileHiderMode = fileMode,
        appHiderName = AppHider.build(application, appMode).name,
        fileHiderName = FileHider.build(application, fileMode, settingsRepo.settings.value).name,
        appHiderErrorResId = appErr,
        fileHiderErrorResId = fileErr,
        obfuscateLevel = obfuscateLevel,
      )
    }

  val uiState: StateFlow<SettingsUiState> =
    combine(settingsRepo.settings, workmodeFlow, XHideModuleBridge.status) {
        settings,
        workmode,
        xHideStatus ->
        SettingsUiState(
          workmode = workmode,
          xHide = XHideSettingsState(settings, xHideStatus),
          privacy = PrivacySettingsState(settings),
          quickHide = QuickHideSettingsState(settings),
          appearance = AppearanceSettingsState(settings),
          updates = UpdateSettingsState(settings, appVersionName),
          about = AboutSettingsState(),
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

  val hasHiddenFiles: StateFlow<Boolean> =
    Hider.folderStates
      .map { states -> states.values.any { it == Hider.State.HIDDEN } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  fun setAppHiderMode(mode: AppHiderMode) {
    viewModelScope.launch { Hider.switchAppHider(mode) }
  }

  fun setFileHiderMode(mode: FileHiderMode) {
    viewModelScope.launch { Hider.switchFileHider(mode) }
  }

  fun setObfuscateLevel(level: Int) =
    viewModelScope.launch { settingsRepo.setObfuscateLevel(level) }

  fun setXHideEnabled(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setXHideEnabled(enabled) }

  fun setDisableOnlyWithXHide(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setDisableOnlyWithXHide(enabled) }

  fun setPassword(hash: String?) {
    viewModelScope.launch {
      settingsRepo.setPassword(hash)
      if (hash != null) SecurityUtil.unlock()
    }
  }

  fun setBiometricAuth(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setBiometricAuth(enabled) }

  fun setDisguise(enabled: Boolean, activity: Activity?) {
    viewModelScope.launch {
      settingsRepo.setDisguise(enabled)
      settingsRepo.setDoShowQuitDisguiseInstruct(true)
    }
    if (enabled) SecurityUtil.lockAndDisguise()
    activity?.let {
      LauncherIconController.setIconState(
        it,
        if (enabled) LauncherIconController.IconState.DISGUISED
        else LauncherIconController.IconState.VISIBLE,
      )
    }
  }

  fun confirmHideIcon(activity: Activity?) {
    viewModelScope.launch {
      settingsRepo.setDisguise(false)
      settingsRepo.setHideAmarokIcon(true)
    }
    activity?.let {
      LauncherIconController.setIconState(it, LauncherIconController.IconState.HIDDEN)
    }
  }

  fun unhideIcon(activity: Activity?) {
    viewModelScope.launch { settingsRepo.setHideAmarokIcon(false) }
    activity?.let {
      LauncherIconController.setIconState(it, LauncherIconController.IconState.VISIBLE)
    }
  }

  fun setHideFromRecents(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setHideFromRecents(enabled) }

  fun setBlockScreenshots(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setBlockScreenshots(enabled) }

  fun setDisableSecurityWhenUnhidden(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setDisableSecurityWhenUnhidden(enabled) }

  fun setDisableToasts(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setDisableToasts(enabled) }

  fun setQuickHideService(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepo.setQuickHideService(enabled)
      if (!enabled) settingsRepo.setPanicButton(false)
    }
  }

  fun setPanicButton(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepo.setPanicButton(enabled)
      if (!enabled) settingsRepo.resetPanicButtonPosition()
    }
  }

  fun setAutoHide(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoHide(enabled) }

  fun setAutoHideDelay(delay: Float) =
    viewModelScope.launch { settingsRepo.setAutoHideDelay(delay.toInt()) }

  fun setDynamicColor(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setDynamicColor(enabled) }

  fun setDarkTheme(mode: Int) {
    AppCompatDelegate.setDefaultNightMode(mode)
    viewModelScope.launch { settingsRepo.setDarkTheme(mode) }
  }

  fun setInvertTileColor(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setInvertTileColor(enabled) }

  fun setUpdateChannel(channel: UpdateUtil.UpdateChannel) =
    viewModelScope.launch { settingsRepo.setUpdateChannel(channel) }

  fun setAutoUpdate(enabled: Boolean) =
    viewModelScope.launch { settingsRepo.setAutoUpdate(enabled) }

  fun setAnalyticsEnabled(enabled: Boolean) {
    AppCenterUtil.setAnalyticsEnabled(enabled)
  }

  fun forceUnhide() {
    Hider.cancelProcess()
    Hider.processAll(application, Hider.Action.UNHIDE)
  }
}
