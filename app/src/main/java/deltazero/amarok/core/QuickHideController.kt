package deltazero.amarok.core

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.QuickHideService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Single reactive authority for the Quick Hide foreground service and its panic button overlay.
 *
 * The service's existence is a derived value of `quickHideService && state != HIDDEN`, not an
 * imperative side effect scattered across callers. This controller is the only code that starts and
 * stops [QuickHideService]; the service is a dumb host that renders [panicButtonState] onto a
 * single reused overlay. The flow is debounced so rapid hide/unhide bursts settle to one decision
 * instead of churning the service (which used to orphan overlay windows), and [reconcile] keys off
 * the actual service liveness reported back via [onServiceCreated]/[onServiceDestroyed].
 *
 * All state below is touched only from the main thread (the collector runs on [Dispatchers.Main],
 * and the service lifecycle callbacks are main-thread).
 */
@Singleton
class QuickHideController
@Inject
constructor(
  @param:ApplicationContext private val appContext: Context,
  private val settingsRepo: SettingsRepository,
  private val hiderController: HiderController,
  private val hiderStateRepo: HiderStateRepository,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var initialized = false

  private var serviceAlive = false
  private var startPending = false
  private var desiredRun = false

  private val _panicButtonState = MutableStateFlow(PanicButtonState())
  val panicButtonState: StateFlow<PanicButtonState> = _panicButtonState.asStateFlow()

  // Whether the user manages any apps or folders. With nothing managed there is nothing to hide, so
  // the service must never run.
  private val anyManaged =
    combine(hiderStateRepo.managedApps, hiderStateRepo.managedFolders) { apps, folders ->
        apps.isNotEmpty() || folders.isNotEmpty()
      }
      .distinctUntilChanged()

  fun init() {
    if (initialized) return
    initialized = true

    // Inputs the running service renders the panic button from.
    scope.launch {
      combine(settingsRepo.settings, hiderController.state) { settings, state ->
          PanicButtonState(
            enabled = settings.panicButton,
            processing = state == Hider.State.PROCESSING,
            color = settings.panicButtonColor,
          )
        }
        .distinctUntilChanged()
        .collect { _panicButtonState.value = it }
    }

    // Sole authority over the service lifecycle. Debounced so toggle bursts settle to one decision.
    scope.launch {
      combine(
          settingsRepo.settings.map { it.quickHideService }.distinctUntilChanged(),
          hiderController.state,
          anyManaged,
        ) { enabled, state, anyManaged ->
          enabled && anyManaged && state != Hider.State.HIDDEN
        }
        .debounce(DEBOUNCE_MS)
        .distinctUntilChanged()
        .collect { run ->
          desiredRun = run
          reconcile()
        }
    }

    // Re-assert when the app returns to the foreground, recovering a service killed under memory
    // pressure while the process survived.
    ProcessLifecycleOwner.get()
      .lifecycle
      .addObserver(
        object : DefaultLifecycleObserver {
          override fun onStart(owner: LifecycleOwner) {
            desiredRun = currentShouldRun()
            reconcile()
          }
        }
      )
  }

  /** Called by [QuickHideService.onCreate]. */
  fun onServiceCreated() {
    serviceAlive = true
    startPending = false
  }

  /** Called by [QuickHideService.onDestroy]. */
  fun onServiceDestroyed() {
    serviceAlive = false
    // The service can be torn down by the system; converge back to the desired state.
    reconcile()
  }

  private fun currentShouldRun(): Boolean {
    val settings = settingsRepo.settings.value
    val anyManaged =
      hiderStateRepo.managedApps.value.isNotEmpty() ||
        hiderStateRepo.managedFolders.value.isNotEmpty()
    return settings.quickHideService &&
      anyManaged &&
      hiderController.state.value != Hider.State.HIDDEN
  }

  private fun reconcile() {
    if (desiredRun) {
      if (!serviceAlive && !startPending) doStart()
    } else {
      if (serviceAlive) doStop()
    }
  }

  private fun doStart() {
    if (!XXPermissions.isGranted(appContext, Permission.NOTIFICATION_SERVICE)) {
      Log.w(TAG, "Notification permission denied. Not starting QuickHideService.")
      return
    }
    startPending = true
    try {
      appContext.startForegroundService(Intent(appContext, QuickHideService::class.java))
    } catch (e: Exception) {
      // e.g. ForegroundServiceStartNotAllowedException when triggered from the background.
      startPending = false
      Log.w(TAG, "Failed to start QuickHideService.", e)
    }
  }

  private fun doStop() {
    appContext.stopService(Intent(appContext, QuickHideService::class.java))
  }

  data class PanicButtonState(
    val enabled: Boolean = false,
    val processing: Boolean = false,
    val color: Int = 0xFFD1D1D1.toInt(),
  )

  private companion object {
    private const val TAG = "QuickHideController"
    private const val DEBOUNCE_MS = 150L
  }
}
