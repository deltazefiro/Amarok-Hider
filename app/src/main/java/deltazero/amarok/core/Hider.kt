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
import deltazero.amarok.apphider.BaseAppHider
import deltazero.amarok.filehider.BaseFileHider
import deltazero.amarok.utils.SecurityUtil
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
  // @get:JvmName avoids clashes with the @JvmStatic scalar getter functions below.

  @get:JvmName("stateFlow")
  val state: StateFlow<State>
    get() = s.state

  val folderStates: StateFlow<Map<String, FolderStatus>>
    get() = s.folderStates

  val hiddenApps: StateFlow<Set<String>>
    get() = s.hiddenApps

  @get:JvmName("appHiderModeFlow")
  val appHiderMode: StateFlow<Int>
    get() = s.appHiderMode

  @get:JvmName("fileHiderModeFlow")
  val fileHiderMode: StateFlow<Int>
    get() = s.fileHiderMode

  val appHiderError: StateFlow<Int>
    get() = s.appHiderError

  val fileHiderError: StateFlow<Int>
    get() = s.fileHiderError

  // LiveData bridges (Java consumers)

  @JvmStatic val stateLiveData: LiveData<State> by lazy { s.state.asLiveData() }

  @JvmStatic val hiddenAppsLiveData: LiveData<Set<String>> by lazy { s.hiddenApps.asLiveData() }

  // Initialization

  /** Must be invoked in [deltazero.amarok.AmarokApplication.onCreate], after [PrefMgr.init]. */
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
        val appHider = BaseAppHider.fromMode(context, getAppHiderMode())
        val (success, msgResId) = appHider.activate()
        if (!success) {
          s.setAppHiderError(msgResId)
          showErrorToast(context, msgResId)
          return@launch
        }
        s.setAppHiderError(0)

        if (PrefMgr.getDisableSecurityWhenUnhidden()) {
          SecurityUtil.unlock()
          SecurityUtil.dismissDisguise()
        }

        processHide(context)
      }
  }

  private suspend fun processHide(context: Context) {
    val managedApps = PrefMgr.getHideApps()
    val alreadyHiddenApps = s.hiddenApps.value
    val appsToHide = managedApps - alreadyHiddenApps

    val currentStates = s.folderStates.value
    val foldersToHide = currentStates.filterValues { it != FolderStatus.HIDDEN }.keys

    if (foldersToHide.isNotEmpty()) s.updateFolderStatuses(foldersToHide, FolderStatus.PROCESSING)
    if (appsToHide.isNotEmpty()) s.setHiddenApps(managedApps)

    Log.i(TAG, "Process 'hide' start.")
    try {
      if (appsToHide.isNotEmpty()) {
        val disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()
        withContext(Dispatchers.IO) {
          BaseAppHider.fromMode(context, getAppHiderMode()).hide(appsToHide, disableOnly)
        }
      }

      val fileHider = BaseFileHider.fromMode(context, getFileHiderMode())
      for (folder in foldersToHide) {
        withContext(Dispatchers.IO) { fileHider.hide(setOf(folder)) }
        s.updateFolderStatus(folder, FolderStatus.HIDDEN)
      }
    } catch (e: CancellationException) {
      Log.w(TAG, "Process 'hide' cancelled.")
      withContext(NonCancellable) { s.clearProcessingFolders() }
      throw e
    }

    Log.i(TAG, "Process 'hide' finish.")
    if (!PrefMgr.getDisableToasts()) {
      Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show()
    }
    QuickHideService.stopService(context)
  }

  @JvmStatic
  fun unhide(context: Context) {
    currentJob =
      scope.launch {
        val appHider = BaseAppHider.fromMode(context, getAppHiderMode())
        val (success, msgResId) = appHider.activate()
        if (!success) {
          s.setAppHiderError(msgResId)
          showErrorToast(context, msgResId)
          return@launch
        }
        s.setAppHiderError(0)

        processUnhide(context)
      }
  }

  private suspend fun processUnhide(context: Context) {
    val currentlyHiddenApps = s.hiddenApps.value
    val currentStates = s.folderStates.value
    val foldersToUnhide = currentStates.filterValues { it == FolderStatus.HIDDEN }.keys

    if (foldersToUnhide.isNotEmpty())
      s.updateFolderStatuses(foldersToUnhide, FolderStatus.PROCESSING)
    s.setHiddenApps(emptySet())

    Log.i(TAG, "Process 'unhide' start.")
    try {
      if (currentlyHiddenApps.isNotEmpty()) {
        withContext(Dispatchers.IO) {
          BaseAppHider.fromMode(context, getAppHiderMode()).unhide(currentlyHiddenApps)
        }
      }

      val fileHider = BaseFileHider.fromMode(context, getFileHiderMode())
      for (folder in foldersToUnhide) {
        withContext(Dispatchers.IO) { fileHider.unhide(setOf(folder)) }
        s.updateFolderStatus(folder, FolderStatus.VISIBLE)
      }
    } catch (e: CancellationException) {
      Log.w(TAG, "Process 'unhide' cancelled.")
      withContext(NonCancellable) { s.clearProcessingFolders() }
      throw e
    }

    Log.i(TAG, "Process 'unhide' finish.")
    if (!PrefMgr.getDisableToasts()) {
      Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show()
    }
    QuickHideService.startService(context)
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
        BaseAppHider.fromMode(context, getAppHiderMode()).hide(setOf(pkgName), disableOnly)
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
        BaseAppHider.fromMode(context, getAppHiderMode()).unhide(setOf(pkgName))
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
          BaseFileHider.fromMode(context, getFileHiderMode()).hide(setOf(path))
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
          BaseFileHider.fromMode(context, getFileHiderMode()).unhide(setOf(path))
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

  @JvmStatic fun getAppHiderMode(): Int = s.appHiderMode.value

  @JvmStatic
  fun setAppHiderMode(mode: Int) {
    s.setAppHiderMode(mode)
  }

  @JvmStatic
  fun setAppHiderError(errorResId: Int) {
    s.setAppHiderError(errorResId)
  }

  @JvmStatic fun getFileHiderMode(): Int = s.fileHiderMode.value

  @JvmStatic
  fun setFileHiderMode(mode: Int) {
    s.setFileHiderMode(mode)
  }

  @JvmStatic
  fun setFileHiderError(errorResId: Int) {
    s.setFileHiderError(errorResId)
  }

  // tryToActivate bridge

  private data class ActivationResult(val success: Boolean, val msgResId: Int)

  private suspend fun BaseAppHider.activate(): ActivationResult =
    suspendCancellableCoroutine { cont ->
      tryToActivate { _, success, msgResId -> cont.resume(ActivationResult(success, msgResId)) }
    }

  // Utilities

  private fun showErrorToast(context: Context, msgResId: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show()
    }
  }
}
