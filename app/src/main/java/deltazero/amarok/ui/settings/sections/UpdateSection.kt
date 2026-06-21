package deltazero.amarok.ui.settings.sections

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import deltazero.amarok.R
import deltazero.amarok.ui.settings.ClickPreferenceItem
import deltazero.amarok.ui.settings.DropdownPreferenceItem
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.UpdateActions
import deltazero.amarok.ui.settings.UpdateSettingsState
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewState
import deltazero.amarok.ui.settings.previewUpdateActions
import deltazero.amarok.utils.UpdateUtil

@Composable
internal fun UpdateSection(state: UpdateSettingsState, actions: UpdateActions) {
  PreferenceGroupHeader(stringResource(R.string.update))
  ClickPreferenceItem(
    title = stringResource(R.string.check_update),
    summary = stringResource(R.string.check_update_description, state.appVersionName),
    icon = prefIcon(Icons.Outlined.Update),
    onClick = actions.checkUpdate,
  )
  DropdownPreferenceItem(
    title = stringResource(R.string.update_channel),
    icon = prefIcon(Icons.AutoMirrored.Outlined.AltRoute),
    selectedValue = state.updateChannel,
    options =
      listOf(
        UpdateUtil.UpdateChannel.RELEASE.name to stringResource(R.string.update_channel_release),
        UpdateUtil.UpdateChannel.BETA.name to stringResource(R.string.update_channel_beta),
      ),
    onValueChange = actions.setUpdateChannel,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.check_update_on_start),
    summary = stringResource(R.string.check_update_on_start_description),
    icon = prefIcon(Icons.Outlined.Autorenew),
    checked = state.autoUpdate,
    onCheckedChange = actions.setAutoUpdate,
  )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpdateSectionPreview() {
  SettingsSectionPreview {
    UpdateSection(state = previewState().updates, actions = previewUpdateActions)
  }
}
