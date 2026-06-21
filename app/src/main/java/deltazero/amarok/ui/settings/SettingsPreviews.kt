package deltazero.amarok.ui.settings

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.XHideStatus

internal fun previewState() =
  SettingsUiState(
    workmode =
      WorkmodeSettingsState(
        appHiderMode = AppHiderMode.ROOT,
        fileHiderMode = FileHiderMode.OBFUSCATE,
        appHiderName = "Root",
        fileHiderName = "Obfuscate",
        obfuscateLevel = 1,
      ),
    xHide =
      XHideSettingsState(
        status = XHideStatus.NotInstalled,
        enabled = false,
        disableOnlyWithXHide = false,
      ),
    privacy =
      PrivacySettingsState(
        hasPassword = true,
        biometricAuth = false,
        disguise = false,
        hideIcon = false,
        hideFromRecents = false,
        blockScreenshots = false,
        disableSecurityWhenUnhidden = false,
      ),
    quickHide =
      QuickHideSettingsState(
        quickHideService = true,
        panicButton = false,
        autoHide = false,
        autoHideDelay = 5f,
      ),
    appearance =
      AppearanceSettingsState(
        dynamicColor = true,
        darkThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        invertTileColor = false,
        disableToasts = false,
      ),
    updates =
      UpdateSettingsState(updateChannel = "RELEASE", autoUpdate = true, appVersionName = "0.10.0"),
    about = AboutSettingsState(analyticsEnabled = false, analyticsAvailable = true),
  )

internal val previewWorkmodeActions =
  WorkmodeActions(setAppHiderMode = {}, setFileHiderMode = {}, setObfuscateLevel = {})

internal val previewXHideActions = XHideActions(setEnabled = {}, setDisableOnlyWithXHide = {})

internal val previewPrivacyActions =
  PrivacyActions(
    requestPassword = {},
    setPasswordHash = {},
    setBiometricAuth = {},
    setLockTrigger = {},
    setDisguise = {},
    confirmHideIcon = {},
    unhideIcon = {},
    showCountdownConfirm = { _, _ -> },
    setHideFromRecents = {},
    setBlockScreenshots = {},
    setDisableSecurityWhenUnhidden = {},
  )

internal val previewQuickHideActions =
  QuickHideActions(
    requestNotificationPermission = { _, _ -> },
    setQuickHideService = {},
    requestSystemAlertPermission = { _, _ -> },
    setPanicButton = {},
    showColorPicker = {},
    setAutoHide = {},
    setAutoHideDelay = {},
  )

internal val previewAppearanceActions =
  AppearanceActions(
    setDynamicColor = {},
    setDarkTheme = {},
    setInvertTileColor = {},
    setDisableToasts = {},
  )

internal val previewUpdateActions =
  UpdateActions(checkUpdate = {}, setUpdateChannel = {}, setAutoUpdate = {})

internal val previewAboutActions = AboutActions(setAnalyticsEnabled = {}, forceUnhide = {})

@Composable
internal fun SettingsSectionPreview(content: @Composable () -> Unit) {
  AmarokTheme(dynamicColor = false) { Surface { Column { content() } } }
}

@Composable
internal fun SettingsScreenPreview(content: @Composable () -> Unit) {
  AmarokTheme(dynamicColor = false) { Surface { content() } }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenContentPreview() {
  SettingsScreenPreview {
    SettingsScreenContent(
      state = previewState(),
      isHidden = false,
      workmodeActions = previewWorkmodeActions,
      xHideActions = previewXHideActions,
      privacyActions = previewPrivacyActions,
      quickHideActions = previewQuickHideActions,
      appearanceActions = previewAppearanceActions,
      updateActions = previewUpdateActions,
      aboutActions = previewAboutActions,
    )
  }
}
