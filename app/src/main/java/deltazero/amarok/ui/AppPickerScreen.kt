package deltazero.amarok.ui

import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.AppInfoUtil.AppInfo

@Composable
fun AppPickerScreen(onBack: () -> Unit, viewModel: AppPickerViewModel = viewModel()) {
  val state by viewModel.uiState.collectAsState()

  AppPickerScreen(
    state = state,
    onBack = onBack,
    onRefresh = viewModel::refreshApps,
    onSearchQueryChange = viewModel::setSearchQuery,
    onToggleSystemApps = viewModel::requestToggleSystemApps,
    onToggleRootApps = viewModel::requestToggleRootApps,
    onToggleApp = viewModel::toggleAppHidden,
    onConfirmWarning = viewModel::confirmWarning,
    onDismissWarning = viewModel::dismissWarning,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
  state: AppPickerUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onToggleSystemApps: () -> Unit,
  onToggleRootApps: () -> Unit,
  onToggleApp: (AppInfo) -> Unit,
  onConfirmWarning: () -> Unit,
  onDismissWarning: () -> Unit,
) {
  var searchActive by remember { mutableStateOf(false) }
  var filterMenuExpanded by remember { mutableStateOf(false) }

  // Warning dialog
  state.pendingWarning?.let { warning ->
    val messageRes =
      when (warning) {
        WarningType.SYSTEM_APPS -> R.string.warning_system_apps
        WarningType.ROOT_APPS -> R.string.warning_root_apps
      }
    AlertDialog(
      onDismissRequest = onDismissWarning,
      title = { Text(stringResource(R.string.warning)) },
      text = { Text(stringResource(messageRes)) },
      confirmButton = {
        TextButton(onClick = onConfirmWarning) { Text(stringResource(R.string.confirm)) }
      },
      dismissButton = {
        TextButton(onClick = onDismissWarning) { Text(stringResource(R.string.cancel)) }
      },
    )
  }

  Scaffold(
    topBar = {
      if (searchActive) {
        SearchTopBar(
          query = state.searchQuery,
          onQueryChange = onSearchQueryChange,
          onClose = {
            searchActive = false
            onSearchQueryChange("")
          },
        )
      } else {
        TopAppBar(
          title = { Text(stringResource(R.string.set_hide_apps)) },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
          },
          actions = {
            IconButton(onClick = { searchActive = true }) {
              Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
            }
            Box {
              IconButton(onClick = { filterMenuExpanded = true }) {
                Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
              }
              DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false },
              ) {
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.display_system_apps)) },
                  onClick = onToggleSystemApps,
                  trailingIcon = {
                    Checkbox(
                      checked = state.showSystemApps,
                      onCheckedChange = { onToggleSystemApps() },
                    )
                  },
                )
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.display_root_apps)) },
                  onClick = onToggleRootApps,
                  trailingIcon = {
                    Checkbox(checked = state.showRootApps, onCheckedChange = { onToggleRootApps() })
                  },
                )
              }
            }
          },
        )
      }
    }
  ) { padding ->
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .pullToRefresh(
            isRefreshing = state.isLoading,
            state = pullToRefreshState,
            onRefresh = onRefresh,
          )
    ) {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.apps, key = { it.packageName() }) { app ->
          val isActuallyHidden = state.actuallyHiddenApps.contains(app.packageName())
          AppPickerItem(
            label = app.label(),
            packageName = app.packageName(),
            icon = {
              AndroidView(
                factory = { ctx -> ImageView(ctx) },
                update = { iv -> iv.setImageDrawable(app.icon()) },
                modifier = Modifier.size(40.dp),
              )
            },
            isHidden = state.hiddenApps.contains(app.packageName()),
            enabled = !isActuallyHidden,
            onToggle = { onToggleApp(app) },
          )
          HorizontalDivider()
        }
      }

      PullToRefreshDefaults.Indicator(
        state = pullToRefreshState,
        isRefreshing = state.isLoading,
        modifier = Modifier.align(Alignment.TopCenter),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  TopAppBar(
    title = {
      TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.search)) },
        singleLine = true,
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
          ),
      )
    },
    navigationIcon = {
      IconButton(onClick = onClose) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
      }
    },
    actions = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(Icons.Default.Close, contentDescription = null)
        }
      }
    },
  )
}

@Composable
private fun AppPickerItem(
  label: String,
  packageName: String,
  icon: @Composable () -> Unit,
  isHidden: Boolean,
  enabled: Boolean,
  onToggle: () -> Unit,
) {
  val context = LocalContext.current
  val onClick = {
    if (enabled) {
      onToggle()
    } else {
      Toast.makeText(context, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show()
    }
  }
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    icon()
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = packageName,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Checkbox(checked = isHidden, enabled = enabled, onCheckedChange = null)
  }
}

// Previews

@Composable
private fun PreviewIcon() {
  Icon(
    Icons.Default.Android,
    contentDescription = null,
    modifier = Modifier.size(40.dp),
    tint = MaterialTheme.colorScheme.primary,
  )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppPickerItemsPreview() {
  AmarokTheme(dynamicColor = false) {
    Surface {
      Column {
        AppPickerItem(
          label = "Gallery",
          packageName = "com.android.gallery3d",
          icon = { PreviewIcon() },
          isHidden = false,
          enabled = true,
          onToggle = {},
        )
        HorizontalDivider()
        AppPickerItem(
          label = "Messages",
          packageName = "com.android.mms",
          icon = { PreviewIcon() },
          isHidden = true,
          enabled = true,
          onToggle = {},
        )
        HorizontalDivider()
        AppPickerItem(
          label = "Calculator",
          packageName = "com.android.calculator2",
          icon = { PreviewIcon() },
          isHidden = true,
          enabled = false,
          onToggle = {},
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppPickerScreenEmptyPreview() {
  AmarokTheme(dynamicColor = false) {
    AppPickerScreen(
      state = AppPickerUiState(),
      onBack = {},
      onRefresh = {},
      onSearchQueryChange = {},
      onToggleSystemApps = {},
      onToggleRootApps = {},
      onToggleApp = {},
      onConfirmWarning = {},
      onDismissWarning = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun AppPickerScreenLoadingPreview() {
  AmarokTheme(dynamicColor = false) {
    AppPickerScreen(
      state = AppPickerUiState(isLoading = true),
      onBack = {},
      onRefresh = {},
      onSearchQueryChange = {},
      onToggleSystemApps = {},
      onToggleRootApps = {},
      onToggleApp = {},
      onConfirmWarning = {},
      onDismissWarning = {},
    )
  }
}
