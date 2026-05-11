package deltazero.amarok.utils

import android.app.Activity
import android.app.Application
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute
import com.microsoft.appcenter.distribute.DistributeListener
import com.microsoft.appcenter.distribute.ReleaseDetails
import com.microsoft.appcenter.distribute.UpdateAction
import deltazero.amarok.R

object AppCenterUtil {
  private const val appSecret = "6bcd9547-9df2-4023-bfcd-6e1a0f0f9e12"

  class AmarokDistributeListener(private val onDisableAutoUpdate: Runnable) : DistributeListener {
    override fun onReleaseAvailable(activity: Activity, releaseDetails: ReleaseDetails): Boolean {
      val versionName = releaseDetails.shortVersion
      val versionCode = releaseDetails.version

      Log.i("CheckUpdate", "Found new update: Amarok $versionName")

      try {
        MaterialAlertDialogBuilder(activity)
          .setTitle(R.string.update_ava)
          .setMessage(activity.getString(R.string.update_description, versionName, versionCode))
          .setPositiveButton(R.string.update) { _, _ ->
            Distribute.notifyUpdateAction(UpdateAction.UPDATE)
          }
          .setNeutralButton(R.string.never) { _, _ ->
            onDisableAutoUpdate.run()
            Distribute.notifyUpdateAction(UpdateAction.POSTPONE)
          }
          .setNegativeButton(R.string.cancel) { _, _ ->
            Distribute.notifyUpdateAction(UpdateAction.POSTPONE)
          }
          .setCancelable(false)
          .show()
      } catch (e: Exception) {
        Log.e("AppCenterUtil", "Failed to update: ", e)
        Toast.makeText(activity, R.string.in_app_update_failed, Toast.LENGTH_LONG).show()
        Distribute.notifyUpdateAction(UpdateAction.POSTPONE)
      }

      return true
    }

    override fun onNoReleaseAvailable(activity: Activity) {
      Log.i("CheckUpdate", "No available update yet.")
    }
  }

  @JvmStatic
  fun cleanUpdatePostpone() {
    // To clean postpone
    Distribute.setEnabled(false)
    Distribute.setEnabled(true)
  }

  @JvmStatic
  fun checkUpdate() {
    cleanUpdatePostpone()
    Distribute.checkForUpdate()
  }

  @JvmStatic
  fun setAnalyticsEnabled(enabled: Boolean) {
    Crashes.setEnabled(enabled)
    Analytics.setEnabled(enabled)
  }

  @JvmStatic fun isAnalyticsEnabled(): Boolean = Crashes.isEnabled().get()

  @JvmStatic
  fun startAppCenter(
    application: Application,
    autoUpdateEnabled: Boolean,
    onDisableAutoUpdate: Runnable,
  ) {
    Distribute.setEnabledForDebuggableBuild(false)
    if (!autoUpdateEnabled) Distribute.disableAutomaticCheckForUpdate()

    Distribute.setListener(AmarokDistributeListener(onDisableAutoUpdate))
    AppCenter.start(
      application,
      appSecret,
      Analytics::class.java,
      Crashes::class.java,
      Distribute::class.java,
    )
  }

  @JvmStatic fun isAvailable(): Boolean = true
}
