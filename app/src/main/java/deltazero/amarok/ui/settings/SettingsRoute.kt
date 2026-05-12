package deltazero.amarok.ui.settings

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import deltazero.amarok.utils.UpdateUtil

@Composable
fun SettingsScreen(
  onSetPassword: (callback: (String?) -> Unit) -> Unit,
  onShowCountdownConfirm: (onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit,
  onRequestNotificationPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onRequestSystemAlertPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onShowColorPicker: () -> Unit,
  onSwitchLocale: () -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val state by viewModel.uiState.collectAsState()
  val hasHiddenFiles by viewModel.hasHiddenFiles.collectAsState()

  SettingsScreenContent(
    state = state,
    isHidden = hasHiddenFiles,
    workmodeActions =
      WorkmodeActions(
        setAppHiderMode = viewModel::setAppHiderMode,
        setFileHiderMode = viewModel::setFileHiderMode,
        setObfuscateLevel = viewModel::setObfuscateLevel,
      ),
    xHideActions =
      XHideActions(
        setEnabled = viewModel::setXHideEnabled,
        setDisableOnlyWithXHide = viewModel::setDisableOnlyWithXHide,
      ),
    privacyActions =
      PrivacyActions(
        requestPassword = onSetPassword,
        setPasswordHash = viewModel::setPassword,
        setBiometricAuth = viewModel::setBiometricAuth,
        setDisguise = { viewModel.setDisguise(it, activity) },
        confirmHideIcon = { viewModel.confirmHideIcon(activity) },
        unhideIcon = { viewModel.unhideIcon(activity) },
        showCountdownConfirm = onShowCountdownConfirm,
        setHideFromRecents = viewModel::setHideFromRecents,
        setBlockScreenshots = viewModel::setBlockScreenshots,
        setDisableSecurityWhenUnhidden = viewModel::setDisableSecurityWhenUnhidden,
        setDisableToasts = viewModel::setDisableToasts,
      ),
    quickHideActions =
      QuickHideActions(
        requestNotificationPermission = onRequestNotificationPermission,
        setQuickHideService = viewModel::setQuickHideService,
        requestSystemAlertPermission = onRequestSystemAlertPermission,
        setPanicButton = viewModel::setPanicButton,
        showColorPicker = onShowColorPicker,
        setAutoHide = viewModel::setAutoHide,
        setAutoHideDelay = viewModel::setAutoHideDelay,
      ),
    appearanceActions =
      AppearanceActions(
        setDynamicColor = viewModel::setDynamicColor,
        setDarkTheme = viewModel::setDarkTheme,
        switchLocale = onSwitchLocale,
        setInvertTileColor = viewModel::setInvertTileColor,
      ),
    updateActions =
      UpdateActions(
        checkUpdate = { UpdateUtil.checkAndNotify(context, false) },
        setUpdateChannel = { viewModel.setUpdateChannel(UpdateUtil.UpdateChannel.fromString(it)) },
        setAutoUpdate = viewModel::setAutoUpdate,
      ),
    aboutActions =
      AboutActions(
        setAnalyticsEnabled = viewModel::setAnalyticsEnabled,
        forceUnhide = viewModel::forceUnhide,
      ),
  )
}
