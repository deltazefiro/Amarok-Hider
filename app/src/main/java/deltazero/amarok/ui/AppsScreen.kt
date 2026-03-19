package deltazero.amarok.ui

import android.content.res.Configuration
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.AppInfoUtil.AppInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(onOpenEditor: () -> Unit = {}, viewModel: AppsViewModel = viewModel()) {
  val apps by viewModel.managedApps.collectAsState()
  val hiddenApps by viewModel.hiddenApps.collectAsState()
  val processingApps by viewModel.processingApps.collectAsState()

  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      viewModel.loadManagedApps()
    }
  }

  val context = LocalContext.current
  AppsScreen(
    apps = apps,
    hiddenApps = hiddenApps,
    processingApps = processingApps,
    onOpenEditor = onOpenEditor,
    onToggleAllApps = { viewModel.toggleAllApps() },
    onHideApp = { viewModel.hideApp(it) },
    onUnhideApp = { viewModel.unhideApp(it) },
    onLaunchApp = {
      context.packageManager.getLaunchIntentForPackage(it)?.let(context::startActivity)
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
  apps: List<AppInfo>,
  hiddenApps: Set<String>,
  processingApps: Set<String>,
  onOpenEditor: () -> Unit,
  onToggleAllApps: () -> Unit,
  onHideApp: (String) -> Unit,
  onUnhideApp: (String) -> Unit,
  onLaunchApp: (String) -> Unit,
) {
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
      FloatingActionButton(onClick = onToggleAllApps) {
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
            onClick = {
              if (isHidden) onUnhideApp(app.packageName()) else onLaunchApp(app.packageName())
            },
            onLongClick = { if (!isHidden) onHideApp(app.packageName()) },
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
          update = { iv -> iv.setImageDrawable(app.icon()) },
          modifier = Modifier.size(48.dp).alpha(if (isHidden) 0.4f else 1f),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItemPreview(label: String, isHidden: Boolean, isProcessing: Boolean) {
  Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
      if (isProcessing) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 2.dp)
      } else {
        Icon(
          Icons.Default.Android,
          contentDescription = null,
          modifier = Modifier.size(48.dp).alpha(if (isHidden) 0.4f else 1f),
          tint =
            if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppsScreenEmptyPreview() {
  AmarokTheme(dynamicColor = false) {
    AppsScreen(
      apps = emptyList(),
      hiddenApps = emptySet(),
      processingApps = emptySet(),
      onOpenEditor = {},
      onToggleAllApps = {},
      onHideApp = {},
      onUnhideApp = {},
      onLaunchApp = {},
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppGridItemsPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface {
      Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(80.dp)) {
          AppGridItemPreview(label = "Gallery", isHidden = false, isProcessing = false)
        }
        Box(modifier = Modifier.width(80.dp)) {
          AppGridItemPreview(label = "Messages", isHidden = true, isProcessing = false)
        }
        Box(modifier = Modifier.width(80.dp)) {
          AppGridItemPreview(label = "Browser", isHidden = false, isProcessing = true)
        }
      }
    }
  }
}
