package deltazero.amarok.ui.settings

import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.core.LockTrigger
import deltazero.amarok.filehider.FileHiderMode

class WorkmodeActions(
  val setAppHiderMode: (AppHiderMode) -> Unit,
  val setFileHiderMode: (FileHiderMode) -> Unit,
  val setObfuscateLevel: (Int) -> Unit,
)

class XHideActions(
  val setEnabled: (Boolean) -> Unit,
  val setDisableOnlyWithXHide: (Boolean) -> Unit,
)

class PrivacyActions(
  val requestPassword: (callback: (String?) -> Unit) -> Unit,
  val setPasswordHash: (String?) -> Unit,
  val setBiometricAuth: (Boolean) -> Unit,
  val setLockTrigger: (LockTrigger) -> Unit,
  val setDisguise: (Boolean) -> Unit,
  val confirmHideIcon: () -> Unit,
  val unhideIcon: () -> Unit,
  val showCountdownConfirm: (onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit,
  val setHideFromRecents: (Boolean) -> Unit,
  val setBlockScreenshots: (Boolean) -> Unit,
  val setDisableSecurityWhenUnhidden: (Boolean) -> Unit,
  val setDisableToasts: (Boolean) -> Unit,
)

class QuickHideActions(
  val requestNotificationPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  val setQuickHideService: (Boolean) -> Unit,
  val requestSystemAlertPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  val setPanicButton: (Boolean) -> Unit,
  val showColorPicker: () -> Unit,
  val setAutoHide: (Boolean) -> Unit,
  val setAutoHideDelay: (Float) -> Unit,
)

class AppearanceActions(
  val setDynamicColor: (Boolean) -> Unit,
  val setDarkTheme: (Int) -> Unit,
  val switchLocale: () -> Unit,
  val setInvertTileColor: (Boolean) -> Unit,
)

class UpdateActions(
  val checkUpdate: () -> Unit,
  val setUpdateChannel: (String) -> Unit,
  val setAutoUpdate: (Boolean) -> Unit,
)

class AboutActions(val setAnalyticsEnabled: (Boolean) -> Unit, val forceUnhide: () -> Unit)
