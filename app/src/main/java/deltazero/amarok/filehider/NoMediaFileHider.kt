package deltazero.amarok.filehider

import android.content.Context
import android.util.Log
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction
import deltazero.amarok.utils.MediaStoreHelper
import java.nio.file.Paths
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

class NoMediaFileHider(private val context: Context) : FileHider {
  override val mode = FileHiderMode.NOMEDIA
  override val name = "NoMedia"

  override suspend fun activate() = ActivationResult(success = true, msgResId = 0)

  override suspend fun process(targetDirs: Set<String>, action: HideAction) {
    val hide = action is HideAction.Hide
    for (dir in targetDirs) {
      coroutineContext.ensureActive()
      Log.i(TAG, "Processing: $dir")
      try {
        val path = Paths.get(dir)
        val nomediaFile = path.resolve(".nomedia").toFile()
        if (hide) {
          val result = nomediaFile.createNewFile()
          if (!result) Log.w(TAG, ".nomedia already exist: $path")
        } else {
          if (!nomediaFile.isFile) {
            Log.w(TAG, ".nomedia not file: $path")
            continue
          }
          val result = nomediaFile.delete()
          if (!result) Log.w(TAG, "Failed to remove .nomedia: $path")
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error while processing $dir: ", e)
      }
    }
    MediaStoreHelper.scan(context, targetDirs)
  }

  companion object {
    private const val TAG = "NoMediaFileHider"
  }
}
