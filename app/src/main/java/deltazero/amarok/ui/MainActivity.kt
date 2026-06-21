package deltazero.amarok.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.OnPermissionCallback
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import deltazero.amarok.AmarokActivity
import deltazero.amarok.R
import deltazero.amarok.core.Hider
import deltazero.amarok.ui.settings.SettingsScreen
import deltazero.amarok.ui.settings.SettingsViewModel
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.PermissionUtil
import deltazero.amarok.utils.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AmarokActivity() {

  private val settingsViewModel: SettingsViewModel by viewModels()
  private lateinit var navController: NavHostController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      AmarokTheme {
        navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar =
          currentRoute in AmarokRoute.tabRoutes || currentRoute == AmarokRoutes.APP_PICKER

        Scaffold(
          bottomBar = { if (showBottomBar) AmarokNavigationBar(navController) },
          // Let each screen's top app bar own the status-bar area so it draws
          // edge-to-edge behind it; only the bottom/horizontal insets are consumed here.
          contentWindowInsets =
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        ) { padding ->
          NavHost(
            navController = navController,
            startDestination = AmarokRoute.DASHBOARD.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(500)) },
          ) {
            composable(AmarokRoute.DASHBOARD.route) {
              DashboardScreen(onChangeStatus = { changeStatus() })
            }
            composable(AmarokRoute.APPS.route) {
              AppsScreen(onOpenEditor = { navController.navigate(AmarokRoutes.APP_PICKER) })
            }
            composable(AmarokRoute.FILES.route) { FilesScreen() }
            composable(AmarokRoutes.APP_PICKER) {
              AppPickerScreen(onBack = { navController.popBackStack() })
            }
            composable(AmarokRoute.SETTINGS.route) {
              SettingsScreen(
                onSetPassword = { callback ->
                  SetPasswordFragment()
                    .setCallback { password -> callback(password) }
                    .show(supportFragmentManager, null)
                },
                onShowCountdownConfirm = { onConfirm, onCancel ->
                  CountdownConfirmDialog.Builder(this@MainActivity)
                    .setTitle(R.string.hide_amarok_icon_dialog_title)
                    .setMessage(R.string.hide_amarok_icon_dialog_message)
                    .setCountdownSeconds(10)
                    .setOnConfirmAction(onConfirm)
                    .setOnCancelAction(onCancel)
                    .show()
                },
                onRequestNotificationPermission = { onGranted, onDenied ->
                  PermissionUtil.requestNotificationPermission(
                    this@MainActivity,
                    object : OnPermissionCallback {
                      override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        onGranted()
                      }

                      override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        onDenied()
                      }
                    },
                  )
                },
                onRequestSystemAlertPermission = { onGranted, onDenied ->
                  PermissionUtil.requestSystemAlertPermission(
                    this@MainActivity,
                    object : OnPermissionCallback {
                      override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        onGranted()
                      }

                      override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        onDenied()
                      }
                    },
                  )
                },
                onShowColorPicker = {
                  val builder =
                    ColorPickerDialog.Builder(this@MainActivity)
                      .setTitle(R.string.panic_button_color)
                      .setPreferenceName("PanicButtonColorPicker")
                      .setPositiveButton(
                        getString(android.R.string.ok),
                        ColorEnvelopeListener { envelope, _ ->
                          lifecycleScope.launch(Dispatchers.IO) {
                            settingsRepo.setPanicButtonColor(envelope.color)
                          }
                        },
                      )
                      .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                      }
                      .attachAlphaSlideBar(true)
                      .attachBrightnessSlideBar(true)
                      .setBottomSpace(12)
                  builder.colorPickerView.setInitialColor(
                    settingsRepo.settings.value.panicButtonColor
                  )
                  builder.show()
                },
                viewModel = settingsViewModel,
              )
            }
          }
        }
      }
    }

    // Show welcome dialog
    if (settingsRepo.settings.value.showWelcome) {
      MaterialAlertDialogBuilder(this)
        .setTitle(R.string.welcome_title)
        .setMessage(R.string.welcome_msg)
        .setPositiveButton(R.string.ok) { _, _ -> PermissionUtil.requestStoragePermission(this) }
        .setNegativeButton(R.string.view_github_repo) { _, _ ->
          startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider"))
          )
          PermissionUtil.requestStoragePermission(this)
        }
        .setOnCancelListener { PermissionUtil.requestStoragePermission(this) }
        .show()
      lifecycleScope.launch(Dispatchers.IO) { settingsRepo.setShowWelcome(false) }
    } else {
      PermissionUtil.requestStoragePermission(this)
    }
    if (settingsRepo.settings.value.autoUpdate) {
      UpdateUtil.checkAndNotify(this, true)
    }
  }

  private fun changeStatus() {
    val appErr = Hider.appHiderError.value
    if (appErr != 0) {
      showNoHiderDialog(appErr)
      return
    }
    val fileErr = Hider.fileHiderError.value
    if (fileErr != 0) {
      showNoHiderDialog(fileErr)
      return
    }
    if (Hider.getState() == Hider.State.HIDDEN) {
      Hider.processAll(this, Hider.Action.UNHIDE)
    } else {
      Hider.processAll(this, Hider.Action.HIDE)
    }
  }

  private fun navigateToSettings() {
    navController.navigate(AmarokRoute.SETTINGS.route) {
      popUpTo(navController.graph.startDestinationId) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  private fun showNoHiderDialog(msgResID: Int) {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.apphider_not_ava_title)
      .setMessage(msgResID)
      .setPositiveButton(R.string.more_settings) { _, _ -> navigateToSettings() }
      .setNegativeButton(getString(R.string.ok), null)
      .show()
  }
}
