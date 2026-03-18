package deltazero.amarok.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.AmarokActivity
import deltazero.amarok.R
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.ui.theme.AmarokTheme

class SetHideAppActivity : AmarokActivity() {

  private val viewModel: AppListViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AmarokTheme {
        val apps by viewModel.appList.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val showSystemApps by viewModel.showSystemApps.collectAsState()
        val showRootApps by viewModel.showRootApps.collectAsState()

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
          },
        )
      }
    }
  }
}
