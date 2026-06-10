package deltazero.amarok.xhide

import android.os.Bundle
import io.github.libxposed.service.XposedService
import java.util.concurrent.atomic.AtomicReference

object XHideFrameworkStatus {
  private val statusRef = AtomicReference(Status())

  val active: Boolean
    get() = statusRef.get().moduleActive

  fun onServiceBind(service: XposedService) {
    statusRef.set(
      Status(
        moduleActive = true,
        frameworkName = runCatching { service.frameworkName }.getOrDefault(""),
        frameworkVersion = runCatching { service.frameworkVersion }.getOrDefault(""),
        frameworkVersionCode = runCatching { service.frameworkVersionCode }.getOrDefault(0),
        apiVersion = runCatching { service.apiVersion }.getOrDefault(0),
      )
    )
  }

  fun onServiceDied(service: XposedService) {
    statusRef.set(statusRef.get().copy(moduleActive = false))
  }

  fun toBundle(): Bundle {
    val status = statusRef.get()
    val snapshot = XHideStateStore.snapshot
    return Bundle().apply {
      putBoolean(XHideContract.KEY_MODULE_ACTIVE, status.moduleActive)
      putString(XHideContract.KEY_FRAMEWORK_NAME, status.frameworkName)
      putString(XHideContract.KEY_FRAMEWORK_VERSION, status.frameworkVersion)
      putLong(XHideContract.KEY_FRAMEWORK_VERSION_CODE, status.frameworkVersionCode)
      putInt(XHideContract.KEY_API_VERSION, status.apiVersion)
      putInt(XHideContract.KEY_PROTOCOL_VERSION, snapshot.protocolVersion)
      putLong(XHideContract.KEY_LAST_SYNC_TIME, snapshot.updatedAt)
    }
  }

  private data class Status(
    val moduleActive: Boolean = false,
    val frameworkName: String = "",
    val frameworkVersion: String = "",
    val frameworkVersionCode: Long = 0,
    val apiVersion: Int = 0,
  )
}
