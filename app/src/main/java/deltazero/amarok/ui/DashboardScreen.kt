package deltazero.amarok.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.R
import deltazero.amarok.core.Hider
import deltazero.amarok.ui.theme.AmarokTheme

@Composable
fun DashboardScreen(onChangeStatus: () -> Unit, viewModel: MainViewModel = viewModel()) {
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
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Spacer(Modifier.height(60.dp))

    // Title - no motto
    Text(
      text = stringResource(R.string.app_name),
      style = MaterialTheme.typography.displayMedium,
      modifier = Modifier.padding(start = 42.dp),
    )

    Spacer(Modifier.height(20.dp))

    // Status card
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 35.dp)) {
      Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 35.dp, top = 35.dp, bottom = 35.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                when (state) {
                  Hider.State.HIDDEN -> stringResource(R.string.hidden_status)
                  else -> stringResource(R.string.visible_status)
                },
              style = MaterialTheme.typography.headlineMedium,
            )
            Text(
              text = stringResource(R.string.item_counts, appCount, folderCount),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Button(onClick = onChangeStatus, enabled = state != Hider.State.PROCESSING) {
                Icon(
                  painter =
                    painterResource(
                      if (state == Hider.State.HIDDEN) R.drawable.ic_wolf else R.drawable.ic_paw
                    ),
                  contentDescription = null,
                  modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                  stringResource(
                    if (state == Hider.State.HIDDEN) R.string.unhide else R.string.hide
                  )
                )
              }
              AnimatedVisibility(visible = state == Hider.State.PROCESSING) {
                CircularProgressIndicator(
                  modifier = Modifier.padding(start = 12.dp).size(20.dp),
                  strokeWidth = 2.dp,
                )
              }
            }
          }

          Image(
            painter =
              painterResource(
                if (state == Hider.State.HIDDEN) R.drawable.img_status_hidden
                else R.drawable.img_status_visible
              ),
            contentDescription = null,
            modifier = Modifier.size(130.dp).offset(x = 55.dp),
            colorFilter =
              if (state == Hider.State.HIDDEN)
                ColorFilter.tint(Color(0xFF1F1F1F), blendMode = BlendMode.Modulate)
              else null,
          )
        }
      }
    }

    Spacer(Modifier.height(20.dp))

    // Info panels card
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 35.dp)) {
      Column(modifier = Modifier.padding(horizontal = 35.dp, vertical = 24.dp)) {
        Text(
          text = stringResource(R.string.app_hiding_mode),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = appHiderName, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.file_hiding_mode),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = fileHiderName, style = MaterialTheme.typography.titleMedium)
      }
    }

    Spacer(Modifier.height(42.dp))
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardScreenPreview() {
  AmarokTheme(dynamicColor = false) {
    DashboardScreen(
      state = Hider.State.VISIBLE,
      appCount = 3,
      folderCount = 5,
      appHiderName = "DSM (Device Owner)",
      fileHiderName = "Obfuscate",
      onChangeStatus = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenHiddenPreview() {
  AmarokTheme(dynamicColor = false) {
    DashboardScreen(
      state = Hider.State.HIDDEN,
      appCount = 3,
      folderCount = 5,
      appHiderName = "DSM (Device Owner)",
      fileHiderName = "Obfuscate",
      onChangeStatus = {},
    )
  }
}
