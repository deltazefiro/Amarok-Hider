package deltazero.amarok.filehider

import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction

class NoneFileHider : FileHider {
  override val mode = FileHiderMode.NONE
  override val name = "Disabled"

  override suspend fun activate() = ActivationResult(success = true, msgResId = 0)

  override suspend fun process(targetDirs: Set<String>, action: HideAction) {
    // No-op
  }
}
