package deltazero.amarok.core

import android.content.Context
import androidx.lifecycle.LiveData
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.filehider.FileHiderMode
import kotlinx.coroutines.flow.StateFlow

object Hider {

  private lateinit var controller: HiderController

  @JvmField var initialized = false

  enum class State {
    HIDDEN,
    VISIBLE,
    PROCESSING,
  }

  enum class Action {
    HIDE,
    UNHIDE,
  }

  @JvmStatic
  fun install(controller: HiderController) {
    this.controller = controller
    initialized = controller.initialized
  }

  @get:JvmName("stateFlow")
  val state: StateFlow<State>
    get() = controller.state

  val folderStates: StateFlow<Map<String, State>>
    get() = controller.folderStates

  val appStates: StateFlow<Map<String, State>>
    get() = controller.appStates

  val appHiderError: StateFlow<Int>
    get() = controller.appHiderError

  val fileHiderError: StateFlow<Int>
    get() = controller.fileHiderError

  @JvmStatic val stateLiveData: LiveData<State> by lazy { controller.stateLiveData }

  @JvmStatic
  val appStatesLiveData: LiveData<Map<String, State>> by lazy { controller.appStatesLiveData }

  @JvmStatic
  fun init(context: Context) {
    controller.init(context)
    initialized = controller.initialized
  }

  @JvmStatic fun getState(): State = controller.getState()

  @JvmStatic
  fun processAll(context: Context, action: Action) = controller.processAll(context, action)

  @JvmStatic
  fun processApps(context: Context, pkgNames: Set<String>, action: Action) =
    controller.processApps(context, pkgNames, action)

  @JvmStatic
  fun processFolders(context: Context, paths: Set<String>, action: Action) =
    controller.processFolders(context, paths, action)

  @JvmStatic fun cancelProcess() = controller.cancelProcess()

  @JvmStatic suspend fun switchAppHider(mode: AppHiderMode) = controller.switchAppHider(mode)

  @JvmStatic suspend fun switchFileHider(mode: FileHiderMode) = controller.switchFileHider(mode)
}
