package deltazero.amarok.filehider

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import deltazero.amarok.R
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction
import deltazero.amarok.utils.MediaStoreHelper
import deltazero.amarok.utils.await
import deltazero.amarok.utils.awaitShell

class ChmodFileHider(private val context: Context) : FileHider {
  override val mode = FileHiderMode.CHMOD
  override val name = "Chmod"

  override suspend fun activate(): ActivationResult {
    val shell = awaitShell()
    return if (shell.isRoot) {
      ActivationResult(success = true, msgResId = 0)
    } else {
      ActivationResult(success = false, msgResId = R.string.root_not_ava)
    }
  }

  override suspend fun process(targetDirs: Set<String>, action: HideAction) {
    val hide = action is HideAction.Hide
    val processDirs = mutableSetOf<String>()
    for (d in targetDirs) {
      if (d.startsWith("/storage/emulated/")) {
        processDirs.add(d.replace("/storage/emulated/", "/data/media/"))
      } else {
        Log.w("ChmodFileHider", "Unsupported path: $d")
      }
    }

    val perm = if (hide) "0" else "2770"
    val processJob = Shell.getShell().newJob()
    for (d in processDirs) {
      processJob.add("chmod -R $perm $d")
    }
    processJob.await()
    MediaStoreHelper.scan(context, processDirs)
  }
}
