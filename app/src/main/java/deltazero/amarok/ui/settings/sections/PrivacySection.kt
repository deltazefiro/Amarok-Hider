package deltazero.amarok.ui.settings.sections

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import deltazero.amarok.R
import deltazero.amarok.core.LockTrigger
import deltazero.amarok.ui.settings.DropdownPreferenceItem
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.PrivacyActions
import deltazero.amarok.ui.settings.PrivacySettingsState
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewPrivacyActions
import deltazero.amarok.ui.settings.previewState
import deltazero.amarok.utils.HashUtil

@Composable
internal fun PrivacySection(state: PrivacySettingsState, actions: PrivacyActions) {
  val context = LocalContext.current

  PreferenceGroupHeader(stringResource(R.string.security))
  SwitchPreferenceItem(
    title = stringResource(R.string.app_lock),
    summary = stringResource(R.string.app_lock_description),
    icon = prefIcon(R.drawable.lock_black_24dp),
    checked = state.hasPassword,
    onCheckedChange = { checked ->
      if (checked) {
        actions.requestPassword { password ->
          actions.setPasswordHash(password?.let(HashUtil::calculateHash))
        }
      } else {
        actions.setPasswordHash(null)
      }
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.biometric_auth),
    summary = stringResource(R.string.biometric_auth_description),
    icon = prefIcon(R.drawable.fingerprint_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.biometricAuth,
    enabled = state.hasPassword,
    onCheckedChange = actions.setBiometricAuth,
  )
  DropdownPreferenceItem(
    title = stringResource(R.string.lock_when),
    icon = prefIcon(R.drawable.lock_clock_fill0_wght400_grad0_opsz24),
    selectedValue = state.lockTrigger.key,
    options =
      listOf(
        LockTrigger.APP_BACKGROUND.key to stringResource(R.string.lock_when_app_background),
        LockTrigger.SCREEN_OFF.key to stringResource(R.string.lock_when_screen_off),
        LockTrigger.ON_REOPEN.key to stringResource(R.string.lock_when_reopen),
      ),
    onValueChange = { actions.setLockTrigger(LockTrigger.fromKey(it)) },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disguise),
    summary = stringResource(R.string.disguise_description),
    icon = prefIcon(R.drawable.calendar_month_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.disguise,
    enabled = !state.hideIcon,
    onCheckedChange = actions.setDisguise,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.hide_amarok_icon),
    summary = stringResource(R.string.hide_amarok_icon_description),
    icon = prefIcon(R.drawable.hide_source_black_24dp),
    checked = state.hideIcon,
    onCheckedChange = { checked ->
      if (checked) {
        actions.showCountdownConfirm(actions.confirmHideIcon, {})
      } else {
        actions.unhideIcon()
      }
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.hide_from_recents),
    summary = stringResource(R.string.hide_from_recents_description),
    icon = prefIcon(R.drawable.search_activity_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.hideFromRecents,
    onCheckedChange = {
      actions.setHideFromRecents(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.block_screenshots),
    summary = stringResource(R.string.block_screenshots_description),
    icon = prefIcon(R.drawable.cancel_presentation_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
    checked = state.blockScreenshots,
    onCheckedChange = {
      actions.setBlockScreenshots(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_security_when_unhidden),
    summary = stringResource(R.string.disable_security_when_unhidden_description),
    icon = prefIcon(R.drawable.encrypted_off_24dp),
    checked = state.disableSecurityWhenUnhidden,
    onCheckedChange = actions.setDisableSecurityWhenUnhidden,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_toasts),
    summary = stringResource(R.string.disable_toasts_description),
    icon = prefIcon(R.drawable.speaker_notes_off_24dp),
    checked = state.disableToasts,
    onCheckedChange = actions.setDisableToasts,
  )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrivacySectionPreview() {
  SettingsSectionPreview {
    PrivacySection(state = previewState().privacy, actions = previewPrivacyActions)
  }
}
