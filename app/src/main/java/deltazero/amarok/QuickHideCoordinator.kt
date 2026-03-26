package deltazero.amarok

import android.content.Context
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr

object QuickHideCoordinator {
  private var initialized = false

  @JvmStatic
  fun init(context: Context) {
    if (initialized) return

    val appContext = context.applicationContext
    Hider.stateLiveData.observeForever { sync(appContext) }
    initialized = true
    sync(appContext)
  }

  @JvmStatic
  fun sync(context: Context) {
    val appContext = context.applicationContext
    val shouldRun = PrefMgr.getEnableQuickHideService() && Hider.getState() != Hider.State.HIDDEN

    when {
      shouldRun && !QuickHideService.isRunning() -> QuickHideService.startService(appContext)
      !shouldRun && QuickHideService.isRunning() -> QuickHideService.stopService(appContext)
    }
  }

  @JvmStatic
  fun refresh(context: Context) {
    val appContext = context.applicationContext
    if (QuickHideService.isRunning()) {
      QuickHideService.stopService(appContext)
    }
    sync(appContext)
  }
}
