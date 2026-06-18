package deltazero.amarok.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import deltazero.amarok.R
import deltazero.amarok.core.Hider
import deltazero.amarok.ui.theme.AmarokPurpleLight
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.ui.theme.AmarokYellow

/**
 * Placeholder content for sections whose data sources are not wired up yet (reveal activity, last
 * action time, item previews). These exist purely so the redesigned layout renders; replace with
 * real ViewModel state when wiring up the logic.
 */
data class DashboardPreviewData(
  val lastActionLabel: String = "3 min ago",
  val appPreviews: List<String> = listOf("Y", "W", "T"),
  val folderPreviews: List<String> = listOf("abc"),
  val daysHidden: Int = 3,
  val lastRevealedLabel: String = "Sun, Jun 14",
  val frequencyLabel: String = "1.5 days / week",
  val activeDays: Set<Int> = setOf(3, 6, 14),
  val today: Int = 18,
  val daysInMonth: Int = 30,
)

@Composable
fun DashboardScreen(onChangeStatus: () -> Unit, viewModel: MainViewModel = hiltViewModel()) {
  val state by viewModel.hiderState.collectAsState()
  val appCount by viewModel.managedAppCount.collectAsState()
  val folderCount by viewModel.managedFolderCount.collectAsState()
  val appHiderName by viewModel.appHiderName.collectAsState()
  val fileHiderName by viewModel.fileHiderName.collectAsState()

  DashboardScreen(
    state = state,
    appCount = appCount,
    folderCount = folderCount,
    appHiderName = appHiderName,
    fileHiderName = fileHiderName,
    onChangeStatus = onChangeStatus,
  )
}

@Composable
fun DashboardScreen(
  state: Hider.State,
  appCount: Int,
  folderCount: Int,
  appHiderName: String,
  fileHiderName: String,
  onChangeStatus: () -> Unit,
  // Not wired yet — see DashboardPreviewData.
  previewData: DashboardPreviewData = DashboardPreviewData(),
) {
  val isHidden = state == Hider.State.HIDDEN

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
      Spacer(Modifier.height(8.dp))

      // Top bar: title + overflow menu
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {}) {
          Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.dashboard_more))
        }
      }

      Spacer(Modifier.height(8.dp))

      val pad = Modifier.padding(horizontal = 16.dp)

      HeroStatusCard(
        isHidden = isHidden,
        itemCount = appCount + folderCount,
        lastActionLabel = previewData.lastActionLabel,
        modifier = pad.fillMaxWidth(),
      )

      Spacer(Modifier.height(12.dp))

      Row(
        modifier = pad.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        StatCard(
          icon = {
            Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(20.dp))
          },
          title = stringResource(R.string.apps),
          mode = appHiderName,
          count = appCount,
          unit = stringResource(R.string.dashboard_apps_hidden),
          previews = previewData.appPreviews,
          previewIsAvatar = true,
          modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatCard(
          icon = {
            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
          },
          title = stringResource(R.string.files),
          mode = fileHiderName,
          count = folderCount,
          unit = stringResource(R.string.dashboard_folders),
          previews = previewData.folderPreviews,
          previewIsAvatar = false,
          modifier = Modifier.weight(1f).fillMaxHeight(),
        )
      }

      Spacer(Modifier.height(12.dp))

      RevealActivityCard(data = previewData, modifier = pad.fillMaxWidth())

      // Bottom space so the scrolling content clears the FAB.
      Spacer(Modifier.height(96.dp))
    }

    // Hide/unhide toggle.
    ExtendedFloatingActionButton(
      onClick = { if (state != Hider.State.PROCESSING) onChangeStatus() },
      text = { Text(stringResource(if (isHidden) R.string.unhide else R.string.hide)) },
      icon = {
        Icon(
          painter = painterResource(if (isHidden) R.drawable.ic_wolf else R.drawable.ic_paw),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
        )
      },
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(16.dp),
    )
  }
}

@Composable
private fun HeroStatusCard(
  isHidden: Boolean,
  itemCount: Int,
  lastActionLabel: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
  ) {
    Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
      // Wolf anchored to the bottom edge and pushed down so only its top (ears/eyes) peeks up.
      // Wrapped in matchParentSize() so the oversized image can't dictate the card height; the
      // outer Box's clipToBounds() hides the lower half.
      Box(Modifier.matchParentSize()) {
        // requiredSize (not size) so the image can grow beyond the card's height; size() gets
        // clamped by the parent constraints, which is why changing it had no effect.
        Image(
          painter = painterResource(R.drawable.img_status_visible),
          contentDescription = null,
          modifier = Modifier.align(Alignment.BottomEnd).requiredSize(150.dp).offset(y = 42.dp),
        )
      }

      Column(modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 36.dp)) {
        Text(
          text = stringResource(R.string.dashboard_currently).uppercase(),
          style = MaterialTheme.typography.labelSmall,
          letterSpacing = 1.2.sp,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
          text = stringResource(if (isHidden) R.string.hidden_status else R.string.visible_status),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(vertical = 2.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = "$lastActionLabel · " + stringResource(R.string.dashboard_item_count, itemCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
          )
        }
      }
    }
  }
}

@Composable
private fun StatCard(
  icon: @Composable () -> Unit,
  title: String,
  mode: String,
  count: Int,
  unit: String,
  previews: List<String>,
  previewIsAvatar: Boolean,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
  ) {
    Column(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
      // Header stays on one line: title keeps its width, the mode chip is pushed to the
      // trailing edge and ellipsizes ("OBFUS…") when there isn't room for the full mode.
      Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
          ModeChip(mode)
        }
      }

      Spacer(Modifier.height(12.dp))

      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = count.toString(),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = unit,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }

      // Push the preview row to the bottom so both cards align regardless of height.
      Spacer(Modifier.weight(1f).height(12.dp))

      // Keep the previews on a single line: reserve a slot for the overflow chip when needed.
      val hasOverflow = count > 3
      val shown = previews.take(if (hasOverflow) 2 else 3)
      val overflow = count - shown.size
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        shown.forEachIndexed { index, label ->
          if (previewIsAvatar) AvatarChip(label, index) else FolderChip(label)
        }
        if (overflow > 0) OverflowChip(overflow)
      }
    }
  }
}

@Composable
private fun ModeChip(text: String) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

private val avatarColors = listOf(AmarokYellow, Color(0xFF89C4F4), AmarokPurpleLight)

@Composable
private fun AvatarChip(label: String, index: Int) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = avatarColors[index % avatarColors.size].copy(alpha = 0.7f),
    modifier = Modifier.size(28.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF333333),
      )
    }
  }
}

@Composable
private fun FolderChip(label: String) {
  Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
      Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
      Spacer(Modifier.width(4.dp))
      Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
  }
}

@Composable
private fun OverflowChip(count: Int) {
  val color = MaterialTheme.colorScheme.outline
  Box(
    modifier = Modifier.dashedBorder(color).padding(horizontal = 10.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "+$count",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      softWrap = false,
    )
  }
}

@Composable
private fun RevealActivityCard(data: DashboardPreviewData, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text(
        text = stringResource(R.string.dashboard_reveal_activity).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = data.daysHidden.toString(),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.dashboard_days_hidden),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 6.dp),
            )
          }
          Spacer(Modifier.height(16.dp))
          LabeledValue(stringResource(R.string.dashboard_last_revealed), data.lastRevealedLabel)
          Spacer(Modifier.height(12.dp))
          LabeledValue(stringResource(R.string.dashboard_frequency), data.frequencyLabel)
        }

        Spacer(Modifier.width(12.dp))

        MiniCalendar(
          daysInMonth = data.daysInMonth,
          activeDays = data.activeDays,
          today = data.today,
        )
      }
    }
  }
}

@Composable
private fun LabeledValue(label: String, value: String) {
  Column {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      letterSpacing = 1.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

private val calendarCellSize = 22.dp
private val calendarCellShape = RoundedCornerShape(6.dp)

@Composable
private fun MiniCalendar(daysInMonth: Int, activeDays: Set<Int>, today: Int) {
  val weeks = (1..daysInMonth).chunked(7)
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    weeks.forEach { week ->
      Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        week.forEach { day -> CalendarCell(day, activeDays, today) }
        repeat(7 - week.size) { Spacer(Modifier.size(calendarCellSize)) }
      }
    }
  }
}

@Composable
private fun CalendarCell(day: Int, activeDays: Set<Int>, today: Int) {
  val scheme = MaterialTheme.colorScheme
  val active = day in activeDays
  val isToday = day == today
  val isPast = day < today

  val background =
    when {
      active -> scheme.primary
      isPast -> scheme.surface
      else -> Color.Transparent
    }
  val textColor =
    when {
      active -> scheme.onPrimary
      day > today -> scheme.onSurfaceVariant.copy(alpha = 0.4f)
      else -> scheme.onSurface
    }

  Box(
    modifier =
      Modifier.size(calendarCellSize)
        .clip(calendarCellShape)
        .let { if (background != Color.Transparent) it.drawBehind { drawRect(background) } else it }
        .let { if (isToday) it.border(1.5.dp, scheme.primary, calendarCellShape) else it },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = day.toString(),
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      fontWeight = if (active || isToday) FontWeight.Bold else FontWeight.Normal,
      textAlign = TextAlign.Center,
    )
  }
}

/** Draws a dashed pill-shaped border around the content. */
private fun Modifier.dashedBorder(color: Color): Modifier =
  this.drawBehind {
    drawRoundRect(
      color = color,
      cornerRadius = CornerRadius(size.height / 2f),
      style =
        Stroke(
          width = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
        ),
    )
  }

@Preview(showBackground = true, heightDp = 900)
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardScreenHiddenPreview() {
  AmarokTheme(dynamicColor = false) {
    DashboardScreen(
      state = Hider.State.HIDDEN,
      appCount = 14,
      folderCount = 3,
      appHiderName = "Root",
      fileHiderName = "Obfuscate",
      onChangeStatus = {},
    )
  }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DashboardScreenVisiblePreview() {
  AmarokTheme(dynamicColor = false) {
    DashboardScreen(
      state = Hider.State.VISIBLE,
      appCount = 14,
      folderCount = 3,
      appHiderName = "Root",
      fileHiderName = "Obfuscate",
      onChangeStatus = {},
    )
  }
}
