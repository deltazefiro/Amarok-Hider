package deltazero.amarok.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import deltazero.amarok.R

enum class AmarokRoute(val route: String, val labelRes: Int, val iconRes: Int) {
  DASHBOARD("dashboard", R.string.dashboard, R.drawable.ic_home),
  APPS("apps", R.string.apps, R.drawable.ic_app),
  FILES("files", R.string.files, R.drawable.ic_folder),
  SETTINGS("settings", R.string.more_settings, R.drawable.ic_settings);

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
        icon = { Icon(painterResource(screen.iconRes), contentDescription = null) },
        label = { Text(stringResource(screen.labelRes)) },
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
