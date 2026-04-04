package deltazero.amarok.ui.settings

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal fun prefIcon(@DrawableRes id: Int): @Composable () -> Unit =
  @Composable {
    Icon(painterResource(id), contentDescription = null, modifier = Modifier.fillMaxSize())
  }

// Category section header
@Composable
fun PreferenceGroupHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
  )
}

// Clickable preference item (no toggle)
@Composable
fun ClickPreferenceItem(
  title: String,
  summary: String? = null,
  icon: (@Composable () -> Unit)? = null,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .alpha(if (enabled) 1f else 0.38f),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Box(modifier = Modifier.size(20.dp)) { icon() }
      Spacer(Modifier.width(16.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyMedium)
      if (summary != null) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// Switch preference with icon (replaces MaterialSwitchPreference)
@Composable
fun SwitchPreferenceItem(
  title: String,
  summary: String? = null,
  icon: (@Composable () -> Unit)? = null,
  checked: Boolean,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = enabled) { onCheckedChange(!checked) }
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .alpha(if (enabled) 1f else 0.38f),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Box(modifier = Modifier.size(20.dp)) { icon() }
      Spacer(Modifier.width(16.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyMedium)
      if (summary != null) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(Modifier.width(16.dp))
    Switch(
      checked = checked,
      onCheckedChange = if (enabled) onCheckedChange else null,
      enabled = enabled,
    )
  }
}

// Slider preference (replaces SeekBarPreference)
@Composable
fun SliderPreferenceItem(
  title: String,
  summary: String? = null,
  icon: (@Composable () -> Unit)? = null,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  steps: Int = 0,
  enabled: Boolean = true,
  valueLabel: (Float) -> String = { it.toInt().toString() },
  onValueChange: (Float) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .alpha(if (enabled) 1f else 0.38f)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Box(modifier = Modifier.size(20.dp)) { icon() }
        Spacer(Modifier.width(16.dp))
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        if (summary != null) {
          Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Text(text = valueLabel(value), style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp))
    Slider(
      modifier = Modifier.padding(start = if (icon != null) 34.dp else 0.dp),
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      steps = steps,
      enabled = enabled,
    )
  }
}

// Dropdown preference (replaces DropDownPreference)
@Composable
fun DropdownPreferenceItem(
  title: String,
  summary: String? = null,
  icon: (@Composable () -> Unit)? = null,
  selectedValue: String,
  options: List<Pair<String, String>>, // (value, label) pairs
  onValueChange: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { expanded = true }
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Box(modifier = Modifier.size(20.dp)) { icon() }
      Spacer(Modifier.width(16.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyMedium)
      val selectedLabel = options.find { it.first == selectedValue }?.second ?: summary ?: ""
      Text(
        text = selectedLabel,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { (value, label) ->
        DropdownMenuItem(
          text = { Text(label) },
          onClick = {
            onValueChange(value)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun SettingsComponentsPreviewContent() {
  var switchEnabled by remember { mutableStateOf(true) }
  var sliderValue by remember { mutableStateOf(3f) }
  var selectedChannel by remember { mutableStateOf("release") }

  Column {
    PreferenceGroupHeader(title = "General")
    ClickPreferenceItem(
      title = "Manage Password",
      summary = "Set up or update your lock password",
      onClick = {},
    )
    SwitchPreferenceItem(
      title = "Enable Quick Hide",
      summary = "Show floating shortcut for quick hide",
      checked = switchEnabled,
      onCheckedChange = { switchEnabled = it },
    )
    SliderPreferenceItem(
      title = "Auto-hide Delay",
      summary = "Hide automatically after inactivity",
      value = sliderValue,
      valueRange = 1f..10f,
      steps = 8,
      valueLabel = { "${it.toInt()} min" },
      onValueChange = { sliderValue = it },
    )
    DropdownPreferenceItem(
      title = "Update Channel",
      summary = "Choose release track",
      selectedValue = selectedChannel,
      options = listOf("release" to "Release", "beta" to "Beta"),
      onValueChange = { selectedChannel = it },
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsComponentsPreview() {
  SettingsSectionPreview { SettingsComponentsPreviewContent() }
}
