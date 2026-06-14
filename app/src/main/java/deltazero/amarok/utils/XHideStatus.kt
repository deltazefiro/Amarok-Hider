package deltazero.amarok.utils

sealed interface XHideStatus {
  data object NotInstalled : XHideStatus

  data object NotActivated : XHideStatus

  data class Incompatible(val moduleProtocol: Int, val appProtocol: Int) : XHideStatus

  data object PendingReboot : XHideStatus

  data class Error(val message: String) : XHideStatus

  data class Active(
    val apiVersion: Int,
    val frameworkName: String,
    val frameworkVersion: String,
    val lastSyncTime: Long,
  ) : XHideStatus
}
