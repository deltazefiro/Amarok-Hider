package deltazero.amarok.xhide

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder

class XHideSyncService : Service() {
  private val binder =
    object : IXHideSyncService.Stub() {
      override fun getStatus(): Bundle {
        if (!isCallerAllowed()) return Bundle()
        return XHideFrameworkStatus.toBundle()
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
    return packages.any { it in XHideContract.ALLOWED_MAIN_PACKAGES } &&
      hasMatchingSignature(packages)
  }

  private fun hasMatchingSignature(packages: Array<String>): Boolean =
    packages.any {
      packageManager.checkSignatures(packageName, it) == PackageManager.SIGNATURE_MATCH
    }
}
