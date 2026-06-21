package deltazero.amarok.ui.settings.sections

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.CrisisAlert
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import deltazero.amarok.R
import deltazero.amarok.ui.settings.ClickPreferenceItem
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.QuickHideActions
import deltazero.amarok.ui.settings.QuickHideSettingsState
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SliderPreferenceItem
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewQuickHideActions
import deltazero.amarok.ui.settings.previewState

@Composable
internal fun QuickHideSection(state: QuickHideSettingsState, actions: QuickHideActions) {
  val context = LocalContext.current

  PreferenceGroupHeader(stringResource(R.string.quick_hide))
  SwitchPreferenceItem(
    title = stringResource(R.string.notification),
    summary = stringResource(R.string.quick_hide_notification_description),
    icon = prefIcon(Icons.Outlined.Notifications),
    checked = state.quickHideService,
    onCheckedChange = { checked ->
      if (checked) {
        actions.requestNotificationPermission(
          { actions.setQuickHideService(true) },
          {
            Toast.makeText(context, R.string.notification_permission_denied, Toast.LENGTH_LONG)
              .show()
          },
        )
      } else {
        actions.setQuickHideService(false)
      }
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.panic_button),
    summary = stringResource(R.string.panic_button_description),
    icon = prefIcon(Icons.Outlined.CrisisAlert),
    checked = state.panicButton,
    enabled = state.quickHideService,
    onCheckedChange = { checked ->
      if (checked) {
        actions.requestSystemAlertPermission(
          { actions.setPanicButton(true) },
          { Toast.makeText(context, R.string.alert_permission_denied, Toast.LENGTH_LONG).show() },
        )
      } else {
        actions.setPanicButton(false)
      }
    },
  )
  ClickPreferenceItem(
    title = stringResource(R.string.panic_button_color),
    summary = stringResource(R.string.panic_button_color_description),
    icon = prefIcon(Icons.Outlined.Colorize),
    enabled = state.quickHideService && state.panicButton,
    onClick = actions.showColorPicker,
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.auto_hide),
    summary = stringResource(R.string.auto_hide_description),
    icon = prefIcon(Icons.Outlined.LockClock),
    checked = state.autoHide,
    enabled = state.quickHideService,
    onCheckedChange = actions.setAutoHide,
  )
  SliderPreferenceItem(
    title = stringResource(R.string.auto_hide_delay),
    summary = stringResource(R.string.auto_hide_delay_description),
    icon = prefIcon(Icons.Outlined.Timer),
    value = state.autoHideDelay,
    valueRange = 0f..30f,
    steps = 29,
    enabled = state.quickHideService && state.autoHide,
    valueLabel = { delay -> context.getString(R.string.auto_hide_delay_minutes, delay.toInt()) },
    onValueChange = actions.setAutoHideDelay,
  )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuickHideSectionPreview() {
  SettingsSectionPreview {
    QuickHideSection(state = previewState().quickHide, actions = previewQuickHideActions)
  }
}
