package deltazero.amarok.utils

import android.app.Application

object AppCenterUtil {
  @JvmStatic fun cleanUpdatePostpone() {}

  @JvmStatic fun checkUpdate() {}

  @JvmStatic fun setAnalyticsEnabled(enabled: Boolean) {}

  @JvmStatic fun isAnalyticsEnabled(): Boolean = false

  @JvmStatic
  fun startAppCenter(
    application: Application,
    autoUpdateEnabled: Boolean,
    onDisableAutoUpdate: Runnable,
  ) {}

  @JvmStatic fun isAvailable(): Boolean = false
}
