package deltazero.amarok.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import deltazero.amarok.xhide.IXHideSyncService
import deltazero.amarok.xhide.XHideContract
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object XHideModuleBridge {
  private const val TAG = "XHideModuleBridge"
  private const val MAX_RETRY_DELAY_MS = 30_000L
  private val RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L, 5_000L, 15_000L, MAX_RETRY_DELAY_MS)

  @Volatile
  var isAvailable = false
    private set

  @Volatile
  var isModuleActive = false
    private set

  @Volatile
  var frameworkName = ""
    private set

  @Volatile
  var frameworkVersion = ""
    private set

  @Volatile
  var apiVersion = 0
    private set

  private val _status = MutableStateFlow(Status())
  val status: StateFlow<Status> = _status.asStateFlow()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val syncLock = Any()
  private var latestSnapshot: Bundle? = null
  private var latestSnapshotVersion = 0L
  private var syncJob: Job? = null

  fun init(context: Context) {
    isAvailable = isTrustedModuleInstalled(context)
    publishStatus()
    if (!isAvailable) return

    val app = context.applicationContext
    scheduleSync(app)
  }

  fun startSync(
    context: Context,
    settingsRepo: deltazero.amarok.core.SettingsRepository,
    hiderStateRepo: deltazero.amarok.core.HiderStateRepository,
  ) {
    val app = context.applicationContext
    scope.launch {
      combine(
          hiderStateRepo.hiddenApps,
          settingsRepo.settings.map { it.xHideEnabled }.distinctUntilChanged(),
        ) { hiddenApps, xHideEnabled ->
          hiddenApps to xHideEnabled
        }
        .distinctUntilChanged()
        .collect { (hiddenApps, xHideEnabled) ->
          synchronized(syncLock) {
            latestSnapshot = buildSnapshot(app, hiddenApps, xHideEnabled)
            latestSnapshotVersion++
          }
          scheduleSync(app, restart = true)
        }
    }
  }

  private fun scheduleSync(context: Context, restart: Boolean = false) {
    synchronized(syncLock) {
      if (restart) syncJob?.cancel()
      if (syncJob?.isActive == true) return
      syncJob = scope.launch { syncUntilReady(context.applicationContext) }
    }
  }

  private suspend fun syncUntilReady(context: Context) {
    var attempt = 0
    while (true) {
      val targetVersion = synchronized(syncLock) { latestSnapshotVersion }
      val success = trySyncOnce(context, targetVersion)
      if (success && synchronized(syncLock) { latestSnapshotVersion == targetVersion }) return
      delay(RETRY_DELAYS_MS.getOrElse(attempt++) { MAX_RETRY_DELAY_MS })
    }
  }

  private suspend fun trySyncOnce(context: Context, targetVersion: Long): Boolean {
    isAvailable = isTrustedModuleInstalled(context)
    if (!isAvailable) {
      isModuleActive = false
      frameworkName = ""
      frameworkVersion = ""
      apiVersion = 0
      publishStatus()
      return true
    }

    val service = bind(context) ?: return false
    val pushed = pushLatestSnapshot(service, targetVersion)
    val statusRead = refreshStatus(service)
    return pushed && statusRead && isModuleActive
  }

  private fun pushLatestSnapshot(service: IXHideSyncService, targetVersion: Long): Boolean {
    val snapshot =
      synchronized(syncLock) { latestSnapshot.takeIf { latestSnapshotVersion == targetVersion } }
        ?: return true
    return runCatching { service.pushSnapshot(snapshot) }
      .onFailure { Log.w(TAG, "Failed to push XHide snapshot", it) }
      .getOrDefault(false)
  }

  private fun refreshStatus(service: IXHideSyncService): Boolean =
    runCatching {
        updateStatus(service.getStatus())
        true
      }
      .onFailure { Log.w(TAG, "Failed to read status", it) }
      .getOrDefault(false)

  private fun updateStatus(status: Bundle) {
    isModuleActive = status.getBoolean(XHideContract.KEY_MODULE_ACTIVE, false)
    frameworkName = status.getString(XHideContract.KEY_FRAMEWORK_NAME, "")
    frameworkVersion = status.getString(XHideContract.KEY_FRAMEWORK_VERSION, "")
    apiVersion = status.getInt(XHideContract.KEY_API_VERSION, 0)
    publishStatus()
  }

  private fun buildSnapshot(
    context: Context,
    hiddenApps: Set<String>,
    xHideEnabled: Boolean,
  ): Bundle =
    Bundle().apply {
      putInt(XHideContract.KEY_PROTOCOL_VERSION, XHideContract.PROTOCOL_VERSION)
      putBoolean(XHideContract.KEY_ENABLED, xHideEnabled)
      putStringArrayList(XHideContract.KEY_HIDDEN_PACKAGES, ArrayList(hiddenApps))
      putString(XHideContract.KEY_MAIN_APP_PACKAGE, context.packageName)
      putLong(XHideContract.KEY_MAIN_APP_VERSION_CODE, packageVersionCode(context))
      putLong(XHideContract.KEY_UPDATED_AT, System.currentTimeMillis())
    }

  private fun publishStatus() {
    _status.value =
      Status(
        isAvailable = isAvailable,
        isModuleActive = isModuleActive,
        frameworkName = frameworkName,
        frameworkVersion = frameworkVersion,
        apiVersion = apiVersion,
      )
  }

  private suspend fun bind(context: Context): IXHideSyncService? =
    withContext(Dispatchers.Main.immediate) {
      kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val intent =
          Intent()
            .setComponent(ComponentName(XHideContract.MODULE_PACKAGE, XHideContract.SERVICE_CLASS))
        lateinit var connection: ServiceConnection
        connection =
          object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
              runCatching { context.unbindService(this) }
              if (continuation.isActive) {
                continuation.resume(IXHideSyncService.Stub.asInterface(service))
              }
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit

            override fun onNullBinding(name: ComponentName?) {
              runCatching { context.unbindService(this) }
              if (continuation.isActive) continuation.resume(null)
            }
          }
        if (!isTrustedModuleInstalled(context)) {
          if (continuation.isActive) continuation.resume(null)
          return@suspendCancellableCoroutine
        }
        val bound =
          runCatching { context.bindService(intent, connection, Context.BIND_AUTO_CREATE) }
            .getOrDefault(false)
        if (!bound && continuation.isActive) continuation.resume(null)
        continuation.invokeOnCancellation { runCatching { context.unbindService(connection) } }
      }
    }

  private fun isTrustedModuleInstalled(context: Context): Boolean =
    runCatching {
        context.packageManager.getPackageInfo(XHideContract.MODULE_PACKAGE, 0)
        context.packageManager.checkSignatures(context.packageName, XHideContract.MODULE_PACKAGE) ==
          PackageManager.SIGNATURE_MATCH
      }
      .getOrDefault(false)

  @Suppress("DEPRECATION")
  private fun packageVersionCode(context: Context): Long =
    runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
          info.longVersionCode
        else info.versionCode.toLong()
      }
      .getOrDefault(0)

  data class Status(
    val isAvailable: Boolean = false,
    val isModuleActive: Boolean = false,
    val frameworkName: String = "",
    val frameworkVersion: String = "",
    val apiVersion: Int = 0,
  )
}
