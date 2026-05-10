package deltazero.amarok

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hjq.window.EasyWindow
import com.hjq.window.draggable.SpringBackDraggable
import deltazero.amarok.core.Hider
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.receivers.ActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class QuickHideService : LifecycleService() {

  private lateinit var panicButton: EasyWindow<*>
  private lateinit var ivPanicButton: ImageView
  private var panicButtonObserversStarted = false

  private lateinit var activityPendingIntent: PendingIntent
  private val settingsRepo: SettingsRepository
    get() = (application as AmarokApplication).settingsRepo

  override fun onCreate() {
    super.onCreate()

    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      )
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

    val actionIntent =
      Intent(this, ActionReceiver::class.java).apply { action = ActionReceiver.ACTION_HIDE }
    activityPendingIntent =
      PendingIntent.getBroadcast(this, 1, actionIntent, PendingIntent.FLAG_IMMUTABLE)
  }

  override fun onDestroy() {
    super.onDestroy()
    cancelPanicButton()
    isServiceRunning = false
    Log.i(TAG, "Service stopped.")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    val notification: Notification =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getText(R.string.quick_hide_notification_title))
        .setContentText(getText(R.string.quick_hide_notification_content))
        .setSmallIcon(R.drawable.ic_paw)
        .setContentIntent(activityPendingIntent)
        .setOngoing(true)
        .build()

    if (Build.VERSION.SDK_INT >= 34) {
      startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
    isServiceRunning = true

    val settings = settingsRepo.settings.value

    panicButton =
      EasyWindow<EasyWindow<*>>(application)
        .setContentView(R.layout.dialog_panic_button)
        .setGravity(Gravity.TOP or Gravity.START)
        .setDraggable(SpringBackDraggable())
        .setOnClickListener(R.id.dialog_iv_panic_button) { _, _ ->
          Hider.processAll(this, Hider.Action.HIDE)
        }

    ivPanicButton = panicButton.findViewById(R.id.dialog_iv_panic_button)
    ivPanicButton.setColorFilter(settings.panicButtonColor, PorterDuff.Mode.SRC_IN)

    restorePanicButtonPosition()

    startPanicButtonObservers()
    updatePanicButton()

    Log.i(TAG, "Service start.")
    return START_STICKY
  }

  private fun updatePanicButton() {
    val settings = settingsRepo.settings.value
    if (!settings.panicButton) {
      cancelPanicButton()
      return
    }

    if (!XXPermissions.isGranted(application, Permission.SYSTEM_ALERT_WINDOW)) {
      Log.w(TAG, "Failed to show PanicButton: Permission denied: SYSTEM_ALERT_WINDOW")
      lifecycleScope.launch(Dispatchers.IO) { settingsRepo.setPanicButton(false) }
      return
    }

    when (Hider.getState()) {
      Hider.State.PROCESSING -> {
        ivPanicButton.setColorFilter(
          application.getColor(com.google.android.material.R.color.design_default_color_error),
          PorterDuff.Mode.SRC_IN,
        )
        ivPanicButton.isEnabled = false
      }
      Hider.State.HIDDEN -> {
        cancelPanicButton()
        ivPanicButton.setColorFilter(settings.panicButtonColor, PorterDuff.Mode.SRC_IN)
        ivPanicButton.isEnabled = true
      }
      Hider.State.VISIBLE -> {
        showPanicButton()
        ivPanicButton.setColorFilter(settings.panicButtonColor, PorterDuff.Mode.SRC_IN)
        ivPanicButton.isEnabled = true
      }
    }
  }

  private fun showPanicButton() {
    if (!panicButton.isShowing) panicButton.show()
  }

  private fun cancelPanicButton() {
    if (::panicButton.isInitialized && panicButton.isShowing) {
      savePanicButtonPosition()
      panicButton.cancel()
    }
  }

  private fun savePanicButtonPosition() {
    val params = panicButton.windowParams
    val screenWidth = resources.displayMetrics.widthPixels
    val y = params.y
    val leftEdge = params.x < screenWidth / 2
    lifecycleScope.launch(Dispatchers.IO) { settingsRepo.setPanicButtonPosition(y, leftEdge) }
  }

  private fun restorePanicButtonPosition() {
    val settings = settingsRepo.settings.value
    panicButton.setYOffset(settings.panicButtonY)
    // Large offset clamps to the right edge; Integer.MAX_VALUE overflows in WindowManager.
    panicButton.setXOffset(if (settings.panicButtonLeftEdge) 0 else 10000)
  }

  private fun startPanicButtonObservers() {
    if (panicButtonObserversStarted) return
    Hider.stateLiveData.observe(this) { updatePanicButton() }
    settingsRepo.settings
      .map { PanicButtonSettings(it.panicButton, it.panicButtonColor) }
      .distinctUntilChanged()
      .asLiveData()
      .observe(this) { updatePanicButton() }
    panicButtonObserversStarted = true
  }

  private data class PanicButtonSettings(val enabled: Boolean, val color: Int)

  companion object {
    private const val TAG = "QuickHideService"
    private const val CHANNEL_ID = "QUICK_HIDE_CHANNEL"
    private const val NOTIFICATION_ID = 1

    var isServiceRunning = false
      private set

    private var initialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun settingsRepo(context: Context) =
      (context.applicationContext as AmarokApplication).settingsRepo

    @JvmStatic
    @MainThread
    fun init(context: Context) {
      if (initialized) return
      val appContext = context.applicationContext
      scope.launch {
        val settingsRepo = settingsRepo(appContext)
        combine(
            settingsRepo.settings.map { it.quickHideService }.distinctUntilChanged(),
            Hider.state,
          ) { enabled, state ->
            enabled to state
          }
          .distinctUntilChanged()
          .collect { (enabled, state) -> sync(appContext, enabled, state) }
      }
      initialized = true
      sync(appContext)
    }

    @JvmStatic
    @MainThread
    fun sync(context: Context) {
      val appContext = context.applicationContext
      val settings = settingsRepo(appContext).settings.value
      sync(appContext, settings.quickHideService, Hider.getState())
    }

    private fun sync(context: Context, quickHideService: Boolean, hiderState: Hider.State) {
      val appContext = context.applicationContext
      val shouldRun = quickHideService && hiderState != Hider.State.HIDDEN
      if (shouldRun && !isServiceRunning) startService(appContext)
      else if (!shouldRun && isServiceRunning) stopService(appContext)
    }

    @JvmStatic
    @MainThread
    fun refresh(context: Context) {
      val appContext = context.applicationContext
      if (isServiceRunning) stopService(appContext)
      sync(appContext)
    }

    @JvmStatic
    @MainThread
    fun startService(context: Context) {
      val appContext = context.applicationContext
      val settings = settingsRepo(appContext).settings.value
      when {
        isServiceRunning -> {
          Log.w(TAG, "Restarting QuickHideService ...")
          stopService(appContext)
          appContext.startForegroundService(Intent(appContext, QuickHideService::class.java))
        }
        !settings.quickHideService ->
          Log.i(TAG, "QuickHideService is disabled. Skip starting service.")
        Hider.getState() == Hider.State.HIDDEN ->
          Log.i(TAG, "Current state is hidden. Skip starting service.")
        !XXPermissions.isGranted(appContext, Permission.NOTIFICATION_SERVICE) -> {
          Log.w(TAG, "Permission denied: NOTIFICATION_SERVICE. Skip starting service.")
        }
        else -> appContext.startForegroundService(Intent(appContext, QuickHideService::class.java))
      }
    }

    @JvmStatic fun isRunning(): Boolean = isServiceRunning

    @JvmStatic
    fun stopService(context: Context) {
      context.applicationContext.stopService(
        Intent(context.applicationContext, QuickHideService::class.java)
      )
    }
  }
}
