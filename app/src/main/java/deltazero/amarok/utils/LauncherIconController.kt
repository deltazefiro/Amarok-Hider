package deltazero.amarok.utils

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import deltazero.amarok.BuildConfig

object LauncherIconController {
  private const val LAUNCHER_DEFAULT = "deltazero.amarok.launcher.default"
  private const val LAUNCHER_CALENDAR = "deltazero.amarok.launcher.calendar"

  enum class IconState {
    VISIBLE, // Normal Amarok icon visible
    DISGUISED, // Calendar icon visible (disguised)
    HIDDEN, // No icon visible
  }

  @JvmStatic
  fun setIconState(activity: Activity, state: IconState) {
    val pm = activity.packageManager

    when (state) {
      IconState.VISIBLE -> {
        // Show normal Amarok icon
        setComponentState(pm, LAUNCHER_DEFAULT, true)
        setComponentState(pm, LAUNCHER_CALENDAR, false)
      }
      IconState.DISGUISED -> {
        // Show calendar icon (disguised)
        setComponentState(pm, LAUNCHER_CALENDAR, true)
        setComponentState(pm, LAUNCHER_DEFAULT, false)
      }
      IconState.HIDDEN -> {
        // Hide all icons
        setComponentState(pm, LAUNCHER_DEFAULT, false)
        setComponentState(pm, LAUNCHER_CALENDAR, false)
      }
    }
  }

  private fun setComponentState(pm: PackageManager, componentName: String, enabled: Boolean) {
    pm.setComponentEnabledSetting(
      ComponentName(BuildConfig.APPLICATION_ID, componentName),
      if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
      },
      PackageManager.DONT_KILL_APP,
    )
  }
}
