package deltazero.amarok.ui.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RadioOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(text = description, fontSize = 11.sp)
        }
        if (trailingContent != null) {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                trailingContent()
            }
        }
    }
}

// Category section header
@Composable
fun PreferenceGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp)
    )
}

// Clickable preference item (no toggle)
@Composable
fun ClickPreferenceItem(
    title: String,
    summary: String? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.38f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(24.dp)) { icon() }
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.38f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(24.dp)) { icon() }
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
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
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.38f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(modifier = Modifier.size(24.dp)) { icon() }
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(text = valueLabel(value), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled
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
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(24.dp)) { icon() }
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            val selectedLabel = options.find { it.first == selectedValue }?.second ?: summary ?: ""
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onValueChange(value); expanded = false }
                )
            }
        }
    }
}
