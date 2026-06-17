package deltazero.amarok

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hjq.window.EasyWindow
import com.hjq.window.draggable.SpringBackDraggable
import dagger.hilt.android.AndroidEntryPoint
import deltazero.amarok.core.Hider
import deltazero.amarok.core.QuickHideController
import deltazero.amarok.core.QuickHideController.PanicButtonState
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.receivers.ActionReceiver
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground host for the floating panic button.
 *
 * This service is intentionally dumb: [QuickHideController] decides when it should exist, and the
 * service merely renders the controller's [QuickHideController.panicButtonState] onto a single,
 * reused overlay. The overlay is created exactly once in [onCreate] and torn down in [onDestroy],
 * so rapid start/stop cycles can never orphan a window.
 */
@AndroidEntryPoint
class QuickHideService : LifecycleService() {

  @Inject lateinit var settingsRepo: SettingsRepository
  @Inject lateinit var quickHideController: QuickHideController

  private lateinit var panicButton: EasyWindow<*>
  private lateinit var ivPanicButton: ImageView
  private lateinit var activityPendingIntent: PendingIntent

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

    createPanicButton()
    quickHideController.onServiceCreated()
    observePanicButtonState()
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

    Log.i(TAG, "Service start.")
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    cancelPanicButton()
    quickHideController.onServiceDestroyed()
    Log.i(TAG, "Service stopped.")
  }

  private fun createPanicButton() {
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
  }

  private fun observePanicButtonState() {
    lifecycleScope.launch { quickHideController.panicButtonState.collect { renderPanicButton(it) } }
  }

  private fun renderPanicButton(state: PanicButtonState) {
    if (!state.enabled) {
      cancelPanicButton()
      return
    }

    if (!XXPermissions.isGranted(application, Permission.SYSTEM_ALERT_WINDOW)) {
      Log.w(TAG, "Failed to show PanicButton: Permission denied: SYSTEM_ALERT_WINDOW")
      lifecycleScope.launch(Dispatchers.IO) { settingsRepo.setPanicButton(false) }
      return
    }

    if (state.processing) {
      ivPanicButton.setColorFilter(
        application.getColor(com.google.android.material.R.color.design_default_color_error),
        PorterDuff.Mode.SRC_IN,
      )
      ivPanicButton.isEnabled = false
    } else {
      ivPanicButton.setColorFilter(state.color, PorterDuff.Mode.SRC_IN)
      ivPanicButton.isEnabled = true
    }
    showPanicButton()
  }

  private fun showPanicButton() {
    if (!panicButton.isShowing) panicButton.show()
  }

  private fun cancelPanicButton() {
    if (panicButton.isShowing) {
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

  private companion object {
    private const val TAG = "QuickHideService"
    private const val CHANNEL_ID = "QUICK_HIDE_CHANNEL"
    private const val NOTIFICATION_ID = 1
  }
}
