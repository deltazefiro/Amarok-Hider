package deltazero.amarok.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import deltazero.amarok.QuickHideService
import deltazero.amarok.R
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.filehider.FileHider
import deltazero.amarok.filehider.FileHiderMode
import deltazero.amarok.utils.SecurityUtil
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

  // Hide / Unhide (all)

  @JvmStatic
  fun hide(context: Context) {
    currentJob =
      scope.launch {
        val appHider = AppHider.fromMode(context, getAppHiderMode())
        val result = appHider.activate()
        if (!result.success) {
          s.setAppHiderError(result.msgResId)
          showErrorToast(context, result.msgResId)
          return@launch
        }
        s.setAppHiderError(0)

        if (PrefMgr.getDisableSecurityWhenUnhidden()) {
          SecurityUtil.unlock()
          SecurityUtil.dismissDisguise()
        }

        process(
          context,
          HideAction.Hide(
            disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()
          ),
        )
      }
  }

  @JvmStatic
  fun unhide(context: Context) {
    currentJob =
      scope.launch {
        val appHider = AppHider.fromMode(context, getAppHiderMode())
        val result = appHider.activate()
        if (!result.success) {
          s.setAppHiderError(result.msgResId)
          showErrorToast(context, result.msgResId)
          return@launch
        }
        s.setAppHiderError(0)

        process(context, HideAction.Unhide)
      }
  }

  private suspend fun process(context: Context, action: HideAction) {
    val hide = action is HideAction.Hide

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
        withContext(Dispatchers.IO) {
          AppHider.fromMode(context, getAppHiderMode()).process(appsToProcess, action)
        }
      }

      val fileHider = FileHider.fromMode(context, getFileHiderMode())
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
    if (hide) QuickHideService.stopService(context) else QuickHideService.startService(context)
  }

  // Individual app hide/unhide

  @JvmStatic
  fun hideApp(context: Context, pkgName: String) {
    val err = s.appHiderError.value
    if (err != 0) {
      Log.w(TAG, "hideApp skipped: app hider in error state")
      showErrorToast(context, err)
      return
    }
    s.setHiddenApps(s.hiddenApps.value + pkgName)

    scope.launch {
      val disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()
      withContext(Dispatchers.IO) {
        AppHider.fromMode(context, getAppHiderMode())
          .process(setOf(pkgName), HideAction.Hide(disableOnly))
      }
    }
  }

  @JvmStatic
  fun unhideApp(context: Context, pkgName: String) {
    val err = s.appHiderError.value
    if (err != 0) {
      Log.w(TAG, "unhideApp skipped: app hider in error state")
      showErrorToast(context, err)
      return
    }
    s.setHiddenApps(s.hiddenApps.value - pkgName)

    scope.launch {
      withContext(Dispatchers.IO) {
        AppHider.fromMode(context, getAppHiderMode()).process(setOf(pkgName), HideAction.Unhide)
      }
    }
  }

  // Individual folder hide/unhide

  @JvmStatic
  fun hideFolder(context: Context, path: String) {
    val err = s.fileHiderError.value
    if (err != 0) {
      Log.w(TAG, "hideFolder skipped: file hider in error state")
      showErrorToast(context, err)
      return
    }
    s.updateFolderStatus(path, FolderStatus.PROCESSING)

    scope.launch {
      try {
        withContext(Dispatchers.IO) {
          FileHider.fromMode(context, getFileHiderMode()).process(setOf(path), HideAction.Hide())
        }
        s.updateFolderStatus(path, FolderStatus.HIDDEN)
      } catch (e: CancellationException) {
        s.updateFolderStatus(path, FolderStatus.VISIBLE)
        throw e
      }
    }
  }

  @JvmStatic
  fun unhideFolder(context: Context, path: String) {
    val err = s.fileHiderError.value
    if (err != 0) {
      Log.w(TAG, "unhideFolder skipped: file hider in error state")
      showErrorToast(context, err)
      return
    }
    s.updateFolderStatus(path, FolderStatus.PROCESSING)

    scope.launch {
      try {
        withContext(Dispatchers.IO) {
          FileHider.fromMode(context, getFileHiderMode()).process(setOf(path), HideAction.Unhide)
        }
        s.updateFolderStatus(path, FolderStatus.VISIBLE)
      } catch (e: CancellationException) {
        s.updateFolderStatus(path, FolderStatus.HIDDEN)
        throw e
      }
    }
  }

  // Force unhide

  @JvmStatic
  fun forceUnhide(context: Context) {
    currentJob?.cancel()
    s.clearProcessingFolders()
    unhide(context)
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
