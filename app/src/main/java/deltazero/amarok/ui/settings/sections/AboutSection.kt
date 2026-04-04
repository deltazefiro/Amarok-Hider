package deltazero.amarok.ui.settings.sections

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import deltazero.amarok.R
import deltazero.amarok.ui.settings.AboutActions
import deltazero.amarok.ui.settings.AboutSettingsState
import deltazero.amarok.ui.settings.ClickPreferenceItem
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewAboutActions
import deltazero.amarok.ui.settings.previewState

@Composable
internal fun AboutSection(state: AboutSettingsState, actions: AboutActions) {
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
            actions.forceUnhide()
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
      actions.setAnalyticsEnabled(it)
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutSectionPreview() {
  SettingsSectionPreview {
    AboutSection(state = previewState().about, actions = previewAboutActions)
  }
}
