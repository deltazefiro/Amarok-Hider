package deltazero.amarok.apphider

import android.content.Context
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction

sealed interface AppHider {
  val mode: AppHiderMode
  val name: String

  suspend fun activate(): ActivationResult

  suspend fun process(pkgNames: Set<String>, action: HideAction)

  companion object {
    fun fromMode(context: Context, mode: AppHiderMode): AppHider =
      when (mode) {
        AppHiderMode.NONE -> NoneAppHider()
        AppHiderMode.ROOT -> RootAppHider()
        AppHiderMode.SHIZUKU -> ShizukuAppHider(context)
        AppHiderMode.DHIZUKU -> DhizukuAppHider(context)
      }
  }
}
