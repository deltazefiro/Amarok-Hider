package deltazero.amarok.apphider

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import deltazero.amarok.R
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.Hider
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class DhizukuAppHider(private val context: Context, private val options: AppHiderOptions) :
  AppHider {
  override val mode = AppHiderMode.DHIZUKU
  override val name = "Dhizuku"

  private val devicePolicyManager =
    context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

  override suspend fun activate(): ActivationResult {
    if (!Dhizuku.init()) {
      Log.w("DhizukuHider", "Dhizuku not init.")
      return ActivationResult(false, R.string.dhizuku_not_init)
    }

    if (Dhizuku.getVersionCode() < 5) {
      Log.w("DhizukuHider", "Unsupported Dhizuku version: pre v5.x")
      return ActivationResult(false, R.string.dhizuku_pre_v5)
    }

    if (Dhizuku.isPermissionGranted()) {
      Log.i("DhizukuHider", "Dhizuku available.")
      return ActivationResult(true, 0)
    }

    Log.i("DhizukuHider", "Requesting permission...")
    return suspendCancellableCoroutine { cont ->
      Dhizuku.requestPermission(
        object : DhizukuRequestPermissionListener() {
          override fun onRequestPermission(grantResult: Int) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
              Log.d("DhizukuHider", "Permission denied.")
              cont.resume(ActivationResult(false, R.string.dhizuku_permission_denied))
              return
            }
            Log.d("DhizukuHider", "Permission granted.")

            try {
              setDelegatedScopes()
            } catch (e: Exception) {
              Log.w("DhizukuHider", "Failed to set delegated scopes.", e)
              cont.resume(ActivationResult(false, R.string.dhizuku_failed_to_set_delegated_scopes))
              return
            }

            cont.resume(ActivationResult(true, 0))
          }
        }
      )
    }
  }

  override suspend fun process(pkgNames: Set<String>, action: Hider.Action) {
    setDelegatedScopes()
    val hidden = action == Hider.Action.HIDE
    for (pkgName in pkgNames) {
      devicePolicyManager.setApplicationHidden(null, pkgName, hidden)
    }
  }

  private fun setDelegatedScopes() {
    if (Dhizuku.getDelegatedScopes().contains(DevicePolicyManager.DELEGATION_PACKAGE_ACCESS)) return
    Dhizuku.setDelegatedScopes(arrayOf(DevicePolicyManager.DELEGATION_PACKAGE_ACCESS))
  }
}
