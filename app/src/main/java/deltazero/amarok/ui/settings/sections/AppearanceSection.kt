package deltazero.amarok.ui.settings.sections

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SpeakerNotesOff
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import deltazero.amarok.R
import deltazero.amarok.ui.settings.AppearanceActions
import deltazero.amarok.ui.settings.AppearanceSettingsState
import deltazero.amarok.ui.settings.ClickPreferenceItem
import deltazero.amarok.ui.settings.DropdownPreferenceItem
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.SwitchPreferenceItem
import deltazero.amarok.ui.settings.prefIcon
import deltazero.amarok.ui.settings.previewAppearanceActions
import deltazero.amarok.ui.settings.previewState

@Composable
internal fun AppearanceSection(state: AppearanceSettingsState, actions: AppearanceActions) {
  val context = LocalContext.current

  PreferenceGroupHeader(stringResource(R.string.appearance))
  SwitchPreferenceItem(
    title = stringResource(R.string.enable_dynamic_color),
    summary = stringResource(R.string.dynamic_color_description),
    icon = prefIcon(Icons.Outlined.Palette),
    checked = state.dynamicColor,
    onCheckedChange = {
      actions.setDynamicColor(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )
  DropdownPreferenceItem(
    title = stringResource(R.string.dark_theme),
    icon = prefIcon(Icons.Outlined.Contrast),
    selectedValue = state.darkThemeMode.toString(),
    options =
      listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString() to
          stringResource(R.string.dark_theme_follow_system),
        AppCompatDelegate.MODE_NIGHT_NO.toString() to stringResource(R.string.dark_theme_light),
        AppCompatDelegate.MODE_NIGHT_YES.toString() to stringResource(R.string.dark_theme_dark),
      ),
    onValueChange = { actions.setDarkTheme(it.toInt()) },
  )
  ClickPreferenceItem(
    title = stringResource(R.string.language),
    summary = stringResource(R.string.language_description),
    icon = prefIcon(Icons.Outlined.Language),
    onClick = actions.switchLocale,
  )
  ClickPreferenceItem(
    title = stringResource(R.string.participate_translation),
    summary = stringResource(R.string.participate_translation_description),
    icon = prefIcon(Icons.Outlined.Translate),
    onClick = {
      context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://hosted.weblate.org/engage/amarok-hider/"))
      )
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.invert_tile_color),
    summary = stringResource(R.string.invert_tile_color_description),
    icon = prefIcon(Icons.Outlined.InvertColors),
    checked = state.invertTileColor,
    onCheckedChange = {
      actions.setInvertTileColor(it)
      Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show()
    },
  )
  SwitchPreferenceItem(
    title = stringResource(R.string.disable_toasts),
    summary = stringResource(R.string.disable_toasts_description),
    icon = prefIcon(Icons.Outlined.SpeakerNotesOff),
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
