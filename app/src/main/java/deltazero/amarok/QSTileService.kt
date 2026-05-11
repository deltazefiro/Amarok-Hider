package deltazero.amarok

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import deltazero.amarok.core.Hider
import deltazero.amarok.ui.SecurityAuthForQSActivity
import deltazero.amarok.utils.SecurityUtil

class QSTileService : TileService() {

  override fun onStartListening() {
    Log.i(TAG, "Tile update triggered.")

    val tile = qsTile
    when (Hider.getState()) {
      Hider.State.PROCESSING -> {
        tile.state = Tile.STATE_UNAVAILABLE
        tile.label = getString(R.string.processing)
      }
      Hider.State.VISIBLE -> {
        tile.label = getString(R.string.app_name)
        tile.state = determineTileState(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          tile.stateDescription = getString(R.string.visible_status)
        }
      }
      Hider.State.HIDDEN -> {
        tile.label = getString(R.string.app_name)
        tile.state = determineTileState(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          tile.stateDescription = getString(R.string.hidden_status)
        }
      }
    }

    tile.updateTile()
  }

  override fun onClick() {
    unlockAndRun {
      Log.i(TAG, "Toggled tile.")
      when (Hider.getState()) {
        Hider.State.VISIBLE -> Hider.processAll(this, Hider.Action.HIDE)
        Hider.State.HIDDEN -> {
          if (SecurityUtil.isUnlockRequired(application as AmarokApplication)) {
            startAuthThenUnhide()
          } else {
            Hider.processAll(this, Hider.Action.UNHIDE)
          }
        }
        Hider.State.PROCESSING ->
          throw IllegalStateException("Unexpected value: " + Hider.getState())
      }
    }
  }

  @SuppressLint("StartActivityAndCollapseDeprecated")
  private fun startAuthThenUnhide() {
    val intent =
      Intent(this, SecurityAuthForQSActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startActivityAndCollapse(
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
      )
    } else {
      @Suppress("DEPRECATION") startActivityAndCollapse(intent)
    }
  }

  private fun determineTileState(isHidden: Boolean): Int {
    val invertTileColor =
      (application as AmarokApplication).settingsRepo.settings.value.invertTileColor
    if (invertTileColor) return if (isHidden) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    return if (isHidden) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
  }

  companion object {
    private const val TAG = "TileService"
    @JvmField var initialized = false

    /**
     * The method should be invoked in [AmarokApplication.onCreate], after [Hider.state] is
     * initialized.
     *
     * @param context Application context
     */
    @JvmStatic
    fun init(context: Context) {
      assert(Hider.initialized)
      if (initialized) return

      val appContext = context.applicationContext
      Hider.stateLiveData.observeForever { requestListeningState(appContext) }
      (appContext as AmarokApplication).invertTileColorLiveData.observeForever {
        requestListeningState(appContext)
      }
      initialized = true
    }

    private fun requestListeningState(context: Context) {
      try {
        TileService.requestListeningState(
          context,
          ComponentName(context, QSTileService::class.java),
        )
      } catch (e: IllegalArgumentException) {
        Log.w(TAG, "QuickSetting is unavailable when running in an Android work profile.")
      }
    }
  }
}
