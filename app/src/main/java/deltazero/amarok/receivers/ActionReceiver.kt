package deltazero.amarok.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import deltazero.amarok.AmarokApplication
import deltazero.amarok.R
import deltazero.amarok.core.Hider
import deltazero.amarok.ui.SecurityAuthForQSActivity
import deltazero.amarok.utils.SecurityUtil

class ActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    Log.i("ActionReceiver", "New action received.")

    if (Hider.getState() == Hider.State.PROCESSING) {
      Log.w("ActionReceiver", "Already processing. Ignore the new action.")
      return
    }

    when (intent.action) {
      ACTION_HIDE -> {
        Hider.processAll(context, Hider.Action.HIDE)
        return
      }
      ACTION_UNHIDE -> {
        if (SecurityUtil.isUnlockRequired(context.applicationContext as AmarokApplication)) {
          context.startActivity(
            Intent(context, SecurityAuthForQSActivity::class.java)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        } else {
          Hider.processAll(context, Hider.Action.UNHIDE)
        }
        return
      }
      ACTION_TOGGLE -> {
        if (Hider.getState() == Hider.State.VISIBLE) {
          Hider.processAll(context, Hider.Action.HIDE)
        } else {
          if (SecurityUtil.isUnlockRequired(context.applicationContext as AmarokApplication)) {
            context.startActivity(
              Intent(context, SecurityAuthForQSActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
          } else {
            Hider.processAll(context, Hider.Action.UNHIDE)
          }
        }
        return
      }
    }

    Log.w("ActionReceiver", "Invalid action: " + intent.action)
    Toast.makeText(
        context,
        context.getString(R.string.invalid_action, intent.action),
        Toast.LENGTH_LONG,
      )
      .show()
  }

  companion object {
    const val ACTION_HIDE = "deltazero.amarok.HIDE"
    const val ACTION_UNHIDE = "deltazero.amarok.UNHIDE"
    const val ACTION_TOGGLE = "deltazero.amarok.TOGGLE"
  }
}
