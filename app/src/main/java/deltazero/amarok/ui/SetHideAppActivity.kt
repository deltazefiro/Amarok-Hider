package deltazero.amarok.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.AmarokActivity
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme

class SetHideAppActivity : AmarokActivity() {

    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmarokTheme {
                val apps by viewModel.getAppList().observeAsState(emptyList())
                val isLoading by viewModel.isLoading().observeAsState(false)
                val showSystemApps by viewModel.getShowSystemApps().observeAsState(false)
                val showRootApps by viewModel.getShowRootApps().observeAsState(false)

                SetHideAppScreen(
                    apps = apps,
                    isLoading = isLoading,
                    isHidden = { app -> PrefMgr.getHideApps().contains(app.packageName()) },
                    showSystemApps = showSystemApps,
                    showRootApps = showRootApps,
                    onToggleApp = { app -> viewModel.toggleAppHidden(app) },
                    onRefresh = { viewModel.refreshApps() },
                    onSearch = { query -> viewModel.setSearchQuery(query) },
                    onToggleSystemApps = { viewModel.toggleSystemApps() },
                    onToggleRootApps = { viewModel.toggleRootApps() },
                    onBack = { finish() },
                    onShowSystemAppsWarning = { onConfirm ->
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.warning_system_apps)
                            .setPositiveButton(R.string.confirm) { _, _ -> onConfirm() }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    },
                    onShowRootAppsWarning = { onConfirm ->
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.warning_root_apps)
                            .setPositiveButton(R.string.confirm) { _, _ -> onConfirm() }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                )
            }
        }
    }
}
