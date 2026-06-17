package deltazero.amarok.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import deltazero.amarok.core.LockTrigger
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.utils.AutoHideUtil
import deltazero.amarok.utils.SecurityUtil

class ScreenStatusReceiver(private val settingsRepo: SettingsRepository) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    assert(intent.action != null)
    when (intent.action!!) {
      Intent.ACTION_SCREEN_ON -> {
        Log.i("ScreenStatusReceiver", "Screen unlocked.")
        AutoHideUtil.cancelAutoHide(context)
      }
      Intent.ACTION_SCREEN_OFF -> {
        Log.i("ScreenStatusReceiver", "Screen locked.")
        SecurityUtil.onTrigger(LockTrigger.SCREEN_OFF, settingsRepo)
        AutoHideUtil.setAutoHide(context)
      }
    }
  }
}
