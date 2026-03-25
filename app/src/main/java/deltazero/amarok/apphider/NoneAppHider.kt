package deltazero.amarok.apphider

import android.util.Log
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction

class NoneAppHider : AppHider {
  override val mode = AppHiderMode.NONE
  override val name = "Disabled"

  override suspend fun activate() = ActivationResult(success = true, msgResId = 0)

  override suspend fun process(pkgNames: Set<String>, action: HideAction) {
    Log.w(
      "AppHider",
      "Skip app ${if (action is HideAction.Hide) "hiding" else "unhiding"}: hider disabled",
    )
  }
}
