package deltazero.amarok.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import deltazero.amarok.ui.MainActivity

class DialerReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val launchIntent = Intent(context, MainActivity::class.java)
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launchIntent)
  }
}
