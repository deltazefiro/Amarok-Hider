package deltazero.amarok.apphider

import android.util.Log
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.Hider

class NoneAppHider : AppHider {
  override val mode = AppHiderMode.NONE
  override val name = "Disabled"

  override suspend fun activate() = ActivationResult(success = true, msgResId = 0)

  override suspend fun process(pkgNames: Set<String>, action: Hider.Action) {
    Log.w(
      "AppHider",
      "Skip app ${if (action == Hider.Action.HIDE) "hiding" else "unhiding"}: hider disabled",
    )
  }
}
