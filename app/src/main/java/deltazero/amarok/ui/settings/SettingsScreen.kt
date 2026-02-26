package deltazero.amarok.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import deltazero.amarok.Hider
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.UpdateUtil
import deltazero.amarok.utils.XHidePrefBridge

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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            WorkmodeSection(onSwitchAppHider, onSwitchFileHider)
            XHideSection()
            PrivacySection(onSetPassword, onShowCountdownConfirm)
            QuickHideSection(onRequestNotificationPermission, onRequestSystemAlertPermission, onShowColorPicker)
            AppearanceSection(onSwitchLocale)
            UpdateSection()
            AboutSection()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WorkmodeSection(onSwitchAppHider: () -> Unit, onSwitchFileHider: () -> Unit) {
    val context = LocalContext.current
    PreferenceGroupHeader(stringResource(R.string.workmode))
    ClickPreferenceItem(
        title = stringResource(R.string.switch_app_hider),
        summary = stringResource(R.string.current_mode, PrefMgr.getAppHider(context).name),
        icon = { Icon(painterResource(R.drawable.apps_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = onSwitchAppHider
    )
    ClickPreferenceItem(
        title = stringResource(R.string.switch_file_hider),
        summary = stringResource(R.string.current_mode, PrefMgr.getFileHider(context).name),
        icon = { Icon(painterResource(R.drawable.folder_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = onSwitchFileHider
    )
}

@Composable
private fun XHideSection() {
    val isAvailable = XHidePrefBridge.isAvailable
    var enableXHide by remember { mutableStateOf(PrefMgr.isXHideEnabled()) }
    var disableOnlyWithXHide by remember { mutableStateOf(PrefMgr.getDisableOnlyWithXHide()) }

    PreferenceGroupHeader(stringResource(R.string.x_hide))
    ClickPreferenceItem(
        title = "",
        summary = stringResource(R.string.x_hide_description),
        enabled = isAvailable,
        onClick = {}
    )
    SwitchPreferenceItem(
        title = stringResource(R.string.enable_x_hide),
        summary = if (isAvailable) stringResource(R.string.xposed_active, XHidePrefBridge.xposedVersion.toString())
        else stringResource(R.string.xposed_inactive),
        icon = { Icon(painterResource(R.drawable.domino_mask_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = enableXHide,
        enabled = isAvailable,
        onCheckedChange = { checked ->
            enableXHide = checked
            PrefMgr.setXHideEnabled(checked)
        }
    )
    SwitchPreferenceItem(
        title = stringResource(R.string.disable_only_with_xhide),
        summary = stringResource(R.string.disable_only_with_xhide_description),
        icon = { Icon(painterResource(R.drawable.visibility_off_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = disableOnlyWithXHide,
        enabled = isAvailable && enableXHide,
        onCheckedChange = { checked ->
            disableOnlyWithXHide = checked
            PrefMgr.setDisableOnlyWithXHide(checked)
        }
    )
}

@Composable
private fun PrivacySection(
    onSetPassword: (callback: (String?) -> Unit) -> Unit,
    onShowCountdownConfirm: (onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var hasPassword by remember { mutableStateOf(PrefMgr.getAmarokPassword() != null) }
    var biometricAuth by remember { mutableStateOf(PrefMgr.getEnableAmarokBiometricAuth()) }
    var disguise by remember { mutableStateOf(PrefMgr.getEnableDisguise()) }
    var hideIcon by remember { mutableStateOf(PrefMgr.getHideAmarokIcon()) }
    var hideFromRecents by remember { mutableStateOf(PrefMgr.getHideFromRecents()) }
    var blockScreenshots by remember { mutableStateOf(PrefMgr.getBlockScreenshots()) }
    var disableSecurityWhenUnhidden by remember { mutableStateOf(PrefMgr.getDisableSecurityWhenUnhidden()) }
    var disableToasts by remember { mutableStateOf(PrefMgr.getDisableToasts()) }

    PreferenceGroupHeader(stringResource(R.string.security))

    // App lock
    SwitchPreferenceItem(
        title = stringResource(R.string.app_lock),
        summary = stringResource(R.string.app_lock_description),
        icon = { Icon(painterResource(R.drawable.lock_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = hasPassword,
        onCheckedChange = { checked ->
            if (checked) {
                onSetPassword { password ->
                    if (password != null) {
                        PrefMgr.setAmarokPassword(deltazero.amarok.utils.HashUtil.calculateHash(password))
                        deltazero.amarok.utils.SecurityUtil.unlock()
                    }
                    hasPassword = PrefMgr.getAmarokPassword() != null
                    biometricAuth = PrefMgr.getEnableAmarokBiometricAuth()
                }
            } else {
                PrefMgr.setAmarokPassword(null)
                hasPassword = false
            }
        }
    )

    // Biometric
    SwitchPreferenceItem(
        title = stringResource(R.string.biometric_auth),
        summary = stringResource(R.string.biometric_auth_description),
        icon = { Icon(painterResource(R.drawable.fingerprint_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = biometricAuth,
        enabled = hasPassword,
        onCheckedChange = { checked ->
            biometricAuth = checked
            PrefMgr.setEnableAmarokBiometricAuth(checked)
        }
    )

    // Disguise
    SwitchPreferenceItem(
        title = stringResource(R.string.disguise),
        summary = stringResource(R.string.disguise_description),
        icon = { Icon(painterResource(R.drawable.calendar_month_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = disguise,
        enabled = !hideIcon,
        onCheckedChange = { checked ->
            disguise = checked
            PrefMgr.setEnableDisguise(checked)
            PrefMgr.setDoShowQuitDisguiseInstuct(true)
            if (checked) deltazero.amarok.utils.SecurityUtil.lockAndDisguise()
            (context as? android.app.Activity)?.let {
                deltazero.amarok.utils.LauncherIconController.setIconState(
                    it,
                    if (checked) deltazero.amarok.utils.LauncherIconController.IconState.DISGUISED
                    else deltazero.amarok.utils.LauncherIconController.IconState.VISIBLE
                )
            }
        }
    )

    // Hide amarok icon
    SwitchPreferenceItem(
        title = stringResource(R.string.hide_amarok_icon),
        summary = stringResource(R.string.hide_amarok_icon_description),
        icon = { Icon(painterResource(R.drawable.hide_source_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = hideIcon,
        onCheckedChange = { checked ->
            if (checked) {
                onShowCountdownConfirm(
                    { // onConfirm
                        disguise = false
                        PrefMgr.setEnableDisguise(false)
                        (context as? android.app.Activity)?.let {
                            deltazero.amarok.utils.LauncherIconController.setIconState(
                                it,
                                deltazero.amarok.utils.LauncherIconController.IconState.HIDDEN
                            )
                        }
                        hideIcon = true
                        PrefMgr.setHideAmarokIcon(true)
                    },
                    { // onCancel
                    }
                )
            } else {
                hideIcon = false
                PrefMgr.setHideAmarokIcon(false)
                (context as? android.app.Activity)?.let {
                    deltazero.amarok.utils.LauncherIconController.setIconState(
                        it,
                        deltazero.amarok.utils.LauncherIconController.IconState.VISIBLE
                    )
                }
            }
        }
    )

    // Hide from recents
    SwitchPreferenceItem(
        title = stringResource(R.string.hide_from_recents),
        summary = stringResource(R.string.hide_from_recents_description),
        icon = { Icon(painterResource(R.drawable.search_activity_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = hideFromRecents,
        onCheckedChange = { checked ->
            hideFromRecents = checked
            PrefMgr.setHideFromRecents(checked)
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
        }
    )

    // Block screenshots
    SwitchPreferenceItem(
        title = stringResource(R.string.block_screenshots),
        summary = stringResource(R.string.block_screenshots_description),
        icon = { Icon(painterResource(R.drawable.cancel_presentation_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = blockScreenshots,
        onCheckedChange = { checked ->
            blockScreenshots = checked
            PrefMgr.setBlockScreenshots(checked)
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
        }
    )

    // Disable security when unhidden
    SwitchPreferenceItem(
        title = stringResource(R.string.disable_security_when_unhidden),
        summary = stringResource(R.string.disable_security_when_unhidden_description),
        icon = { Icon(painterResource(R.drawable.encrypted_off_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = disableSecurityWhenUnhidden,
        onCheckedChange = { checked ->
            disableSecurityWhenUnhidden = checked
            PrefMgr.setDisableSecurityWhenUnhidden(checked)
        }
    )

    // Disable toasts
    SwitchPreferenceItem(
        title = stringResource(R.string.disable_toasts),
        summary = stringResource(R.string.disable_toasts_description),
        icon = { Icon(painterResource(R.drawable.speaker_notes_off_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = disableToasts,
        onCheckedChange = { checked ->
            disableToasts = checked
            PrefMgr.setDisableToasts(checked)
        }
    )
}

@Composable
private fun QuickHideSection(
    onRequestNotificationPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
    onRequestSystemAlertPermission: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit,
    onShowColorPicker: () -> Unit
) {
    val context = LocalContext.current
    var quickHideService by remember { mutableStateOf(PrefMgr.getEnableQuickHideService()) }
    var panicButton by remember { mutableStateOf(PrefMgr.getEnablePanicButton()) }
    var autoHide by remember { mutableStateOf(PrefMgr.getEnableAutoHide()) }
    var autoHideDelay by remember { mutableStateOf(PrefMgr.getAutoHideDelay().toFloat()) }

    PreferenceGroupHeader(stringResource(R.string.quick_hide))

    // Quick hide service notification
    SwitchPreferenceItem(
        title = stringResource(R.string.notification),
        summary = stringResource(R.string.quick_hide_notification_description),
        icon = { Icon(painterResource(R.drawable.notifications_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = quickHideService,
        onCheckedChange = { checked ->
            if (checked) {
                onRequestNotificationPermission(
                    { // granted
                        quickHideService = true
                        PrefMgr.setEnableQuickHideService(true)
                        deltazero.amarok.QuickHideService.startService(context)
                    },
                    { // denied
                        quickHideService = false
                        Toast.makeText(context, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                quickHideService = false
                PrefMgr.setEnableQuickHideService(false)
                panicButton = false
                PrefMgr.setEnablePanicButton(false)
                deltazero.amarok.QuickHideService.stopService(context)
            }
        }
    )

    // Panic button
    SwitchPreferenceItem(
        title = stringResource(R.string.panic_button),
        summary = stringResource(R.string.panic_button_description),
        icon = { Icon(painterResource(R.drawable.crisis_alert_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = panicButton,
        enabled = quickHideService,
        onCheckedChange = { checked ->
            if (checked) {
                onRequestSystemAlertPermission(
                    { // granted
                        panicButton = true
                        PrefMgr.setEnablePanicButton(true)
                        deltazero.amarok.QuickHideService.startService(context)
                    },
                    { // denied
                        panicButton = false
                        Toast.makeText(context, R.string.alert_permission_denied, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                panicButton = false
                PrefMgr.setEnablePanicButton(false)
                PrefMgr.resetPanicButtonPosition()
                deltazero.amarok.QuickHideService.stopService(context)
                deltazero.amarok.QuickHideService.startService(context)
            }
        }
    )

    // Panic button color
    ClickPreferenceItem(
        title = stringResource(R.string.panic_button_color),
        summary = stringResource(R.string.panic_button_color_description),
        icon = { Icon(painterResource(R.drawable.colors_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        enabled = quickHideService && panicButton,
        onClick = onShowColorPicker
    )

    // Auto hide after screen off
    SwitchPreferenceItem(
        title = stringResource(R.string.auto_hide),
        summary = stringResource(R.string.auto_hide_description),
        icon = { Icon(painterResource(R.drawable.lock_clock_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = autoHide,
        enabled = quickHideService,
        onCheckedChange = { checked ->
            autoHide = checked
            PrefMgr.setEnableAutoHide(checked)
        }
    )

    // Auto hide delay slider
    SliderPreferenceItem(
        title = stringResource(R.string.auto_hide_delay),
        summary = stringResource(R.string.auto_hide_delay_description),
        icon = { Icon(painterResource(R.drawable.timer_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        value = autoHideDelay,
        valueRange = 0f..30f,
        steps = 29,
        enabled = quickHideService && autoHide,
        onValueChange = { value ->
            autoHideDelay = value
            PrefMgr.setAutoHideDelay(value.toInt())
        }
    )
}

@Composable
private fun AppearanceSection(onSwitchLocale: () -> Unit) {
    val context = LocalContext.current
    var dynamicColor by remember { mutableStateOf(PrefMgr.getEnableDynamicColor()) }
    var invertTileColor by remember { mutableStateOf(PrefMgr.getInvertTileColor()) }

    PreferenceGroupHeader(stringResource(R.string.appearance))

    SwitchPreferenceItem(
        title = stringResource(R.string.enable_dynamic_color),
        summary = stringResource(R.string.dynamic_color_description),
        icon = { Icon(painterResource(R.drawable.palette_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = dynamicColor,
        onCheckedChange = { checked ->
            dynamicColor = checked
            PrefMgr.setEnableDynamicColor(checked)
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
        }
    )

    // Dark theme dialog
    var showDarkThemeDialog by remember { mutableStateOf(false) }
    var darkThemeMode by remember { mutableStateOf(PrefMgr.getDarkTheme()) }

    if (showDarkThemeDialog) {
        val options = listOf(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to stringResource(R.string.dark_theme_follow_system),
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO to stringResource(R.string.dark_theme_light),
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES to stringResource(R.string.dark_theme_dark),
        )
        AlertDialog(
            onDismissRequest = { showDarkThemeDialog = false },
            title = { Text(stringResource(R.string.dark_theme)) },
            text = {
                Column {
                    options.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    darkThemeMode = mode
                                    PrefMgr.setDarkTheme(mode)
                                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
                                    showDarkThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = mode == darkThemeMode, onClick = null)
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
            }
        )
    }

    ClickPreferenceItem(
        title = stringResource(R.string.dark_theme),
        summary = stringResource(
            when (darkThemeMode) {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> R.string.dark_theme_dark
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> R.string.dark_theme_light
                else -> R.string.dark_theme_follow_system
            }
        ),
        icon = { Icon(painterResource(R.drawable.contrast_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        onClick = { showDarkThemeDialog = true }
    )

    ClickPreferenceItem(
        title = stringResource(R.string.language),
        summary = stringResource(R.string.language_description),
        icon = { Icon(painterResource(R.drawable.ic_language), null, modifier = Modifier.fillMaxSize()) },
        onClick = onSwitchLocale
    )

    ClickPreferenceItem(
        title = stringResource(R.string.participate_translation),
        summary = stringResource(R.string.participate_translation_description),
        icon = { Icon(painterResource(R.drawable.translate_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://hosted.weblate.org/engage/amarok-hider/"))) }
    )

    SwitchPreferenceItem(
        title = stringResource(R.string.invert_tile_color),
        summary = stringResource(R.string.invert_tile_color_description),
        icon = { Icon(painterResource(R.drawable.invert_colors_24dp_5f6368_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        checked = invertTileColor,
        onCheckedChange = { checked ->
            invertTileColor = checked
            PrefMgr.setInvertTileColor(checked)
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun UpdateSection() {
    val context = LocalContext.current
    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            "?"
        }
    }
    var updateChannel by remember { mutableStateOf(PrefMgr.getUpdateChannel().name) }
    var autoUpdate by remember { mutableStateOf(PrefMgr.getEnableAutoUpdate()) }

    PreferenceGroupHeader(stringResource(R.string.update))

    ClickPreferenceItem(
        title = stringResource(R.string.check_update),
        summary = stringResource(R.string.check_update_description, appVersionName ?: "?"),
        icon = { Icon(painterResource(R.drawable.update_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = { UpdateUtil.checkAndNotify(context, false) }
    )

    DropdownPreferenceItem(
        title = stringResource(R.string.update_channel),
        icon = { Icon(painterResource(R.drawable.alt_route_24dp_1f1f1f_fill0_wght400_grad0_opsz24), null, modifier = Modifier.fillMaxSize()) },
        selectedValue = updateChannel,
        options = listOf(
            UpdateUtil.UpdateChannel.RELEASE.name to stringResource(R.string.update_channel_release),
            UpdateUtil.UpdateChannel.BETA.name to stringResource(R.string.update_channel_beta)
        ),
        onValueChange = { value ->
            updateChannel = value
            PrefMgr.setUpdateChannel(UpdateUtil.UpdateChannel.fromString(value))
        }
    )

    SwitchPreferenceItem(
        title = stringResource(R.string.check_update_on_start),
        summary = stringResource(R.string.check_update_on_start_description),
        icon = { Icon(painterResource(R.drawable.autorenew_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = autoUpdate,
        onCheckedChange = { checked ->
            autoUpdate = checked
            PrefMgr.setEnableAutoUpdate(checked)
        }
    )
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    var showForceUnhideDialog by remember { mutableStateOf(false) }

    if (showForceUnhideDialog) {
        AlertDialog(
            onDismissRequest = { showForceUnhideDialog = false },
            title = { Text(stringResource(R.string.force_unhide)) },
            text = { Text(stringResource(R.string.force_unhide_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    Hider.forceUnhide(context)
                    Toast.makeText(context, R.string.performing_force_unhide, Toast.LENGTH_LONG).show()
                    (context as? android.app.Activity)?.finish()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showForceUnhideDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    PreferenceGroupHeader(stringResource(R.string.about))

    SwitchPreferenceItem(
        title = stringResource(R.string.enable_analytics),
        summary = stringResource(R.string.analytics_description),
        icon = { Icon(painterResource(R.drawable.feedback_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        checked = AppCenterUtil.isAnalyticsEnabled(),
        enabled = AppCenterUtil.isAvailable(),
        onCheckedChange = { checked ->
            AppCenterUtil.setAnalyticsEnabled(checked)
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
        }
    )

    ClickPreferenceItem(
        title = stringResource(R.string.force_unhide),
        summary = stringResource(R.string.force_unhide_description),
        icon = { Icon(painterResource(R.drawable.settings_backup_restore_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = { showForceUnhideDialog = true }
    )

    ClickPreferenceItem(
        title = stringResource(R.string.view_github_repo),
        summary = stringResource(R.string.view_github_repo_description),
        icon = { Icon(painterResource(R.drawable.code_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider"))) }
    )

    ClickPreferenceItem(
        title = stringResource(R.string.join_developer_channel),
        summary = stringResource(R.string.developer_channel_telegram),
        icon = { Icon(painterResource(R.drawable.ic_telegram), null, modifier = Modifier.fillMaxSize()) },
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/amarok_dev"))) }
    )

    ClickPreferenceItem(
        title = stringResource(R.string.usage),
        summary = stringResource(R.string.usage_description),
        icon = { Icon(painterResource(R.drawable.help_outline_black_24dp), null, modifier = Modifier.fillMaxSize()) },
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.doc_url)))) }
    )
}
