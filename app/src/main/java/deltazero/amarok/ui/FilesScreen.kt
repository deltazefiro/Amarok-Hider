package deltazero.amarok.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.R
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.SDCardUtil
import java.io.File
import java.nio.file.Paths

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(viewModel: FilesViewModel = viewModel()) {
  val context = LocalContext.current
  val folders by viewModel.managedFolders.collectAsState()
  val hiddenFolders by viewModel.hiddenFolders.collectAsState()
  val processingFolders by viewModel.processingFolders.collectAsState()

  val dirLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      uri ?: return@rememberLauncherForActivityResult
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
      val newPath = getPathFromUri(context, uri)
      if (newPath == null) {
        Toast.makeText(context, R.string.not_local_storage, Toast.LENGTH_LONG).show()
        return@rememberLauncherForActivityResult
      }
      // Check for path overlap
      val current = PrefMgr.getHideFilePath()
      val p2 = Paths.get(newPath).toAbsolutePath()
      for (p in current) {
        val p1 = Paths.get(p).toAbsolutePath()
        if (p1.startsWith(p2) || p2.startsWith(p1)) {
          Toast.makeText(context, R.string.path_duplicated, Toast.LENGTH_LONG).show()
          return@rememberLauncherForActivityResult
        }
      }
      viewModel.addFolder(newPath)
    }

  FilesScreen(
    folders = folders,
    hiddenFolders = hiddenFolders,
    processingFolders = processingFolders,
    onAddFolder = {
      if (Hider.getState() == Hider.State.HIDDEN) {
        Toast.makeText(context, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show()
        return@FilesScreen
      }
      dirLauncher.launch(null)
    },
    onToggleAllFolders = { viewModel.toggleAllFolders() },
    onToggleFolder = { path ->
      val isHidden = hiddenFolders.contains(path)
      if (isHidden) viewModel.unhideFolder(path) else viewModel.hideFolder(path)
    },
    onRemoveFolder = { viewModel.removeFolder(it) },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
  folders: List<String>,
  hiddenFolders: Set<String>,
  processingFolders: Set<String>,
  onAddFolder: () -> Unit,
  onToggleAllFolders: () -> Unit,
  onToggleFolder: (String) -> Unit,
  onRemoveFolder: (String) -> Unit,
) {
  var pathToConfirmRemove by remember { mutableStateOf<String?>(null) }

  // Remove confirmation dialog
  pathToConfirmRemove?.let { path ->
    AlertDialog(
      onDismissRequest = { pathToConfirmRemove = null },
      title = { Text(stringResource(R.string.remove_hide_path)) },
      text = { Text(stringResource(R.string.remove_hide_path_description, path)) },
      confirmButton = {
        TextButton(
          onClick = {
            onRemoveFolder(path)
            pathToConfirmRemove = null
          }
        ) {
          Text(stringResource(R.string.confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { pathToConfirmRemove = null }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.files)) },
        actions = {
          IconButton(onClick = onAddFolder) { Icon(Icons.Default.Add, contentDescription = null) }
        },
      )
    },
    floatingActionButton = {
      if (folders.isNotEmpty()) {
        val anyProcessing = processingFolders.isNotEmpty()
        val allHidden = hiddenFolders.containsAll(folders.toSet())
        FloatingActionButton(onClick = { if (!anyProcessing) onToggleAllFolders() }) {
          if (anyProcessing) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              strokeWidth = 3.dp,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          } else {
            Icon(
              if (allHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = null,
            )
          }
        }
      }
    },
  ) { padding ->
    if (folders.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.no_managed_folders),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        items(folders, key = { it }) { path ->
          val isHidden = hiddenFolders.contains(path)
          val isProcessing = processingFolders.contains(path)
          val folderName = path.substringAfterLast(File.separator).ifEmpty { path }

          val dismissState =
            rememberSwipeToDismissBoxState(
              confirmValueChange = {
                if (it == SwipeToDismissBoxValue.StartToEnd && !isHidden) {
                  pathToConfirmRemove = path
                }
                false // Don't actually dismiss; let the dialog handle removal
              }
            )

          SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = false,
            enableDismissFromStartToEnd = !isHidden,
            backgroundContent = {
              val color by
                animateColorAsState(
                  if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd)
                    MaterialTheme.colorScheme.errorContainer
                  else MaterialTheme.colorScheme.surface,
                  label = "swipe-bg",
                )
              Box(
                modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
            },
          ) {
            FolderListItem(
              folderName = folderName,
              fullPath = path,
              isHidden = isHidden,
              isProcessing = isProcessing,
              onToggle = { onToggleFolder(path) },
            )
          }
          HorizontalDivider()
        }
      }
    }
  }
}

@Composable
private fun FolderListItem(
  folderName: String,
  fullPath: String,
  isHidden: Boolean,
  isProcessing: Boolean,
  onToggle: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 24.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = folderName,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = fullPath,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (isProcessing) {
      Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
      }
    } else {
      IconButton(onClick = onToggle) {
        Icon(
          if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
          contentDescription = null,
        )
      }
    }
  }
}

private fun getPathFromUri(context: android.content.Context, uri: Uri): String? {
  val splitUri = uri.path?.split(":") ?: return null
  if (splitUri.size != 2) return null
  SDCardUtil.getSdCardPathFromUri(context, splitUri.toTypedArray())?.let {
    return it
  }
  if ("com.android.externalstorage.documents" == uri.authority) {
    return Environment.getExternalStorageDirectory().toString() + File.separator + splitUri[1]
  }
  return null
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilesScreenEmptyPreview() {
  AmarokTheme(dynamicColor = false) {
    FilesScreen(
      folders = emptyList(),
      hiddenFolders = emptySet(),
      processingFolders = emptySet(),
      onAddFolder = {},
      onToggleAllFolders = {},
      onToggleFolder = {},
      onRemoveFolder = {},
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilesScreenPopulatedPreview() {
  AmarokTheme(dynamicColor = false) {
    FilesScreen(
      folders =
        listOf(
          "/storage/emulated/0/DCIM",
          "/storage/emulated/0/Documents/Private",
          "/storage/emulated/0/Download/Sensitive",
        ),
      hiddenFolders = setOf("/storage/emulated/0/DCIM"),
      processingFolders = setOf("/storage/emulated/0/Download/Sensitive"),
      onAddFolder = {},
      onToggleAllFolders = {},
      onToggleFolder = {},
      onRemoveFolder = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun FolderListItemPreview() {
  AmarokTheme(dynamicColor = false) {
    Column {
      FolderListItem(
        folderName = "DCIM",
        fullPath = "/storage/emulated/0/DCIM",
        isHidden = true,
        isProcessing = false,
        onToggle = {},
      )
      HorizontalDivider()
      FolderListItem(
        folderName = "Documents",
        fullPath = "/storage/emulated/0/Documents",
        isHidden = false,
        isProcessing = false,
        onToggle = {},
      )
      HorizontalDivider()
      FolderListItem(
        folderName = "Download",
        fullPath = "/storage/emulated/0/Download",
        isHidden = false,
        isProcessing = true,
        onToggle = {},
      )
    }
  }
}
