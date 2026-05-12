package deltazero.amarok

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.ui.CalendarActivity
import deltazero.amarok.ui.SecurityAuthActivity
import deltazero.amarok.utils.SecurityUtil
import javax.inject.Inject

@AndroidEntryPoint
open class AmarokActivity : AppCompatActivity() {
  @Inject lateinit var settingsRepo: SettingsRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Enable edge-to-edge display
    enableEdgeToEdge()
  }

  override fun onStart() {
    val settings = settingsRepo.settings.value
    if (settings.blockScreenshots) {
      window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE,
      )
    }

    if (settings.hideFromRecents) {
      val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
      if (am != null) {
        val tasks = am.appTasks
        if (tasks.isNotEmpty()) {
          tasks[0].setExcludeFromRecents(true)
        }
      }
    }

    super.onStart()
  }

  override fun onResume() {
    if (SecurityUtil.isDisguiseNeeded(settingsRepo))
      startActivity(Intent(this, CalendarActivity::class.java))
    else if (SecurityUtil.isUnlockRequired(settingsRepo)) {
      startActivity(Intent(this, SecurityAuthActivity::class.java))
    }
    super.onResume()
  }
}
