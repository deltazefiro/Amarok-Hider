package deltazero.amarok.ui.settings.sections

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import deltazero.amarok.R
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.WorkmodeActions
import deltazero.amarok.ui.settings.WorkmodeSettingsState
import deltazero.amarok.ui.settings.previewState
import deltazero.amarok.ui.settings.previewWorkmodeActions

private data class HiderModeOption<T>(
  val mode: T,
  @StringRes val nameResId: Int,
  @StringRes val descResId: Int,
)

private data class ObfuscateLevelOption(
  val level: Int,
  @StringRes val nameResId: Int,
  @StringRes val descResId: Int,
)

private val appHiderModes =
  listOf(
    HiderModeOption(AppHiderMode.ROOT, R.string.apphider_root, R.string.apphider_root_description),
    HiderModeOption(
      AppHiderMode.SHIZUKU,
      R.string.apphider_shizuku,
      R.string.apphider_shizuku_description,
    ),
    HiderModeOption(
      AppHiderMode.DHIZUKU,
      R.string.apphider_dhizuku,
      R.string.apphider_dhizuku_description,
    ),
  )

private val fileHiderModes =
  listOf(
    HiderModeOption(
      FileHiderMode.OBFUSCATE,
      R.string.filehider_obfuscate,
      R.string.filehider_obfuscate_description,
    ),
    HiderModeOption(
      FileHiderMode.CHMOD,
      R.string.filehider_chmod,
      R.string.filehider_chmod_description,
    ),
    HiderModeOption(
      FileHiderMode.NOMEDIA,
      R.string.filehider_nomedia,
      R.string.filehider_nomedia_description,
    ),
  )

private val obfuscateLevels =
  listOf(
    ObfuscateLevelOption(0, R.string.filehider_none, R.string.filehider_obfuscate_description),
    ObfuscateLevelOption(
      1,
      R.string.obfuscate_file_header,
      R.string.obfuscate_file_header_description,
    ),
    ObfuscateLevelOption(2, R.string.obfuscate_text_file, R.string.obfuscate_text_file_description),
    ObfuscateLevelOption(
      3,
      R.string.obfuscate_text_file_enhanced,
      R.string.obfuscate_text_file_description_enhanced,
    ),
  )

@Composable
internal fun WorkmodeSection(
  state: WorkmodeSettingsState,
  isHidden: Boolean,
  actions: WorkmodeActions,
) {
  PreferenceGroupHeader(stringResource(R.string.workmode))
  AppHiderCard(state = state, setAppHiderMode = actions.setAppHiderMode)
  FileHiderCard(
    state = state,
    isHidden = isHidden,
    setFileHiderMode = actions.setFileHiderMode,
    setObfuscateLevel = actions.setObfuscateLevel,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppHiderCard(state: WorkmodeSettingsState, setAppHiderMode: (AppHiderMode) -> Unit) {
  val context = LocalContext.current
  val selectedAppDesc =
    appHiderModes.find { it.mode == state.appHiderMode }?.descResId
      ?: R.string.apphider_none_description

  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.switch_app_hider),
        style = MaterialTheme.typography.titleSmall,
      )
      Spacer(Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        appHiderModes.forEach { option ->
          val isSelected = state.appHiderMode == option.mode
          val isFailed = isSelected && state.appHiderErrorResId != 0
          FilterChip(
            selected = isSelected,
            onClick = { setAppHiderMode(if (isSelected) AppHiderMode.NONE else option.mode) },
            label = { Text(stringResource(option.nameResId)) },
            colors =
              if (isFailed)
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                )
              else FilterChipDefaults.filterChipColors(),
            border =
              if (isFailed)
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = true,
                  borderColor = MaterialTheme.colorScheme.error,
                  selectedBorderColor = MaterialTheme.colorScheme.error,
                  selectedBorderWidth = 1.dp,
                )
              else FilterChipDefaults.filterChipBorder(enabled = true, selected = false),
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(selectedAppDesc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (state.appHiderErrorResId != 0) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 4.dp),
        ) {
          Text(
            text = stringResource(state.appHiderErrorResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.hideapp_doc_url)))
              )
            }
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileHiderCard(
  state: WorkmodeSettingsState,
  isHidden: Boolean,
  setFileHiderMode: (FileHiderMode) -> Unit,
  setObfuscateLevel: (Int) -> Unit,
) {
  val context = LocalContext.current
  val selectedFileDesc =
    fileHiderModes.find { it.mode == state.fileHiderMode }?.descResId
      ?: R.string.filehider_none_description
  val fileCardAlpha = if (isHidden) 0.38f else 1f

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .then(
          if (isHidden)
            Modifier.clickable {
              Toast.makeText(context, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT)
                .show()
            }
          else Modifier
        ),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.padding(16.dp).alpha(fileCardAlpha)) {
      Text(
        text = stringResource(R.string.switch_file_hider),
        style = MaterialTheme.typography.titleSmall,
      )
      Spacer(Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fileHiderModes.forEach { option ->
          val isSelected = state.fileHiderMode == option.mode
          val isFailed = isSelected && state.fileHiderErrorResId != 0
          FilterChip(
            selected = isSelected,
            enabled = !isHidden,
            onClick = { setFileHiderMode(if (isSelected) FileHiderMode.NONE else option.mode) },
            label = { Text(stringResource(option.nameResId)) },
            colors =
              if (isFailed)
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                )
              else FilterChipDefaults.filterChipColors(),
            border =
              if (isFailed)
                FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = true,
                  borderColor = MaterialTheme.colorScheme.error,
                  selectedBorderColor = MaterialTheme.colorScheme.error,
                  selectedBorderWidth = 1.dp,
                )
              else FilterChipDefaults.filterChipBorder(enabled = true, selected = false),
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(selectedFileDesc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (state.fileHiderErrorResId != 0) {
        Text(
          text = stringResource(state.fileHiderErrorResId),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      AnimatedVisibility(visible = state.fileHiderMode == FileHiderMode.OBFUSCATE) {
        ObfuscationOptions(
          obfuscateLevel = state.obfuscateLevel,
          isHidden = isHidden,
          setObfuscateLevel = setObfuscateLevel,
        )
      }
    }
  }
}

@Composable
private fun ObfuscationOptions(
  obfuscateLevel: Int,
  isHidden: Boolean,
  setObfuscateLevel: (Int) -> Unit,
) {
  val levelLabel =
    obfuscateLevels.find { it.level == obfuscateLevel }?.nameResId ?: obfuscateLevels[0].nameResId
  val levelDesc =
    obfuscateLevels.find { it.level == obfuscateLevel }?.descResId
      ?: R.string.filehider_obfuscate_description

  Column {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.obfuscation_options),
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = stringResource(levelLabel),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Slider(
      value = obfuscateLevel.toFloat(),
      onValueChange = { setObfuscateLevel(it.toInt()) },
      enabled = !isHidden,
      valueRange = 0f..3f,
      steps = 2,
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = stringResource(levelDesc),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkmodeSectionPreview() {
  SettingsSectionPreview {
    WorkmodeSection(
      state = previewState().workmode,
      isHidden = false,
      actions = previewWorkmodeActions,
    )
  }
}
