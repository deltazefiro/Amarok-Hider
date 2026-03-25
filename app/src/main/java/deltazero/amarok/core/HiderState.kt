package deltazero.amarok.core

import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.core.Hider.FolderStatus
import deltazero.amarok.core.Hider.State
import deltazero.amarok.filehider.FileHiderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns all mutable state for [Hider]. All mutations must happen on [Dispatchers.Main] (enforced by
 * the caller's coroutine context), so no synchronization is needed.
 */
class HiderState(private val scope: CoroutineScope) {

  // --- Folder states ---

  private val _folderStates = MutableStateFlow<Map<String, FolderStatus>>(emptyMap())
  val folderStates: StateFlow<Map<String, FolderStatus>> = _folderStates.asStateFlow()

  // --- Hidden apps ---

  private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
  val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

  // --- Workmode ---

  private val _appHiderMode = MutableStateFlow(AppHiderMode.NONE)
  val appHiderMode: StateFlow<AppHiderMode> = _appHiderMode.asStateFlow()

  private val _fileHiderMode = MutableStateFlow(FileHiderMode.NONE)
  val fileHiderMode: StateFlow<FileHiderMode> = _fileHiderMode.asStateFlow()

  // --- Errors (string resource ID, 0 = no error) ---

  private val _appHiderError = MutableStateFlow(0)
  val appHiderError: StateFlow<Int> = _appHiderError.asStateFlow()

  private val _fileHiderError = MutableStateFlow(0)
  val fileHiderError: StateFlow<Int> = _fileHiderError.asStateFlow()

  // --- Derived global state ---

  val state: StateFlow<State> =
    combine(_folderStates, _hiddenApps) { fs, ha -> computeState(fs, ha) }
      .stateIn(scope, SharingStarted.Eagerly, State.VISIBLE)

  // --- Initialization ---

  fun init() {
    // Hidden apps
    val initialApps = PrefMgr.getHiddenApps()
    _hiddenApps.value = initialApps

    // Folder states from persisted data
    val managedFolders = PrefMgr.getHideFilePath()
    var hiddenFolders = PrefMgr.getHiddenFolders()
    val legacyIsHidden = PrefMgr.getPrefs().getBoolean("isHidden", false)

    // Legacy migration
    if (hiddenFolders.isEmpty() && managedFolders.isNotEmpty() && legacyIsHidden) {
      hiddenFolders = managedFolders
    }
    if (initialApps.isEmpty() && PrefMgr.getHideApps().isNotEmpty() && legacyIsHidden) {
      _hiddenApps.value = PrefMgr.getHideApps()
    }

    _folderStates.value =
      managedFolders.associateWith { path ->
        if (path in hiddenFolders) FolderStatus.HIDDEN else FolderStatus.VISIBLE
      }

    // Workmode & error
    _appHiderMode.value = AppHiderMode.fromKey(PrefMgr.getAppHiderMode())
    _fileHiderMode.value = FileHiderMode.fromKey(PrefMgr.getFileHiderMode())
    _appHiderError.value = 0
    _fileHiderError.value = 0

    // Persist on change
    scope.launch {
      _folderStates.collect { fs ->
        val hidden = fs.filterValues { it == FolderStatus.HIDDEN }.keys
        PrefMgr.setHiddenFolders(hidden)
      }
    }
    scope.launch { _hiddenApps.collect { apps -> PrefMgr.setHiddenApps(apps) } }
  }

  // --- State computation ---

  private fun computeState(fs: Map<String, FolderStatus>, ha: Set<String>): State {
    if (fs.values.any { it == FolderStatus.PROCESSING }) return State.PROCESSING

    val anyHidden = ha.isNotEmpty() || fs.values.any { it == FolderStatus.HIDDEN }
    if (!anyHidden) return State.VISIBLE

    val allAppsHidden = ha.containsAll(PrefMgr.getHideApps())
    val managedFolders = PrefMgr.getHideFilePath()
    val allFoldersHidden = managedFolders.all { fs[it] == FolderStatus.HIDDEN }

    return if (allAppsHidden && allFoldersHidden) State.HIDDEN else State.VISIBLE
  }

  // --- Mutators (call from Main dispatcher only) ---

  fun updateFolderStatus(path: String, status: FolderStatus) {
    _folderStates.value = _folderStates.value + (path to status)
  }

  fun updateFolderStatuses(paths: Set<String>, status: FolderStatus) {
    _folderStates.value = _folderStates.value + paths.associateWith { status }
  }

  fun clearProcessingFolders() {
    _folderStates.value =
      _folderStates.value.mapValues { (_, v) ->
        if (v == FolderStatus.PROCESSING) FolderStatus.VISIBLE else v
      }
  }

  fun setHiddenApps(apps: Set<String>) {
    _hiddenApps.value = apps
  }

  fun setAppHiderMode(mode: AppHiderMode) {
    PrefMgr.setAppHiderMode(mode.key)
    _appHiderMode.value = mode
  }

  fun setFileHiderMode(mode: FileHiderMode) {
    PrefMgr.setFileHiderMode(mode.key)
    _fileHiderMode.value = mode
  }

  fun setAppHiderError(errorResId: Int) {
    _appHiderError.value = errorResId
  }

  fun setFileHiderError(errorResId: Int) {
    _fileHiderError.value = errorResId
  }
}
