package deltazero.amarok.apphider

import com.topjohnwu.superuser.Shell
import deltazero.amarok.R
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.Hider
import deltazero.amarok.utils.await
import deltazero.amarok.utils.awaitShell

class RootAppHider(private val options: AppHiderOptions) : AppHider {
  override val mode = AppHiderMode.ROOT
  override val name = "Root"

  override suspend fun activate(): ActivationResult {
    val shell = awaitShell()
    return if (shell.isRoot) {
      ActivationResult(success = true, msgResId = 0)
    } else {
      ActivationResult(success = false, msgResId = R.string.root_not_ava)
    }
  }

  override suspend fun process(pkgNames: Set<String>, action: Hider.Action) {
    for (p in pkgNames) {
      when (action) {
        Hider.Action.HIDE -> {
          if (options.disableOnly) {
            Shell.cmd("pm disable $p").await()
          } else {
            Shell.cmd("pm disable $p & pm hide $p").await()
          }
        }
        Hider.Action.UNHIDE -> {
          Shell.cmd("pm unhide $p & pm enable $p").await()
        }
      }
    }
  }
}
