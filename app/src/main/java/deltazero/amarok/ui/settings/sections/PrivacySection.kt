package deltazero.amarok.ui.settings.sections

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.NoEncryption
import androidx.compose.material.icons.outlined.ScreenSearchDesktop
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
    icon = prefIcon(Icons.Outlined.Lock),
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
    icon = prefIcon(Icons.Outlined.Fingerprint),
    checked = state.biometricAuth,
    enabled = state.hasPassword,
    onCheckedChange = actions.setBiometricAuth,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disguise),
    summary = stringResource(R.string.disguise_description),
    icon = prefIcon(Icons.Outlined.CalendarMonth),
    checked = state.disguise,
    enabled = !state.hideIcon,
    onCheckedChange = actions.setDisguise,
  )
  DropdownPreferenceItem(
    title = stringResource(R.string.lock_when),
    icon = prefIcon(Icons.Outlined.LockClock),
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
    title = stringResource(R.string.disable_security_when_unhidden),
    summary = stringResource(R.string.disable_security_when_unhidden_description),
    icon = prefIcon(Icons.Outlined.NoEncryption),
    checked = state.disableSecurityWhenUnhidden,
    onCheckedChange = actions.setDisableSecurityWhenUnhidden,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.hide_amarok_icon),
    summary = stringResource(R.string.hide_amarok_icon_description),
    icon = prefIcon(Icons.Outlined.HideSource),
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
    icon = prefIcon(Icons.Outlined.ScreenSearchDesktop),
    checked = state.hideFromRecents,
    onCheckedChange = {
      actions.setHideFromRecents(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.block_screenshots),
    summary = stringResource(R.string.block_screenshots_description),
    icon = prefIcon(Icons.Outlined.CancelPresentation),
    checked = state.blockScreenshots,
    onCheckedChange = {
      actions.setBlockScreenshots(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
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
