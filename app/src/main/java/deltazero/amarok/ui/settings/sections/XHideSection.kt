package deltazero.amarok.ui.settings.sections

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import deltazero.amarok.R
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.XHideActions
import deltazero.amarok.ui.settings.XHideSettingsState
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewState
import deltazero.amarok.ui.settings.previewXHideActions
import deltazero.amarok.utils.XHideStatus

@Composable
internal fun XHideSection(state: XHideSettingsState, actions: XHideActions) {
  PreferenceGroupHeader(stringResource(R.string.x_hide))
  Text(
    text = stringResource(R.string.x_hide_description),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.enable_x_hide),
    summary = xHideStatusSummary(state.status),
    icon = prefIcon(R.drawable.domino_mask_fill0_wght400_grad0_opsz24),
    checked = state.enabled,
    enabled = state.status is XHideStatus.Active,
    onCheckedChange = actions.setEnabled,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_only_with_xhide),
    summary = stringResource(R.string.disable_only_with_xhide_description),
    icon = prefIcon(R.drawable.visibility_off_24dp),
    checked = state.disableOnlyWithXHide,
    enabled = state.isActive && state.enabled,
    onCheckedChange = actions.setDisableOnlyWithXHide,
  )
}

@Composable
private fun xHideStatusSummary(status: XHideStatus): String =
  when (status) {
    XHideStatus.NotInstalled -> stringResource(R.string.xposed_not_installed)
    XHideStatus.NotActivated -> stringResource(R.string.xposed_not_activated)
    is XHideStatus.Incompatible ->
      stringResource(R.string.xposed_incompatible, status.moduleProtocol, status.appProtocol)
    XHideStatus.PendingReboot -> stringResource(R.string.xposed_pending_reboot)
    is XHideStatus.Error -> stringResource(R.string.xposed_error, status.message)
    is XHideStatus.Active -> stringResource(R.string.xposed_active, status.apiVersion)
  }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun XHideSectionPreview() {
  SettingsSectionPreview {
    XHideSection(state = previewState().xHide, actions = previewXHideActions)
  }
}
