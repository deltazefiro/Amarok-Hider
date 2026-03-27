package deltazero.amarok.apphider

import android.content.Context
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.Hider

data class AppHiderOptions(val disableOnly: Boolean = false)

sealed interface AppHider {
  val mode: AppHiderMode
  val name: String

  suspend fun activate(): ActivationResult

  suspend fun process(pkgNames: Set<String>, action: Hider.Action)

  companion object {
    @JvmStatic
    fun build(
      context: Context,
      mode: AppHiderMode,
      options: AppHiderOptions = AppHiderOptions(),
    ): AppHider =
      when (mode) {
        AppHiderMode.NONE -> NoneAppHider()
        AppHiderMode.ROOT -> RootAppHider(options)
        AppHiderMode.SHIZUKU -> ShizukuAppHider(context, options)
        AppHiderMode.DHIZUKU -> DhizukuAppHider(context, options)
      }
  }
}
