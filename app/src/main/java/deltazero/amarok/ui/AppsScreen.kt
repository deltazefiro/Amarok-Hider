package deltazero.amarok.ui

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.R
import deltazero.amarok.utils.AppInfoUtil.AppInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(onOpenEditor: () -> Unit = {}, viewModel: AppsViewModel = viewModel()) {
  val apps by viewModel.managedApps.collectAsState()
  val hiddenApps by viewModel.hiddenApps.collectAsState()
  val processingApps by viewModel.processingApps.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.apps)) },
        actions = {
          IconButton(onClick = onOpenEditor) { Icon(Icons.Default.Edit, contentDescription = null) }
        },
      )
    },
    floatingActionButton = {
      val allHidden =
        apps.isNotEmpty() && hiddenApps.containsAll(apps.map { it.packageName() }.toSet())
      FloatingActionButton(onClick = { viewModel.toggleAllApps() }) {
        Icon(
          if (allHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
          contentDescription = null,
        )
      }
    },
  ) { padding ->
    if (apps.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.no_managed_apps),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        items(apps, key = { it.packageName() }) { app ->
          val isHidden = hiddenApps.contains(app.packageName())
          val isProcessing = processingApps.contains(app.packageName())

          AppGridItem(
            app = app,
            isHidden = isHidden,
            isProcessing = isProcessing,
            onClick = { if (isHidden) viewModel.unhideApp(app.packageName()) },
            onLongClick = { if (!isHidden) viewModel.hideApp(app.packageName()) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
  app: AppInfo,
  isHidden: Boolean,
  isProcessing: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (isProcessing) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 2.dp)
      } else {
        AndroidView(
          factory = { ctx -> ImageView(ctx) },
          update = { iv ->
            iv.setImageDrawable(app.icon())
            if (isHidden) {
              iv.colorFilter =
                android.graphics.ColorMatrixColorFilter(
                  android.graphics.ColorMatrix().apply { setSaturation(0f) }
                )
            } else {
              iv.colorFilter = null
            }
          },
          modifier = Modifier.size(48.dp),
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = app.label(),
      fontSize = 11.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
