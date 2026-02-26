package deltazero.amarok.ui

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import deltazero.amarok.R
import deltazero.amarok.utils.AppInfoUtil.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetHideAppScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    isHidden: (AppInfo) -> Boolean,
    showSystemApps: Boolean,
    showRootApps: Boolean,
    onToggleApp: (AppInfo) -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onToggleSystemApps: () -> Unit,
    onToggleRootApps: () -> Unit,
    onBack: () -> Unit,
    onShowSystemAppsWarning: (() -> Unit) -> Unit,
    onShowRootAppsWarning: (() -> Unit) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_hide_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; onSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true
                )
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showSystemApps,
                    onClick = {
                        if (!showSystemApps) onShowSystemAppsWarning(onToggleSystemApps)
                        else onToggleSystemApps()
                    },
                    label = { Text(stringResource(R.string.display_system_apps)) }
                )
                FilterChip(
                    selected = showRootApps,
                    onClick = {
                        if (!showRootApps) onShowRootAppsWarning(onToggleRootApps)
                        else onToggleRootApps()
                    },
                    label = { Text(stringResource(R.string.display_root_apps)) }
                )
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName() }) { app ->
                    AppListItem(
                        app = app,
                        isHidden = isHidden(app),
                        onToggle = { onToggleApp(app) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    isHidden: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { ctx -> ImageView(ctx) },
            update = { iv -> iv.setImageDrawable(app.icon()) },
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName(),
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            checked = isHidden,
            onCheckedChange = { onToggle() }
        )
    }
}
