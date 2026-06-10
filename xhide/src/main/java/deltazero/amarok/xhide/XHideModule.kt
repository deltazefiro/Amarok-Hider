package deltazero.amarok.xhide

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class XHideModule : XposedModule() {
  override fun onModuleLoaded(param: ModuleLoadedParam) {
    runCatching { XHideStateStore.attach(getRemotePreferences(XHideContract.REMOTE_PREF_GROUP)) }
      .onFailure { log(Log.ERROR, TAG, "Failed to attach remote preferences", it) }
  }

  override fun onSystemServerStarting(param: SystemServerStartingParam) {
    FilterHooks.load(this, param.classLoader)
  }

  companion object {
    private const val TAG = "Amarok-XHide"
  }
}
