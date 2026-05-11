package deltazero.amarok.utils

import android.app.Application
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.crashes.Crashes

object AppCenterUtil {
  private const val appSecret = "6bcd9547-9df2-4023-bfcd-6e1a0f0f9e12"

  @JvmStatic
  fun cleanUpdatePostpone() {
    throw NotImplementedError("Not available in Play Store version.")
  }

  @JvmStatic
  fun checkUpdate() {
    throw NotImplementedError("Not available in Play Store version.")
  }

  @JvmStatic
  fun setAnalyticsEnabled(enabled: Boolean) {
    Crashes.setEnabled(enabled)
  }

  @JvmStatic fun isAnalyticsEnabled(): Boolean = Crashes.isEnabled().get()

  @JvmStatic
  fun startAppCenter(
    application: Application,
    autoUpdateEnabled: Boolean,
    onDisableAutoUpdate: Runnable,
  ) {
    AppCenter.start(application, appSecret, Crashes::class.java)
  }

  @JvmStatic fun isAvailable(): Boolean = false
}
