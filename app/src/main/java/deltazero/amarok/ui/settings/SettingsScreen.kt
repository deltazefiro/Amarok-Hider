package deltazero.amarok.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.HashUtil
import deltazero.amarok.utils.UpdateUtil

@Composable
private fun prefIcon(@DrawableRes id: Int) =
  @Composable {
    Icon(painterResource(id), contentDescription = null, modifier = Modifier.fillMaxSize())
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  // Workmode
  onSwitchAppHider: () -> Unit,
  onSwitchFileHider: () -> Unit,
  // Privacy
  onSetPassword: (callback: (String?) -> Unit) -> Unit,
  onShowCountdownConfirm: (onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit,
  // Quick Hide
  onRequestNotificationPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onRequestSystemAlertPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onShowColorPicker: () -> Unit,
  // Appearance
  onSwitchLocale: () -> Unit,
  viewModel: SettingsViewModel = viewModel(),
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsState()

  SettingsScreen(
    state = state,
    onSwitchAppHider = onSwitchAppHider,
    onSwitchFileHider = onSwitchFileHider,
    onSetXHideEnabled = { viewModel.setXHideEnabled(it) },
    onSetDisableOnlyWithXHide = { viewModel.setDisableOnlyWithXHide(it) },
    onSetPassword = onSetPassword,
    onPasswordHashChanged = { viewModel.setPassword(it) },
    onSetBiometricAuth = { viewModel.setBiometricAuth(it) },
    onSetDisguise = { viewModel.setDisguise(it, context as? Activity) },
    onConfirmHideIcon = { viewModel.confirmHideIcon(context as? Activity) },
    onUnhideIcon = { viewModel.unhideIcon(context as? Activity) },
    onShowCountdownConfirm = onShowCountdownConfirm,
    onSetHideFromRecents = { viewModel.setHideFromRecents(it) },
    onSetBlockScreenshots = { viewModel.setBlockScreenshots(it) },
    onSetDisableSecurityWhenUnhidden = { viewModel.setDisableSecurityWhenUnhidden(it) },
    onSetDisableToasts = { viewModel.setDisableToasts(it) },
    onRequestNotificationPermission = onRequestNotificationPermission,
    onSetQuickHideService = { viewModel.setQuickHideService(it) },
    onRequestSystemAlertPermission = onRequestSystemAlertPermission,
    onSetPanicButton = { viewModel.setPanicButton(it) },
    onShowColorPicker = onShowColorPicker,
    onSetAutoHide = { viewModel.setAutoHide(it) },
    onSetAutoHideDelay = { viewModel.setAutoHideDelay(it) },
    onSetDynamicColor = { viewModel.setDynamicColor(it) },
    onSetDarkTheme = { viewModel.setDarkTheme(it) },
    onSwitchLocale = onSwitchLocale,
    onSetInvertTileColor = { viewModel.setInvertTileColor(it) },
    onCheckUpdate = { UpdateUtil.checkAndNotify(context, false) },
    onSetUpdateChannel = { viewModel.setUpdateChannel(UpdateUtil.UpdateChannel.fromString(it)) },
    onSetAutoUpdate = { viewModel.setAutoUpdate(it) },
    onSetAnalyticsEnabled = { viewModel.setAnalyticsEnabled(it) },
    onForceUnhide = { viewModel.forceUnhide() },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  state: SettingsUiState,
  onSwitchAppHider: () -> Unit,
  onSwitchFileHider: () -> Unit,
  onSetXHideEnabled: (Boolean) -> Unit,
  onSetDisableOnlyWithXHide: (Boolean) -> Unit,
  onSetPassword: (callback: (String?) -> Unit) -> Unit,
  onPasswordHashChanged: (String?) -> Unit,
  onSetBiometricAuth: (Boolean) -> Unit,
  onSetDisguise: (Boolean) -> Unit,
  onConfirmHideIcon: () -> Unit,
  onUnhideIcon: () -> Unit,
  onShowCountdownConfirm: (onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit,
  onSetHideFromRecents: (Boolean) -> Unit,
  onSetBlockScreenshots: (Boolean) -> Unit,
  onSetDisableSecurityWhenUnhidden: (Boolean) -> Unit,
  onSetDisableToasts: (Boolean) -> Unit,
  onRequestNotificationPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onSetQuickHideService: (Boolean) -> Unit,
  onRequestSystemAlertPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onSetPanicButton: (Boolean) -> Unit,
  onShowColorPicker: () -> Unit,
  onSetAutoHide: (Boolean) -> Unit,
  onSetAutoHideDelay: (Float) -> Unit,
  onSetDynamicColor: (Boolean) -> Unit,
  onSetDarkTheme: (Int) -> Unit,
  onSwitchLocale: () -> Unit,
  onSetInvertTileColor: (Boolean) -> Unit,
  onCheckUpdate: () -> Unit,
  onSetUpdateChannel: (String) -> Unit,
  onSetAutoUpdate: (Boolean) -> Unit,
  onSetAnalyticsEnabled: (Boolean) -> Unit,
  onForceUnhide: () -> Unit,
) {
  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.more_settings)) }) }) {
    padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
    ) {
      WorkmodeSection(state, onSwitchAppHider, onSwitchFileHider)
      XHideSection(state, onSetXHideEnabled, onSetDisableOnlyWithXHide)
      PrivacySection(
        state,
        onSetPassword,
        onPasswordHashChanged,
        onSetBiometricAuth,
        onSetDisguise,
        onConfirmHideIcon,
        onUnhideIcon,
        onShowCountdownConfirm,
        onSetHideFromRecents,
        onSetBlockScreenshots,
        onSetDisableSecurityWhenUnhidden,
        onSetDisableToasts,
      )
      QuickHideSection(
        state,
        onRequestNotificationPermission,
        onSetQuickHideService,
        onRequestSystemAlertPermission,
        onSetPanicButton,
        onShowColorPicker,
        onSetAutoHide,
        onSetAutoHideDelay,
      )
      AppearanceSection(
        state,
        onSetDynamicColor,
        onSetDarkTheme,
        onSwitchLocale,
        onSetInvertTileColor,
      )
      UpdateSection(state, onCheckUpdate, onSetUpdateChannel, onSetAutoUpdate)
      AboutSection(state, onSetAnalyticsEnabled, onForceUnhide)
      Spacer(Modifier.height(32.dp))
    }
  }
}

@Composable
internal fun WorkmodeSection(
  state: SettingsUiState,
  onSwitchAppHider: () -> Unit,
  onSwitchFileHider: () -> Unit,
) {
  PreferenceGroupHeader(stringResource(R.string.workmode))
  ClickPreferenceItem(
    title = stringResource(R.string.switch_app_hider),
    summary = stringResource(R.string.current_mode, state.appHiderName),
    icon = prefIcon(R.drawable.apps_black_24dp),
    onClick = onSwitchAppHider,
  )
  ClickPreferenceItem(
    title = stringResource(R.string.switch_file_hider),
    summary = stringResource(R.string.current_mode, state.fileHiderName),
    icon = prefIcon(R.drawable.folder_black_24dp),
    onClick = onSwitchFileHider,
  )
}

@Composable
internal fun XHideSection(
  state: SettingsUiState,
  onSetXHideEnabled: (Boolean) -> Unit,
  onSetDisableOnlyWithXHide: (Boolean) -> Unit,
) {
  PreferenceGroupHeader(stringResource(R.string.x_hide))
  ClickPreferenceItem(
    title = "",
    summary = stringResource(R.string.x_hide_description),
    enabled = state.isXHideAvailable,
    onClick = {},
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.enable_x_hide),
    summary =
      if (state.isXHideAvailable)
        stringResource(R.string.xposed_active, state.xposedVersion.toString())
      else stringResource(R.string.xposed_inactive),
    icon = prefIcon(R.drawable.domino_mask_fill0_wght400_grad0_opsz24),
    checked = state.enableXHide,
    enabled = state.isXHideAvailable,
    onCheckedChange = onSetXHideEnabled,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_only_with_xhide),
    summary = stringResource(R.string.disable_only_with_xhide_description),
    icon = prefIcon(R.drawable.visibility_off_24dp),
    checked = state.disableOnlyWithXHide,
    enabled = state.isXHideAvailable && state.enableXHide,
    onCheckedChange = onSetDisableOnlyWithXHide,
  )
}

@Composable
internal fun PrivacySection(
  state: SettingsUiState,
  onSetPassword: (callback: (String?) -> Unit) -> Unit,
  onPasswordHashChanged: (String?) -> Unit,
  onSetBiometricAuth: (Boolean) -> Unit,
  onSetDisguise: (Boolean) -> Unit,
  onConfirmHideIcon: () -> Unit,
  onUnhideIcon: () -> Unit,
  onShowCountdownConfirm: (onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit,
  onSetHideFromRecents: (Boolean) -> Unit,
  onSetBlockScreenshots: (Boolean) -> Unit,
  onSetDisableSecurityWhenUnhidden: (Boolean) -> Unit,
  onSetDisableToasts: (Boolean) -> Unit,
) {
  val context = LocalContext.current

  PreferenceGroupHeader(stringResource(R.string.security))

  // App lock
  SwitchPreferenceItem(
    title = stringResource(R.string.app_lock),
    summary = stringResource(R.string.app_lock_description),
    icon = prefIcon(R.drawable.lock_black_24dp),
    checked = state.hasPassword,
    onCheckedChange = { checked ->
      if (checked) {
        onSetPassword { password ->
          if (password != null) {
            onPasswordHashChanged(HashUtil.calculateHash(password))
          } else {
            onPasswordHashChanged(null)
          }
        }
      } else {
        onPasswordHashChanged(null)
      }
    },
  )

  // Biometric
  SwitchPreferenceItem(
    title = stringResource(R.string.biometric_auth),
    summary = stringResource(R.string.biometric_auth_description),
    icon = prefIcon(R.drawable.fingerprint_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.biometricAuth,
    enabled = state.hasPassword,
    onCheckedChange = onSetBiometricAuth,
  )

  // Disguise
  SwitchPreferenceItem(
    title = stringResource(R.string.disguise),
    summary = stringResource(R.string.disguise_description),
    icon = prefIcon(R.drawable.calendar_month_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.disguise,
    enabled = !state.hideIcon,
    onCheckedChange = onSetDisguise,
  )

  // Hide amarok icon
  SwitchPreferenceItem(
    title = stringResource(R.string.hide_amarok_icon),
    summary = stringResource(R.string.hide_amarok_icon_description),
    icon = prefIcon(R.drawable.hide_source_black_24dp),
    checked = state.hideIcon,
    onCheckedChange = { checked ->
      if (checked) {
        onShowCountdownConfirm(onConfirmHideIcon, { /* onCancel */ })
      } else {
        onUnhideIcon()
      }
    },
  )

  // Hide from recents
  SwitchPreferenceItem(
    title = stringResource(R.string.hide_from_recents),
    summary = stringResource(R.string.hide_from_recents_description),
    icon = prefIcon(R.drawable.search_activity_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.hideFromRecents,
    onCheckedChange = {
      onSetHideFromRecents(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )

  // Block screenshots
  SwitchPreferenceItem(
    title = stringResource(R.string.block_screenshots),
    summary = stringResource(R.string.block_screenshots_description),
    icon = prefIcon(R.drawable.cancel_presentation_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.blockScreenshots,
    onCheckedChange = {
      onSetBlockScreenshots(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )

  // Disable security when unhidden
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_security_when_unhidden),
    summary = stringResource(R.string.disable_security_when_unhidden_description),
    icon = prefIcon(R.drawable.encrypted_off_24dp),
    checked = state.disableSecurityWhenUnhidden,
    onCheckedChange = onSetDisableSecurityWhenUnhidden,
  )

  // Disable toasts
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_toasts),
    summary = stringResource(R.string.disable_toasts_description),
    icon = prefIcon(R.drawable.speaker_notes_off_24dp),
    checked = state.disableToasts,
    onCheckedChange = onSetDisableToasts,
  )
}

@Composable
internal fun QuickHideSection(
  state: SettingsUiState,
  onRequestNotificationPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onSetQuickHideService: (Boolean) -> Unit,
  onRequestSystemAlertPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
  onSetPanicButton: (Boolean) -> Unit,
  onShowColorPicker: () -> Unit,
  onSetAutoHide: (Boolean) -> Unit,
  onSetAutoHideDelay: (Float) -> Unit,
) {
  val context = LocalContext.current

  PreferenceGroupHeader(stringResource(R.string.quick_hide))

  // Quick hide service notification
  SwitchPreferenceItem(
    title = stringResource(R.string.notification),
    summary = stringResource(R.string.quick_hide_notification_description),
    icon = prefIcon(R.drawable.notifications_black_24dp),
    checked = state.quickHideService,
    onCheckedChange = { checked ->
      if (checked) {
        onRequestNotificationPermission(
          { onSetQuickHideService(true) },
          {
            Toast.makeText(context, R.string.notification_permission_denied, Toast.LENGTH_LONG)
              .show()
          },
        )
      } else {
        onSetQuickHideService(false)
      }
    },
  )

  // Panic button
  SwitchPreferenceItem(
    title = stringResource(R.string.panic_button),
    summary = stringResource(R.string.panic_button_description),
    icon = prefIcon(R.drawable.crisis_alert_black_24dp),
    checked = state.panicButton,
    enabled = state.quickHideService,
    onCheckedChange = { checked ->
      if (checked) {
        onRequestSystemAlertPermission(
          { onSetPanicButton(true) },
          { Toast.makeText(context, R.string.alert_permission_denied, Toast.LENGTH_LONG).show() },
        )
      } else {
        onSetPanicButton(false)
      }
    },
  )

  // Panic button color
  ClickPreferenceItem(
    title = stringResource(R.string.panic_button_color),
    summary = stringResource(R.string.panic_button_color_description),
    icon = prefIcon(R.drawable.colors_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    enabled = state.quickHideService && state.panicButton,
    onClick = onShowColorPicker,
  )

  // Auto hide after screen off
  SwitchPreferenceItem(
    title = stringResource(R.string.auto_hide),
    summary = stringResource(R.string.auto_hide_description),
    icon = prefIcon(R.drawable.lock_clock_fill0_wght400_grad0_opsz24),
    checked = state.autoHide,
    enabled = state.quickHideService,
    onCheckedChange = onSetAutoHide,
  )

  // Auto hide delay slider
  SliderPreferenceItem(
    title = stringResource(R.string.auto_hide_delay),
    summary = stringResource(R.string.auto_hide_delay_description),
    icon = prefIcon(R.drawable.timer_fill0_wght400_grad0_opsz24),
    value = state.autoHideDelay,
    valueRange = 0f..30f,
    steps = 29,
    enabled = state.quickHideService && state.autoHide,
    onValueChange = onSetAutoHideDelay,
  )
}

@Composable
internal fun AppearanceSection(
  state: SettingsUiState,
  onSetDynamicColor: (Boolean) -> Unit,
  onSetDarkTheme: (Int) -> Unit,
  onSwitchLocale: () -> Unit,
  onSetInvertTileColor: (Boolean) -> Unit,
) {
  val context = LocalContext.current

  PreferenceGroupHeader(stringResource(R.string.appearance))

  SwitchPreferenceItem(
    title = stringResource(R.string.enable_dynamic_color),
    summary = stringResource(R.string.dynamic_color_description),
    icon = prefIcon(R.drawable.palette_black_24dp),
    checked = state.dynamicColor,
    onCheckedChange = {
      onSetDynamicColor(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )

  // Dark theme dialog
  var showDarkThemeDialog by remember { mutableStateOf(false) }

  if (showDarkThemeDialog) {
    val options =
      listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to
          stringResource(R.string.dark_theme_follow_system),
        AppCompatDelegate.MODE_NIGHT_NO to stringResource(R.string.dark_theme_light),
        AppCompatDelegate.MODE_NIGHT_YES to stringResource(R.string.dark_theme_dark),
      )
    AlertDialog(
      onDismissRequest = { showDarkThemeDialog = false },
      title = { Text(stringResource(R.string.dark_theme)) },
      text = {
        Column {
          options.forEach { (mode, label) ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    onSetDarkTheme(mode)
                    showDarkThemeDialog = false
                  }
                  .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(selected = mode == state.darkThemeMode, onClick = null)
              Spacer(Modifier.width(12.dp))
              Text(label)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showDarkThemeDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }

  ClickPreferenceItem(
    title = stringResource(R.string.dark_theme),
    summary =
      stringResource(
        when (state.darkThemeMode) {
          AppCompatDelegate.MODE_NIGHT_YES -> R.string.dark_theme_dark
          AppCompatDelegate.MODE_NIGHT_NO -> R.string.dark_theme_light
          else -> R.string.dark_theme_follow_system
        }
      ),
    icon = prefIcon(R.drawable.contrast_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    onClick = { showDarkThemeDialog = true },
  )

  ClickPreferenceItem(
    title = stringResource(R.string.language),
    summary = stringResource(R.string.language_description),
    icon = prefIcon(R.drawable.ic_language),
    onClick = onSwitchLocale,
  )

  ClickPreferenceItem(
    title = stringResource(R.string.participate_translation),
    summary = stringResource(R.string.participate_translation_description),
    icon = prefIcon(R.drawable.translate_black_24dp),
    onClick = {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://hosted.weblate.org/engage/amarok-hider/"))
      )
    },
  )

  SwitchPreferenceItem(
    title = stringResource(R.string.invert_tile_color),
    summary = stringResource(R.string.invert_tile_color_description),
    icon = prefIcon(R.drawable.invert_colors_24dp_5f6368_fill0_wght400_grad0_opsz24),
    checked = state.invertTileColor,
    onCheckedChange = {
      onSetInvertTileColor(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )
}

@Composable
internal fun UpdateSection(
  state: SettingsUiState,
  onCheckUpdate: () -> Unit,
  onSetUpdateChannel: (String) -> Unit,
  onSetAutoUpdate: (Boolean) -> Unit,
) {
  PreferenceGroupHeader(stringResource(R.string.update))

  ClickPreferenceItem(
    title = stringResource(R.string.check_update),
    summary = stringResource(R.string.check_update_description, state.appVersionName),
    icon = prefIcon(R.drawable.update_black_24dp),
    onClick = onCheckUpdate,
  )

  DropdownPreferenceItem(
    title = stringResource(R.string.update_channel),
    icon = prefIcon(R.drawable.alt_route_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    selectedValue = state.updateChannel,
    options =
      listOf(
        UpdateUtil.UpdateChannel.RELEASE.name to stringResource(R.string.update_channel_release),
        UpdateUtil.UpdateChannel.BETA.name to stringResource(R.string.update_channel_beta),
      ),
    onValueChange = onSetUpdateChannel,
  )

  SwitchPreferenceItem(
    title = stringResource(R.string.check_update_on_start),
    summary = stringResource(R.string.check_update_on_start_description),
    icon = prefIcon(R.drawable.autorenew_black_24dp),
    checked = state.autoUpdate,
    onCheckedChange = onSetAutoUpdate,
  )
}

@Composable
internal fun AboutSection(
  state: SettingsUiState,
  onSetAnalyticsEnabled: (Boolean) -> Unit,
  onForceUnhide: () -> Unit,
) {
  val context = LocalContext.current
  var showForceUnhideDialog by remember { mutableStateOf(false) }

  if (showForceUnhideDialog) {
    AlertDialog(
      onDismissRequest = { showForceUnhideDialog = false },
      title = { Text(stringResource(R.string.force_unhide)) },
      text = { Text(stringResource(R.string.force_unhide_confirm_msg)) },
      confirmButton = {
        TextButton(
          onClick = {
            onForceUnhide()
            Toast.makeText(context, R.string.performing_force_unhide, Toast.LENGTH_LONG).show()
            (context as? Activity)?.finish()
          }
        ) {
          Text(stringResource(R.string.confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showForceUnhideDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }

  PreferenceGroupHeader(stringResource(R.string.about))

  SwitchPreferenceItem(
    title = stringResource(R.string.enable_analytics),
    summary = stringResource(R.string.analytics_description),
    icon = prefIcon(R.drawable.feedback_black_24dp),
    checked = state.analyticsEnabled,
    enabled = state.analyticsAvailable,
    onCheckedChange = {
      onSetAnalyticsEnabled(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )

  ClickPreferenceItem(
    title = stringResource(R.string.force_unhide),
    summary = stringResource(R.string.force_unhide_description),
    icon = prefIcon(R.drawable.settings_backup_restore_black_24dp),
    onClick = { showForceUnhideDialog = true },
  )

  ClickPreferenceItem(
    title = stringResource(R.string.view_github_repo),
    summary = stringResource(R.string.view_github_repo_description),
    icon = prefIcon(R.drawable.code_black_24dp),
    onClick = {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider"))
      )
    },
  )

  ClickPreferenceItem(
    title = stringResource(R.string.join_developer_channel),
    summary = stringResource(R.string.developer_channel_telegram),
    icon = prefIcon(R.drawable.ic_telegram),
    onClick = {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/amarok_dev")))
    },
  )

  ClickPreferenceItem(
    title = stringResource(R.string.usage),
    summary = stringResource(R.string.usage_description),
    icon = prefIcon(R.drawable.help_outline_black_24dp),
    onClick = {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.doc_url)))
      )
    },
  )
}

// Previews

private fun previewState() =
  SettingsUiState(
    isXHideAvailable = false,
    xposedVersion = 0,
    enableXHide = false,
    disableOnlyWithXHide = false,
    hasPassword = true,
    biometricAuth = false,
    disguise = false,
    hideIcon = false,
    hideFromRecents = false,
    blockScreenshots = false,
    disableSecurityWhenUnhidden = false,
    disableToasts = false,
    quickHideService = true,
    panicButton = false,
    autoHide = false,
    autoHideDelay = 5f,
    dynamicColor = true,
    darkThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    invertTileColor = false,
    appHiderName = "DSM (Device Owner)",
    fileHiderName = "Obfuscate",
    updateChannel = "RELEASE",
    autoUpdate = true,
    appVersionName = "0.10.0",
    analyticsEnabled = false,
    analyticsAvailable = true,
  )

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkmodeSectionPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface { Column { WorkmodeSection(previewState(), {}, {}) } }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun XHideSectionPreview() {
  AmarokTheme(dynamicColor = false) { Surface { Column { XHideSection(previewState(), {}, {}) } } }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrivacySectionPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface {
      Column { PrivacySection(previewState(), {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}) }
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuickHideSectionPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface {
      Column { QuickHideSection(previewState(), { _, _ -> }, {}, { _, _ -> }, {}, {}, {}, {}) }
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceSectionPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface { Column { AppearanceSection(previewState(), {}, {}, {}, {}) } }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpdateSectionPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface { Column { UpdateSection(previewState(), {}, {}, {}) } }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutSectionPreview() {
  AmarokTheme(dynamicColor = false) { Surface { Column { AboutSection(previewState(), {}, {}) } } }
}
