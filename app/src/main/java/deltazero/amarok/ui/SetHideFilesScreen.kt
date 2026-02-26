package deltazero.amarok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import deltazero.amarok.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetHideFilesScreen(
    paths: List<String>,
    onRemovePath: (String) -> Unit,
    onAddFolder: () -> Unit,
    onBack: () -> Unit
) {
    var pathToConfirmRemove by remember { mutableStateOf<String?>(null) }

    // Remove confirmation dialog
    pathToConfirmRemove?.let { path ->
        AlertDialog(
            onDismissRequest = { pathToConfirmRemove = null },
            title = { Text(stringResource(R.string.remove_hide_path)) },
            text = { Text(stringResource(R.string.remove_hide_path_description, path)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemovePath(path)
                    pathToConfirmRemove = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pathToConfirmRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_hide_files)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(paths, key = { _, path -> path }) { _, path ->
                FilePathItem(
                    path = path,
                    onClick = { pathToConfirmRemove = path }
                )
                HorizontalDivider()
            }

            item {
                Button(
                    onClick = onAddFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 24.dp)
                ) {
                    Text(stringResource(R.string.add_hide_files))
                }
            }
        }
    }
}

@Composable
private fun FilePathItem(path: String, onClick: () -> Unit) {
    val folderName = path.substringAfterLast(File.separator).ifEmpty { path }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 15.dp)
    ) {
        Text(
            text = folderName,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = path,
            fontSize = 11.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
