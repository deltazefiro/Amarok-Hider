package deltazero.amarok.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
  private val RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L, 5_000L, 15_000L, 30_000L)

  private val _status = MutableStateFlow<XHideStatus>(XHideStatus.NotInstalled)
  val status: StateFlow<XHideStatus> = _status.asStateFlow()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val syncLock = Any()
  private var latestSnapshot: Bundle? = null
  private var latestSnapshotVersion = 0L
  private var syncJob: Job? = null

  fun init(context: Context) {
    val app = context.applicationContext
    if (!isModuleInstalled(app)) {
      _status.value = XHideStatus.NotInstalled
      return
    }
    _status.value = XHideStatus.NotActivated
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

  fun refresh(context: Context) {
    scheduleSync(context.applicationContext, restart = true)
  }

  private fun scheduleSync(context: Context, restart: Boolean = false) {
    synchronized(syncLock) {
      if (restart) syncJob?.cancel()
      if (syncJob?.isActive == true) return
      syncJob = scope.launch { syncUntilReady(context.applicationContext) }
    }
  }

  private suspend fun syncUntilReady(context: Context) {
    for (retryDelay in RETRY_DELAYS_MS) {
      val targetVersion = synchronized(syncLock) { latestSnapshotVersion }
      val success = trySyncOnce(context, targetVersion)
      if (success && synchronized(syncLock) { latestSnapshotVersion == targetVersion }) return
      delay(retryDelay)
    }

    val targetVersion = synchronized(syncLock) { latestSnapshotVersion }
    trySyncOnce(context, targetVersion)
  }

  private suspend fun trySyncOnce(context: Context, targetVersion: Long): Boolean {
    if (!isModuleInstalled(context)) {
      _status.value = XHideStatus.NotInstalled
      return true
    }

    val service = bind(context)
    if (service == null) {
      if (!isModuleInstalled(context)) _status.value = XHideStatus.NotInstalled
      return false
    }
    val pushed = pushLatestSnapshot(service, targetVersion)
    if (!refreshStatus(service) && !isModuleInstalled(context))
      _status.value = XHideStatus.NotInstalled
    return pushed
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
    _status.value =
      deriveXHideStatus(
        installed = true,
        moduleActive = status.getBoolean(XHideContract.KEY_MODULE_ACTIVE, false),
        moduleProtocol = status.getInt(XHideContract.KEY_PROTOCOL_VERSION, 0),
        appProtocol = XHideContract.PROTOCOL_VERSION,
        apiVersion = status.getInt(XHideContract.KEY_API_VERSION, 0),
        frameworkName = status.getString(XHideContract.KEY_FRAMEWORK_NAME, ""),
        frameworkVersion = status.getString(XHideContract.KEY_FRAMEWORK_VERSION, ""),
        lastSyncTime = status.getLong(XHideContract.KEY_LAST_SYNC_TIME, 0),
        hooksLive = status.getBoolean(XHideContract.KEY_HOOKS_LIVE, false),
        hookCount = status.getInt(XHideContract.KEY_HOOK_COUNT, 0),
        hookError = status.getString(XHideContract.KEY_HOOK_ERROR, ""),
      )
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
        if (!isModuleInstalled(context)) {
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

  private fun isModuleInstalled(context: Context): Boolean =
    runCatching {
        context.packageManager.getPackageInfo(XHideContract.MODULE_PACKAGE, 0)
        true
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
}

internal fun deriveXHideStatus(
  installed: Boolean,
  moduleActive: Boolean,
  moduleProtocol: Int,
  appProtocol: Int,
  apiVersion: Int,
  frameworkName: String,
  frameworkVersion: String,
  lastSyncTime: Long,
  hooksLive: Boolean,
  hookCount: Int,
  hookError: String,
): XHideStatus =
  when {
    !installed -> XHideStatus.NotInstalled
    !moduleActive -> XHideStatus.NotActivated
    moduleProtocol != appProtocol -> XHideStatus.Incompatible(moduleProtocol, appProtocol)
    !hooksLive -> XHideStatus.PendingReboot
    hookError.isNotEmpty() || hookCount == 0 ->
      XHideStatus.Error(hookError.ifEmpty { "No hooks attached" })
    else ->
      XHideStatus.Active(
        apiVersion = apiVersion,
        frameworkName = frameworkName,
        frameworkVersion = frameworkVersion,
        lastSyncTime = lastSyncTime,
      )
  }
