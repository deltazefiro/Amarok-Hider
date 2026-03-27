package deltazero.amarok.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import deltazero.amarok.R
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.apphider.AppHiderOptions
import deltazero.amarok.filehider.FileHider
import deltazero.amarok.filehider.FileHiderMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Hider {

  private const val TAG = "Hider"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private lateinit var s: HiderState
  private var currentJob: Job? = null

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

  enum class FolderStatus {
    VISIBLE,
    HIDDEN,
    PROCESSING,
  }

  // StateFlow accessors (Kotlin consumers)

  @get:JvmName("stateFlow")
  val state: StateFlow<State>
    get() = s.state

  val folderStates: StateFlow<Map<String, FolderStatus>>
    get() = s.folderStates

  val hiddenApps: StateFlow<Set<String>>
    get() = s.hiddenApps

  @get:JvmName("appHiderModeFlow")
  val appHiderMode: StateFlow<AppHiderMode>
    get() = s.appHiderMode

  @get:JvmName("fileHiderModeFlow")
  val fileHiderMode: StateFlow<FileHiderMode>
    get() = s.fileHiderMode

  val appHiderError: StateFlow<Int>
    get() = s.appHiderError

  val fileHiderError: StateFlow<Int>
    get() = s.fileHiderError

  // LiveData bridges (Java consumers)

  @JvmStatic val stateLiveData: LiveData<State> by lazy { s.state.asLiveData() }

  @JvmStatic val hiddenAppsLiveData: LiveData<Set<String>> by lazy { s.hiddenApps.asLiveData() }

  // Initialization

  @JvmStatic
  fun init() {
    assert(PrefMgr.initialized)
    s = HiderState(scope)
    s.init()
    initialized = true
  }

  @JvmStatic fun getState(): State = s.state.value

  // Process managed items

  private fun buildAppHider(context: Context): AppHider =
    AppHider.build(
      context = context,
      mode = getAppHiderMode(),
      options =
        AppHiderOptions(disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()),
    )

  /** Activate the app hider, returning true on success. Sets error state and toasts on failure. */
  private suspend fun activateAppHider(context: Context): Boolean {
    val result = buildAppHider(context).activate()
    if (!result.success) {
      s.setAppHiderError(result.msgResId)
      showErrorToast(context, result.msgResId)
      return false
    }
    s.setAppHiderError(0)
    return true
  }

  @JvmStatic
  fun processAll(context: Context, action: Action) {
    currentJob =
      scope.launch {
        if (!activateAppHider(context)) return@launch
        processAllInternal(context, action)
      }
  }

  private suspend fun processAllInternal(context: Context, action: Action) {
    val hide = action == Action.HIDE

    val managedApps = PrefMgr.getHideApps()
    val currentStates = s.folderStates.value

    val appsToProcess: Set<String>
    val foldersToProcess: Set<String>

    if (hide) {
      val alreadyHiddenApps = s.hiddenApps.value
      appsToProcess = managedApps - alreadyHiddenApps
      foldersToProcess = currentStates.filterValues { it != FolderStatus.HIDDEN }.keys
      if (foldersToProcess.isNotEmpty())
        s.updateFolderStatuses(foldersToProcess, FolderStatus.PROCESSING)
      if (appsToProcess.isNotEmpty()) s.setHiddenApps(managedApps)
    } else {
      appsToProcess = s.hiddenApps.value
      foldersToProcess = currentStates.filterValues { it == FolderStatus.HIDDEN }.keys
      if (foldersToProcess.isNotEmpty())
        s.updateFolderStatuses(foldersToProcess, FolderStatus.PROCESSING)
      s.setHiddenApps(emptySet())
    }

    Log.i(TAG, "Process '${if (hide) "hide" else "unhide"}' start.")
    try {
      if (appsToProcess.isNotEmpty()) {
        withContext(Dispatchers.IO) { buildAppHider(context).process(appsToProcess, action) }
      }

      val fileHider = FileHider.build(context, getFileHiderMode())
      val targetStatus = if (hide) FolderStatus.HIDDEN else FolderStatus.VISIBLE
      for (folder in foldersToProcess) {
        withContext(Dispatchers.IO) { fileHider.process(setOf(folder), action) }
        s.updateFolderStatus(folder, targetStatus)
      }
    } catch (e: CancellationException) {
      Log.w(TAG, "Process cancelled.")
      withContext(NonCancellable) { s.clearProcessingFolders() }
      throw e
    }

    Log.i(TAG, "Process '${if (hide) "hide" else "unhide"}' finish.")
    if (!PrefMgr.getDisableToasts()) {
      val msgRes = if (hide) R.string.hidden_toast else R.string.unhidden_toast
      Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show()
    }
  }

  // Process apps

  @JvmStatic
  fun processApps(context: Context, pkgNames: Set<String>, action: Action) {
    if (pkgNames.isEmpty()) return

    val err = s.appHiderError.value
    if (err != 0) {
      Log.w(TAG, "processApps skipped: app hider in error state")
      showErrorToast(context, err)
      return
    }

    val hide = action == Action.HIDE
    s.setHiddenApps(if (hide) s.hiddenApps.value + pkgNames else s.hiddenApps.value - pkgNames)

    scope.launch {
      withContext(Dispatchers.IO) { buildAppHider(context).process(pkgNames, action) }
    }
  }

  // Process folders

  @JvmStatic
  fun processFolders(context: Context, paths: Set<String>, action: Action) {
    if (paths.isEmpty()) return

    val err = s.fileHiderError.value
    if (err != 0) {
      Log.w(TAG, "processFolders skipped: file hider in error state")
      showErrorToast(context, err)
      return
    }
    val hide = action == Action.HIDE
    s.updateFolderStatuses(paths, FolderStatus.PROCESSING)

    val doneStatus = if (hide) FolderStatus.HIDDEN else FolderStatus.VISIBLE
    val rollbackStatus = if (hide) FolderStatus.VISIBLE else FolderStatus.HIDDEN

    scope.launch {
      try {
        withContext(Dispatchers.IO) {
          FileHider.build(context, getFileHiderMode()).process(paths, action)
        }
        s.updateFolderStatuses(paths, doneStatus)
      } catch (e: CancellationException) {
        s.updateFolderStatuses(paths, rollbackStatus)
        throw e
      }
    }
  }

  // Force unhide

  @JvmStatic
  fun forceUnhide(context: Context) {
    currentJob?.cancel()
    s.clearProcessingFolders()
    processAll(context, Action.UNHIDE)
  }

  // Workmode getters/setters

  @JvmStatic fun getAppHiderMode(): AppHiderMode = s.appHiderMode.value

  @JvmStatic
  fun setAppHiderMode(mode: AppHiderMode) {
    s.setAppHiderMode(mode)
  }

  @JvmStatic
  fun setAppHiderError(errorResId: Int) {
    s.setAppHiderError(errorResId)
  }

  @JvmStatic fun getFileHiderMode(): FileHiderMode = s.fileHiderMode.value

  @JvmStatic
  fun setFileHiderMode(mode: FileHiderMode) {
    s.setFileHiderMode(mode)
  }

  @JvmStatic
  fun setFileHiderError(errorResId: Int) {
    s.setFileHiderError(errorResId)
  }

  // Utilities

  private fun showErrorToast(context: Context, msgResId: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show()
    }
  }
}
