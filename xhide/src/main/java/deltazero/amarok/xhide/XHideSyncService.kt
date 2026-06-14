package deltazero.amarok.xhide

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder

class XHideSyncService : Service() {
  private val binder =
    object : IXHideSyncService.Stub() {
      override fun getStatus(): Bundle {
        if (!isCallerAllowed()) return Bundle()
        return XHideFrameworkStatus.toBundle().apply {
          @Suppress("DEPRECATION")
          val sentinel =
            runCatching { packageManager.getInstalledPackages(0) }
              .getOrNull()
              ?.firstOrNull { it.packageName == XHideContract.SENTINEL_PACKAGE }
          putBoolean(XHideContract.KEY_HOOKS_LIVE, sentinel != null)
          putInt(XHideContract.KEY_HOOK_COUNT, sentinel?.longVersionCode?.toInt() ?: 0)
          putString(XHideContract.KEY_HOOK_ERROR, sentinel?.versionName ?: "")
        }
      }

      override fun pushSnapshot(snapshot: Bundle): Boolean {
        if (!isCallerAllowed()) return false
        val parsed = XHideSnapshot.fromBundle(snapshot) ?: return false
        return XHideStateStore.write(parsed)
      }
    }

  override fun onBind(intent: Intent?): IBinder = binder

  private fun isCallerAllowed(): Boolean {
    val packages = packageManager.getPackagesForUid(IXHideSyncService.Stub.getCallingUid())
    if (packages.isNullOrEmpty()) return false
    return packages.any { it in XHideContract.ALLOWED_MAIN_PACKAGES }
  }
}
