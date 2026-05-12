package deltazero.amarok.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import deltazero.amarok.utils.AutoHideUtil
import deltazero.amarok.utils.SecurityUtil

@AndroidEntryPoint
class ScreenStatusReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    assert(intent.action != null)
    when (intent.action!!) {
      Intent.ACTION_SCREEN_ON -> {
        Log.i("ScreenStatusReceiver", "Screen unlocked.")
        AutoHideUtil.cancelAutoHide(context)
      }
      Intent.ACTION_SCREEN_OFF -> {
        Log.i("ScreenStatusReceiver", "Screen locked.")
        SecurityUtil.lockAndDisguise()
        AutoHideUtil.setAutoHide(context)
      }
    }
  }
}
