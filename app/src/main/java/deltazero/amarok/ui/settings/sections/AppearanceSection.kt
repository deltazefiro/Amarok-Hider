package deltazero.amarok.ui.settings.sections

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import deltazero.amarok.R
import deltazero.amarok.ui.settings.AppearanceActions
import deltazero.amarok.ui.settings.AppearanceSettingsState
import deltazero.amarok.ui.settings.ClickPreferenceItem
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewAppearanceActions
import deltazero.amarok.ui.settings.previewState

@Composable
internal fun AppearanceSection(state: AppearanceSettingsState, actions: AppearanceActions) {
  val context = LocalContext.current
  var showDarkThemeDialog by remember { mutableStateOf(false) }

  PreferenceGroupHeader(stringResource(R.string.appearance))
  SwitchPreferenceItem(
    title = stringResource(R.string.enable_dynamic_color),
    summary = stringResource(R.string.dynamic_color_description),
    icon = prefIcon(R.drawable.palette_black_24dp),
    checked = state.dynamicColor,
    onCheckedChange = {
      actions.setDynamicColor(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )

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
                    actions.setDarkTheme(mode)
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
    onClick = actions.switchLocale,
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
      actions.setInvertTileColor(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
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
private fun AppearanceSectionPreview() {
  SettingsSectionPreview {
    AppearanceSection(state = previewState().appearance, actions = previewAppearanceActions)
  }
}
