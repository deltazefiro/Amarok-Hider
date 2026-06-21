package deltazero.amarok.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import deltazero.amarok.R

enum class AmarokRoute(val route: String, val labelRes: Int, val icon: ImageVector) {
  DASHBOARD("dashboard", R.string.dashboard, Icons.Outlined.Home),
  APPS("apps", R.string.apps, Icons.Outlined.Apps),
  FILES("files", R.string.files, Icons.Outlined.Folder),
  SETTINGS("settings", R.string.more_settings, Icons.Outlined.Settings);

  companion object {
    val tabRoutes: Set<String> = entries.map { it.route }.toSet()
  }
}

object AmarokRoutes {
  const val APP_PICKER = "app_picker"
}

@Composable
fun AmarokNavigationBar(navController: NavController) {
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route

  NavigationBar {
    AmarokRoute.entries.forEach { screen ->
      NavigationBarItem(
        icon = { Icon(screen.icon, contentDescription = null) },
        label = { Text(stringResource(screen.labelRes)) },
        alwaysShowLabel = false,
        selected = currentRoute == screen.route,
        onClick = {
          if (currentRoute != screen.route) {
            navController.navigate(screen.route) {
              popUpTo(navController.graph.startDestinationId) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          }
        },
      )
    }
  }
}
