package deltazero.amarok.filehider

import android.content.Context
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction

sealed interface FileHider {
  val mode: FileHiderMode
  val name: String

  suspend fun activate(): ActivationResult

  suspend fun process(targetDirs: Set<String>, action: HideAction)

  companion object {
    fun fromMode(context: Context, mode: FileHiderMode): FileHider =
      when (mode) {
        FileHiderMode.NONE -> NoneFileHider()
        FileHiderMode.OBFUSCATE -> ObfuscateFileHider(context)
        FileHiderMode.NOMEDIA -> NoMediaFileHider(context)
        FileHiderMode.CHMOD -> ChmodFileHider(context)
      }
  }
}
