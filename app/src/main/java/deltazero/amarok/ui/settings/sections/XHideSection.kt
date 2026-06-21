package deltazero.amarok.ui.settings.sections

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import deltazero.amarok.R
import deltazero.amarok.ui.settings.PreferenceGroupHeader
import deltazero.amarok.ui.settings.SettingsSectionPreview
import deltazero.amarok.ui.settings.XHideActions
import deltazero.amarok.ui.settings.XHideSettingsState
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
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column {
      ModuleStatusTile(status = state.status, modifier = Modifier.padding(16.dp))

      if (state.status is XHideStatus.Active) {
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        XHideSwitchRow(
          title = stringResource(R.string.enable_x_hide),
          summary = stringResource(R.string.enable_x_hide_summary),
          checked = state.enabled,
          enabled = true,
          onCheckedChange = actions.setEnabled,
        )
        XHideSwitchRow(
          title = stringResource(R.string.disable_only_with_xhide),
          summary = stringResource(R.string.disable_only_with_xhide_description),
          checked = state.disableOnlyWithXHide,
          enabled = state.enabled,
          onCheckedChange = actions.setDisableOnlyWithXHide,
        )
      }
    }
  }
}

/** Visual accent for the status tile, mapped from [XHideStatus]. */
private enum class TileAccent {
  NEUTRAL,
  WARNING,
  ERROR,
  SUCCESS,
}

private data class TileColors(val container: Color, val content: Color)

@Composable
private fun TileAccent.colors(): TileColors =
  when (this) {
    TileAccent.NEUTRAL ->
      TileColors(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
      )
    TileAccent.WARNING ->
      TileColors(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
      )
    TileAccent.ERROR ->
      TileColors(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
      )
    TileAccent.SUCCESS ->
      TileColors(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
      )
  }

@Composable
private fun ModuleStatusTile(status: XHideStatus, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val accent =
    when (status) {
      XHideStatus.NotInstalled -> TileAccent.NEUTRAL
      XHideStatus.NotActivated,
      XHideStatus.PendingReboot -> TileAccent.WARNING
      is XHideStatus.Incompatible,
      is XHideStatus.Error -> TileAccent.ERROR
      is XHideStatus.Active -> TileAccent.SUCCESS
    }
  val summary =
    when (status) {
      XHideStatus.NotInstalled -> stringResource(R.string.xhide_status_not_installed)
      XHideStatus.NotActivated -> stringResource(R.string.xhide_status_not_activated)
      is XHideStatus.Incompatible ->
        stringResource(
          R.string.xhide_status_incompatible,
          status.moduleProtocol,
          status.appProtocol,
        )
      XHideStatus.PendingReboot -> stringResource(R.string.xhide_status_pending_reboot)
      is XHideStatus.Error -> stringResource(R.string.xhide_status_error, status.message)
      is XHideStatus.Active -> stringResource(R.string.xhide_status_active)
    }
  // States with an in-app fix get a download button; the rest get a labelling pill.
  val showDownload = status is XHideStatus.NotInstalled || status is XHideStatus.Incompatible
  val pillLabel =
    when (status) {
      XHideStatus.NotActivated -> stringResource(R.string.xhide_pill_inactive)
      XHideStatus.PendingReboot -> stringResource(R.string.xhide_pill_reboot)
      is XHideStatus.Error -> stringResource(R.string.xhide_pill_error)
      is XHideStatus.Active -> stringResource(R.string.xhide_pill_active)
      else -> null
    }
  val colors = accent.colors()

  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Surface(color = colors.container, shape = RoundedCornerShape(12.dp)) {
      Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(R.drawable.ic_domino_mask),
          contentDescription = null,
          tint = colors.content,
          modifier = Modifier.size(24.dp),
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.xhide_module),
        style = MaterialTheme.typography.titleSmall,
      )
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.width(12.dp))
    when {
      showDownload ->
        FilledIconButton(
          onClick = {
            context.startActivity(
              Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.xhide_download_url)))
            )
          }
        ) {
          Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = stringResource(R.string.xhide_download),
          )
        }
      pillLabel != null -> StatusPill(label = pillLabel, colors = colors)
    }
  }
}

@Composable
private fun StatusPill(label: String, colors: TileColors) {
  Surface(color = colors.container, shape = CircleShape) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = colors.content, shape = CircleShape) { Box(Modifier.size(6.dp)) }
      Spacer(Modifier.width(6.dp))
      Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.content)
    }
  }
}

@Composable
private fun XHideSwitchRow(
  title: String,
  summary: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .alpha(if (enabled) 1f else 0.38f),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyMedium)
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.width(16.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun XHideSectionPreview() {
  SettingsSectionPreview {
    XHideSection(state = previewState().xHide, actions = previewXHideActions)
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun XHideSectionActivePreview() {
  SettingsSectionPreview {
    XHideSection(
      state =
        previewState()
          .xHide
          .copy(
            status =
              XHideStatus.Active(
                apiVersion = 101,
                frameworkName = "LSPosed",
                frameworkVersion = "1.9.2",
                lastSyncTime = 0L,
              ),
            enabled = true,
            disableOnlyWithXHide = false,
          ),
      actions = previewXHideActions,
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun XHideSectionInactivePreview() {
  SettingsSectionPreview {
    XHideSection(
      state = previewState().xHide.copy(status = XHideStatus.NotActivated),
      actions = previewXHideActions,
    )
  }
}
