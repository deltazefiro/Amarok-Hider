package deltazero.amarok.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import deltazero.amarok.QuickHideService
import deltazero.amarok.R
import deltazero.amarok.apphider.BaseAppHider
import deltazero.amarok.filehider.BaseFileHider
import deltazero.amarok.utils.SecurityUtil
import java.util.concurrent.atomic.AtomicReference

object Hider {

  private const val TAG = "Hider"
  private val hiderThread = HandlerThread("HIDER_THREAD").apply { start() }
  private val threadHandler = Handler(hiderThread.looper)

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

  // --- Per-item state ---
  // AtomicReferences are the source of truth (safe to read from any thread).
  // LiveData is posted for UI observation.

  private val folderStatesRef = AtomicReference<Map<String, FolderStatus>>(emptyMap())
  private val _folderStates = MutableLiveData<Map<String, FolderStatus>>()
  @JvmField val folderStates: LiveData<Map<String, FolderStatus>> = _folderStates

  private val hiddenAppsRef = AtomicReference<Set<String>>(emptySet())
  private val _hiddenApps = MutableLiveData<Set<String>>()
  @JvmField val hiddenApps: LiveData<Set<String>> = _hiddenApps

  // --- Derived global state (auto-recomputed via MediatorLiveData) ---

  private val _state = MediatorLiveData<State>()
  @JvmField val state: LiveData<State> = _state

  // --- Workmode & error ---

  private val _appHiderMode = MutableLiveData<Int>()
  @JvmField val appHiderMode: LiveData<Int> = _appHiderMode

  private val _fileHiderMode = MutableLiveData<Int>()
  @JvmField val fileHiderMode: LiveData<Int> = _fileHiderMode

  /** Error string resource ID from the last failed tryToActivate, 0 means no error. */
  private val _appHiderError = MutableLiveData(0)
  @JvmField val appHiderError: LiveData<Int> = _appHiderError

  /** Error string resource ID from the last failed tryToActivate, 0 means no error. */
  private val _fileHiderError = MutableLiveData(0)
  @JvmField val fileHiderError: LiveData<Int> = _fileHiderError

  /** Must be invoked in [deltazero.amarok.AmarokApplication.onCreate], after [PrefMgr.init]. */
  @JvmStatic
  fun init() {
    assert(PrefMgr.initialized)

    // Initialize hidden apps
    val initialApps = PrefMgr.getHiddenApps()
    hiddenAppsRef.set(initialApps)
    _hiddenApps.value = initialApps

    // Build initial folder states from persisted data
    val managedFolders = PrefMgr.getHideFilePath()
    var hiddenFolders = PrefMgr.getHiddenFolders()
    val legacyIsHidden = PrefMgr.getPrefs().getBoolean("isHidden", false)

    // Legacy migration
    if (hiddenFolders.isEmpty() && managedFolders.isNotEmpty() && legacyIsHidden) {
      hiddenFolders = managedFolders
    }
    if (initialApps.isEmpty() && PrefMgr.getHideApps().isNotEmpty() && legacyIsHidden) {
      val migrated = PrefMgr.getHideApps()
      hiddenAppsRef.set(migrated)
      _hiddenApps.value = migrated
    }

    val initialStates =
      managedFolders.associateWith { path ->
        if (path in hiddenFolders) FolderStatus.HIDDEN else FolderStatus.VISIBLE
      }
    folderStatesRef.set(initialStates)
    _folderStates.value = initialStates

    // Workmode & error
    _appHiderMode.value = PrefMgr.getAppHiderMode()
    _fileHiderMode.value = PrefMgr.getFileHiderMode()
    _appHiderError.value = 0
    _fileHiderError.value = 0

    // Derived global state — auto-recomputes when folderStates or hiddenApps change
    _state.addSource(_folderStates) { _state.value = computeState() }
    _state.addSource(_hiddenApps) { _state.value = computeState() }
    _state.value = computeState()

    // Centralized persistence
    _folderStates.observeForever { fs ->
      val hidden = fs.filterValues { it == FolderStatus.HIDDEN }.keys
      PrefMgr.setHiddenFolders(hidden)
    }
    _hiddenApps.observeForever { apps -> PrefMgr.setHiddenApps(apps ?: HashSet()) }

    initialized = true
  }

  @JvmStatic fun getState(): State = _state.value!!

  private fun computeState(): State {
    val fs = folderStatesRef.get()
    if (fs.values.any { it == FolderStatus.PROCESSING }) return State.PROCESSING

    val ha = hiddenAppsRef.get()
    val anyHidden = ha.isNotEmpty() || fs.values.any { it == FolderStatus.HIDDEN }
    if (!anyHidden) return State.VISIBLE

    val allAppsHidden = ha.containsAll(PrefMgr.getHideApps())
    val managedFolders = PrefMgr.getHideFilePath()
    val allFoldersHidden = managedFolders.all { fs[it] == FolderStatus.HIDDEN }

    return if (allAppsHidden && allFoldersHidden) State.HIDDEN else State.VISIBLE
  }

  // --- State update helpers ---
  // Always update AtomicReference first, then postValue to LiveData.

  private fun updateFolderStatus(path: String, status: FolderStatus) {
    val updated = HashMap(folderStatesRef.get())
    updated[path] = status
    folderStatesRef.set(updated)
    _folderStates.postValue(updated)
  }

  private fun updateFolderStatuses(paths: Set<String>, status: FolderStatus) {
    val updated = HashMap(folderStatesRef.get())
    for (p in paths) updated[p] = status
    folderStatesRef.set(updated)
    _folderStates.postValue(updated)
  }

  private fun clearProcessingFolders() {
    val updated = HashMap(folderStatesRef.get())
    for ((k, v) in updated) {
      if (v == FolderStatus.PROCESSING) updated[k] = FolderStatus.VISIBLE
    }
    folderStatesRef.set(updated)
    _folderStates.postValue(updated)
  }

  private fun updateHiddenApps(apps: Set<String>) {
    hiddenAppsRef.set(apps)
    _hiddenApps.postValue(apps)
  }

  // --- Hide / Unhide (all) ---

  @JvmStatic
  fun hide(context: Context) {
    BaseAppHider.fromMode(context, getAppHiderMode()).tryToActivate { _, succeed, msg ->
      if (succeed) {
        _appHiderError.postValue(0)
        processHide(context)
      } else {
        _appHiderError.postValue(msg)
        showErrorToast(context, msg)
      }
    }

    if (PrefMgr.getDisableSecurityWhenUnhidden()) {
      SecurityUtil.unlock()
      SecurityUtil.dismissDisguise()
    }
  }

  private fun processHide(context: Context) {
    val managedApps = PrefMgr.getHideApps()
    val alreadyHiddenApps = hiddenAppsRef.get()
    val appsToHide = managedApps - alreadyHiddenApps

    val currentStates = folderStatesRef.get()
    val foldersToHide = currentStates.filterValues { it != FolderStatus.HIDDEN }.keys

    if (foldersToHide.isNotEmpty()) updateFolderStatuses(foldersToHide, FolderStatus.PROCESSING)
    if (appsToHide.isNotEmpty()) updateHiddenApps(managedApps)

    threadHandler.post {
      Log.i(TAG, "Process 'hide' start.")
      try {
        if (appsToHide.isNotEmpty()) {
          val disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()
          BaseAppHider.fromMode(context, getAppHiderMode()).hide(appsToHide, disableOnly)
        }

        val fileHider = BaseFileHider.fromMode(context, getFileHiderMode())
        for (folder in foldersToHide) {
          fileHider.hide(setOf(folder))
          updateFolderStatus(folder, FolderStatus.HIDDEN)
        }
      } catch (e: InterruptedException) {
        Log.w(TAG, "Process 'hide' interrupted.")
        clearProcessingFolders()
        return@post
      }

      Log.i(TAG, "Process 'hide' finish.")
      if (!PrefMgr.getDisableToasts()) {
        Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show()
      }
      QuickHideService.stopService(context)
    }
  }

  @JvmStatic
  fun unhide(context: Context) {
    BaseAppHider.fromMode(context, getAppHiderMode()).tryToActivate { _, succeed, msg ->
      if (succeed) {
        _appHiderError.postValue(0)
        processUnhide(context)
      } else {
        _appHiderError.postValue(msg)
        showErrorToast(context, msg)
      }
    }
  }

  private fun processUnhide(context: Context) {
    val currentlyHiddenApps = hiddenAppsRef.get()
    val currentStates = folderStatesRef.get()
    val foldersToUnhide = currentStates.filterValues { it == FolderStatus.HIDDEN }.keys

    if (foldersToUnhide.isNotEmpty()) updateFolderStatuses(foldersToUnhide, FolderStatus.PROCESSING)
    updateHiddenApps(HashSet())

    threadHandler.post {
      Log.i(TAG, "Process 'unhide' start.")
      try {
        if (currentlyHiddenApps.isNotEmpty()) {
          BaseAppHider.fromMode(context, getAppHiderMode()).unhide(currentlyHiddenApps)
        }

        val fileHider = BaseFileHider.fromMode(context, getFileHiderMode())
        for (folder in foldersToUnhide) {
          fileHider.unhide(setOf(folder))
          updateFolderStatus(folder, FolderStatus.VISIBLE)
        }
      } catch (e: InterruptedException) {
        Log.w(TAG, "Process 'unhide' interrupted.")
        clearProcessingFolders()
        return@post
      }

      Log.i(TAG, "Process 'unhide' finish.")
      if (!PrefMgr.getDisableToasts()) {
        Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show()
      }
      Handler(Looper.getMainLooper()).post { QuickHideService.startService(context) }
    }
  }

  // --- Individual app hide/unhide ---

  @JvmStatic
  fun hideApp(context: Context, pkgName: String) {
    val err = _appHiderError.value ?: 0
    if (err != 0) {
      Log.w(TAG, "hideApp skipped: app hider in error state")
      showErrorToast(context, err)
      return
    }
    val updated = HashSet(hiddenAppsRef.get())
    updated.add(pkgName)
    updateHiddenApps(updated)

    threadHandler.post {
      val disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()
      BaseAppHider.fromMode(context, getAppHiderMode()).hide(setOf(pkgName), disableOnly)
    }
  }

  @JvmStatic
  fun unhideApp(context: Context, pkgName: String) {
    val err = _appHiderError.value ?: 0
    if (err != 0) {
      Log.w(TAG, "unhideApp skipped: app hider in error state")
      showErrorToast(context, err)
      return
    }
    val updated = HashSet(hiddenAppsRef.get())
    updated.remove(pkgName)
    updateHiddenApps(updated)

    threadHandler.post { BaseAppHider.fromMode(context, getAppHiderMode()).unhide(setOf(pkgName)) }
  }

  // --- Individual folder hide/unhide ---

  @JvmStatic
  fun hideFolder(context: Context, path: String) {
    val err = _fileHiderError.value ?: 0
    if (err != 0) {
      Log.w(TAG, "hideFolder skipped: file hider in error state")
      showErrorToast(context, err)
      return
    }
    updateFolderStatus(path, FolderStatus.PROCESSING)

    threadHandler.post {
      try {
        BaseFileHider.fromMode(context, getFileHiderMode()).hide(setOf(path))
      } catch (e: InterruptedException) {
        Log.w(TAG, "hideFolder interrupted")
      }
      updateFolderStatus(path, FolderStatus.HIDDEN)
    }
  }

  @JvmStatic
  fun unhideFolder(context: Context, path: String) {
    val err = _fileHiderError.value ?: 0
    if (err != 0) {
      Log.w(TAG, "unhideFolder skipped: file hider in error state")
      showErrorToast(context, err)
      return
    }
    updateFolderStatus(path, FolderStatus.PROCESSING)

    threadHandler.post {
      try {
        BaseFileHider.fromMode(context, getFileHiderMode()).unhide(setOf(path))
      } catch (e: InterruptedException) {
        Log.w(TAG, "unhideFolder interrupted")
      }
      updateFolderStatus(path, FolderStatus.VISIBLE)
    }
  }

  // --- Force unhide ---

  @JvmStatic
  fun forceUnhide(context: Context) {
    if (_state.value == State.PROCESSING) hiderThread.interrupt()
    clearProcessingFolders()
    unhide(context)
  }

  // --- Workmode getters/setters ---

  @JvmStatic fun getAppHiderMode(): Int = _appHiderMode.value!!

  @JvmStatic
  fun setAppHiderMode(mode: Int) {
    PrefMgr.setAppHiderMode(mode)
    _appHiderMode.postValue(mode)
  }

  @JvmStatic
  fun setAppHiderError(errorResId: Int) {
    _appHiderError.postValue(errorResId)
  }

  @JvmStatic fun getFileHiderMode(): Int = _fileHiderMode.value!!

  @JvmStatic
  fun setFileHiderMode(mode: Int) {
    PrefMgr.setFileHiderMode(mode)
    _fileHiderMode.postValue(mode)
  }

  @JvmStatic
  fun setFileHiderError(errorResId: Int) {
    _fileHiderError.postValue(errorResId)
  }

  private fun showErrorToast(context: Context, msgResId: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show()
    }
  }
}
