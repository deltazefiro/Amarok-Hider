package deltazero.amarok.apphider

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.system.Os
import android.util.Log
import deltazero.amarok.BuildConfig
import deltazero.amarok.R
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction
import java.lang.reflect.Method
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper

class ShizukuAppHider(private val context: Context) : AppHider {
  override val mode = AppHiderMode.SHIZUKU
  override val name = "Shizuku"

  companion object {
    const val SHIZUKU_REQ_CODE = 600

    init {
      ShizukuProvider.enableMultiProcessSupport(false)
    }
  }

  override suspend fun activate(): ActivationResult {
    return try {
      if (Shizuku.isPreV11()) {
        Log.w("ShizukuHider", "checkAvailability: Shizuku is running pre v11.")
        return ActivationResult(false, R.string.shizuku_pre_v11)
      }

      if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
        return if (Shizuku.pingBinder()) {
          Log.i("ShizukuHider", "checkAvailability: Shizuku available.")
          ActivationResult(true, 0)
        } else {
          Log.w("ShizukuHider", "checkAvailability: Binder not available.")
          ActivationResult(false, R.string.shizuku_service_not_running)
        }
      }

      if (Shizuku.shouldShowRequestPermissionRationale()) {
        Log.w("ShizukuHider", "checkAvailability: permission denied.")
        return ActivationResult(false, R.string.shizuku_permission_denied)
      }

      // Request permission
      suspendCancellableCoroutine { cont ->
        val listener =
          object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
              if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.i("ShizukuHider", "Permission granted.")
                cont.resume(ActivationResult(true, 0))
              } else {
                Log.i("ShizukuHider", "Permission denied.")
                cont.resume(ActivationResult(false, R.string.shizuku_permission_denied))
              }
              Shizuku.removeRequestPermissionResultListener(this)
            }
          }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(SHIZUKU_REQ_CODE)
      }
    } catch (e: IllegalStateException) {
      Log.w("ShizukuHider", "checkAvailability: Shizuku not available: ", e)
      ActivationResult(false, R.string.shizuku_not_working)
    }
  }

  override suspend fun process(pkgNames: Set<String>, action: HideAction) {
    if (!Shizuku.pingBinder()) {
      Log.w("ShizukuHider", "Binder not available.")
      return
    }

    when (action) {
      is HideAction.Hide -> {
        setAppDisabled(true, pkgNames)
        if (!action.disableOnly) {
          setAppHidden(true, pkgNames)
        }
      }
      is HideAction.Unhide -> {
        setAppDisabled(false, pkgNames)
        setAppHidden(false, pkgNames)
      }
    }
  }

  @SuppressLint("PrivateApi")
  private fun setAppDisabled(disabled: Boolean, pkgNames: Set<String>) {
    val mSetApplicationEnabledSetting: Method
    val iPmInstance: Any

    try {
      val iPmClass = Class.forName("android.content.pm.IPackageManager")
      val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
      val asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder::class.java)
      iPmInstance =
        asInterfaceMethod.invoke(
          null,
          ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")),
        )!!
      mSetApplicationEnabledSetting =
        iPmClass.getMethod(
          "setApplicationEnabledSetting",
          String::class.java,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          String::class.java,
        )
    } catch (e: Exception) {
      Log.e("ShizukuHider", e.toString())
      return
    }

    for (p in pkgNames) {
      try {
        mSetApplicationEnabledSetting.invoke(
          iPmInstance,
          p,
          if (disabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
          else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
          0,
          Os.getuid() / 100000,
          BuildConfig.APPLICATION_ID,
        )
        Log.i("ShizukuHider", "Set app disabled=$disabled: $p")
      } catch (e: Exception) {
        Log.w("ShizukuHider", e.toString())
      }
    }
  }

  @SuppressLint("PrivateApi")
  private fun setAppHidden(hidden: Boolean, pkgNames: Set<String>) {
    val mSetApplicationHiddenSettingAsUser: Method
    val iPmInstance: Any

    try {
      val iPmClass = Class.forName("android.content.pm.IPackageManager")
      val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
      val asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder::class.java)
      iPmInstance =
        asInterfaceMethod.invoke(
          null,
          ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")),
        )!!
      mSetApplicationHiddenSettingAsUser =
        iPmClass.getMethod(
          "setApplicationHiddenSettingAsUser",
          String::class.java,
          Boolean::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
        )
    } catch (e: Exception) {
      Log.e("ShizukuHider", e.toString())
      return
    }

    for (p in pkgNames) {
      try {
        mSetApplicationHiddenSettingAsUser.invoke(iPmInstance, p, hidden, Os.getuid() / 100000)
        Log.i("ShizukuHider", "Set app hidden=$hidden: $p")
      } catch (e: Exception) {
        Log.w("ShizukuHider", e.toString())
      }
    }
  }
}
