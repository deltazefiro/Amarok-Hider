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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Hider {

  private const val TAG = "Hider"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var currentJob: Job? = null
  private lateinit var appHider: AppHider
  private lateinit var fileHider: FileHider

  private val _folderStates = MutableStateFlow<Map<String, State>>(emptyMap())
  private val _appStates = MutableStateFlow<Map<String, State>>(emptyMap())
  private val _appHiderMode = MutableStateFlow(AppHiderMode.NONE)
  private val _fileHiderMode = MutableStateFlow(FileHiderMode.NONE)
  private val _appHiderError = MutableStateFlow(0)
  private val _fileHiderError = MutableStateFlow(0)

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

  // StateFlow accessors (Kotlin consumers)

  @get:JvmName("stateFlow")
  val state: StateFlow<State>
    get() = _state

  val folderStates: StateFlow<Map<String, State>>
    get() = _folderStates.asStateFlow()

  val appStates: StateFlow<Map<String, State>>
    get() = _appStates.asStateFlow()

  @get:JvmName("appHiderModeFlow")
  val appHiderMode: StateFlow<AppHiderMode>
    get() = _appHiderMode.asStateFlow()

  @get:JvmName("fileHiderModeFlow")
  val fileHiderMode: StateFlow<FileHiderMode>
    get() = _fileHiderMode.asStateFlow()

  val appHiderError: StateFlow<Int>
    get() = _appHiderError.asStateFlow()

  val fileHiderError: StateFlow<Int>
    get() = _fileHiderError.asStateFlow()

  private val _state: StateFlow<State> =
    combine(_folderStates, _appStates) { folderStates, appStates ->
        computeState(folderStates, appStates)
      }
      .stateIn(scope, SharingStarted.Eagerly, State.VISIBLE)

  // LiveData bridges (Java consumers)

  @JvmStatic val stateLiveData: LiveData<State> by lazy { _state.asLiveData() }

  @JvmStatic val appStatesLiveData: LiveData<Map<String, State>> by lazy { _appStates.asLiveData() }

  // Initialization

  @JvmStatic
  fun init(context: Context) {
    assert(PrefMgr.initialized)
    if (initialized) return
    loadPersistedState()
    startPersistence()
    appHider = buildAppHider(context, _appHiderMode.value)
    fileHider = buildFileHider(context, _fileHiderMode.value)
    initialized = true
    scope.launch { activateAppHider(context, showToast = false) }
    scope.launch { activateFileHider(context, showToast = false) }
  }

  @JvmStatic fun getState(): State = _state.value

  // Process managed items

  @JvmStatic
  fun processAll(context: Context, action: Action) {
    launchTrackedProcess("processAll") {
      if (!activateAppHider(context)) return@launchTrackedProcess
      if (!activateFileHider(context)) return@launchTrackedProcess
      val hide = action == Action.HIDE
      val managedApps = PrefMgr.getHideApps()
      val managedFolders = PrefMgr.getHideFilePath()
      val currentAppStates = _appStates.value
      val currentFolderStates = _folderStates.value

      val appsToProcess =
        managedApps.filterTo(mutableSetOf()) { pkgName ->
          val state = currentAppStates[pkgName] ?: State.VISIBLE
          if (hide) state != State.HIDDEN else state == State.HIDDEN
        }
      val foldersToProcess =
        managedFolders.filterTo(mutableSetOf()) { path ->
          val state = currentFolderStates[path] ?: State.VISIBLE
          if (hide) state != State.HIDDEN else state == State.HIDDEN
        }

      Log.i(TAG, "Process '${if (hide) "hide" else "unhide"}' start.")
      processTargets(_appStates, appsToProcess, action) {
        withContext(Dispatchers.IO) { appHider.process(appsToProcess, action) }
      }
      processTargets(_folderStates, foldersToProcess, action) {
        withContext(Dispatchers.IO) { fileHider.process(foldersToProcess, action) }
      }

      Log.i(TAG, "Process '${if (hide) "hide" else "unhide"}' finish.")
      if (!PrefMgr.getDisableToasts()) {
        val msgRes = if (hide) R.string.hidden_toast else R.string.unhidden_toast
        Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show()
      }
    }
  }

  @JvmStatic
  fun processApps(context: Context, pkgNames: Set<String>, action: Action) {
    if (pkgNames.isEmpty()) return
    launchTrackedProcess("processApps") {
      if (!activateAppHider(context)) return@launchTrackedProcess
      processTargets(_appStates, pkgNames, action) {
        withContext(Dispatchers.IO) { appHider.process(pkgNames, action) }
      }
    }
  }

  @JvmStatic
  fun processFolders(context: Context, paths: Set<String>, action: Action) {
    if (paths.isEmpty()) return
    launchTrackedProcess("processFolders") {
      if (!activateFileHider(context)) return@launchTrackedProcess
      processTargets(_folderStates, paths, action) {
        withContext(Dispatchers.IO) { fileHider.process(paths, action) }
      }
    }
  }

  @JvmStatic
  fun cancelProcess() {
    currentJob?.cancel()
  }

  @JvmStatic
  suspend fun switchAppHider(context: Context, mode: AppHiderMode): ActivationResult {
    PrefMgr.setAppHiderMode(mode.key)
    _appHiderMode.value = mode
    appHider = buildAppHider(context, mode)
    val result = activateAppHider(context, showToast = false)
    return if (result) ActivationResult(true, 0) else ActivationResult(false, _appHiderError.value)
  }

  @JvmStatic
  suspend fun switchFileHider(context: Context, mode: FileHiderMode): ActivationResult {
    PrefMgr.setFileHiderMode(mode.key)
    _fileHiderMode.value = mode
    fileHider = buildFileHider(context, mode)
    val result = activateFileHider(context, showToast = false)
    return if (result) ActivationResult(true, 0) else ActivationResult(false, _fileHiderError.value)
  }

  private suspend fun activateAppHider(context: Context, showToast: Boolean = true): Boolean {
    val result = withContext(Dispatchers.IO) { appHider.activate() }
    if (!result.success) {
      _appHiderError.value = result.msgResId
      if (showToast) showErrorToast(context, result.msgResId)
      return false
    }
    _appHiderError.value = 0
    return true
  }

  private suspend fun activateFileHider(context: Context, showToast: Boolean = true): Boolean {
    val result = withContext(Dispatchers.IO) { fileHider.activate() }
    if (!result.success) {
      _fileHiderError.value = result.msgResId
      if (showToast) showErrorToast(context, result.msgResId)
      return false
    }
    _fileHiderError.value = 0
    return true
  }

  // Utilities

  private fun loadPersistedState() {
    val managedApps = PrefMgr.getHideApps()
    var hiddenApps = PrefMgr.getHiddenApps()

    val managedFolders = PrefMgr.getHideFilePath()
    var hiddenFolders = PrefMgr.getHiddenFolders()
    val legacyIsHidden = PrefMgr.getPrefs().getBoolean("isHidden", false)

    if (hiddenFolders.isEmpty() && managedFolders.isNotEmpty() && legacyIsHidden) {
      hiddenFolders = managedFolders
    }
    if (hiddenApps.isEmpty() && managedApps.isNotEmpty() && legacyIsHidden) {
      hiddenApps = managedApps
    }

    _appStates.value =
      managedApps.associateWith { pkgName ->
        if (pkgName in hiddenApps) State.HIDDEN else State.VISIBLE
      }

    _folderStates.value =
      managedFolders.associateWith { path ->
        if (path in hiddenFolders) State.HIDDEN else State.VISIBLE
      }

    _appHiderMode.value = AppHiderMode.fromKey(PrefMgr.getAppHiderMode())
    _fileHiderMode.value = FileHiderMode.fromKey(PrefMgr.getFileHiderMode())
    _appHiderError.value = 0
    _fileHiderError.value = 0
  }

  private fun startPersistence() {
    scope.launch {
      _folderStates.collect { fs ->
        val managedFolders = PrefMgr.getHideFilePath().toSet()
        val hidden = fs.filterValues { it == State.HIDDEN }.keys.intersect(managedFolders)
        PrefMgr.setHiddenFolders(hidden)
      }
    }
    scope.launch {
      _appStates
        .map { states ->
          val managedApps = PrefMgr.getHideApps().toSet()
          states.filterValues { it == State.HIDDEN }.keys.intersect(managedApps)
        }
        .collect { apps -> PrefMgr.setHiddenApps(apps) }
    }
  }

  private fun computeState(folderStates: Map<String, State>, appStates: Map<String, State>): State {
    val managedApps = PrefMgr.getHideApps()
    val managedFolders = PrefMgr.getHideFilePath()

    if (
      managedFolders.any { folderStates[it] == State.PROCESSING } ||
        managedApps.any { appStates[it] == State.PROCESSING }
    ) {
      return State.PROCESSING
    }

    val anyHidden =
      managedApps.any { appStates[it] == State.HIDDEN } ||
        managedFolders.any { folderStates[it] == State.HIDDEN }
    if (!anyHidden) return State.VISIBLE

    val allAppsHidden = managedApps.all { appStates[it] == State.HIDDEN }
    val allFoldersHidden = managedFolders.all { folderStates[it] == State.HIDDEN }

    return if (allAppsHidden && allFoldersHidden) State.HIDDEN else State.VISIBLE
  }

  private fun doneStateFor(action: Action): State =
    if (action == Action.HIDE) State.HIDDEN else State.VISIBLE

  private fun launchTrackedProcess(name: String, block: suspend () -> Unit) {
    if (currentJob?.isActive == true) {
      Log.w(TAG, "Ignoring '$name': another process is already running.")
      return
    }

    val job =
      scope.launch {
        try {
          block()
        } catch (e: CancellationException) {
          Log.w(TAG, "'$name' cancelled.")
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "'$name' failed.", e)
        }
      }

    currentJob = job
    job.invokeOnCompletion { if (currentJob === job) currentJob = null }
  }

  private suspend fun processTargets(
    stateFlow: MutableStateFlow<Map<String, State>>,
    keys: Set<String>,
    action: Action,
    process: suspend () -> Unit,
  ) {
    val originalStates = snapshotStates(stateFlow.value, keys)
    updateStates(stateFlow, keys, State.PROCESSING)

    try {
      process()
      updateStates(stateFlow, keys, doneStateFor(action))
    } catch (e: CancellationException) {
      restoreStates(stateFlow, originalStates)
      throw e
    } catch (e: Exception) {
      restoreStates(stateFlow, originalStates)
      throw e
    }
  }

  private fun snapshotStates(current: Map<String, State>, keys: Set<String>): Map<String, State> =
    keys.associateWith { key -> current[key] ?: State.VISIBLE }

  private fun updateStates(
    stateFlow: MutableStateFlow<Map<String, State>>,
    keys: Set<String>,
    state: State,
  ) {
    if (keys.isEmpty()) return
    stateFlow.value += keys.associateWith { state }
  }

  private fun restoreStates(
    stateFlow: MutableStateFlow<Map<String, State>>,
    originalStates: Map<String, State>,
  ) {
    if (originalStates.isEmpty()) return
    stateFlow.value += originalStates
  }

  private fun buildAppHider(context: Context, mode: AppHiderMode = appHiderMode.value): AppHider =
    AppHider.build(
      context = context,
      mode = mode,
      options =
        AppHiderOptions(disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide()),
    )

  private fun buildFileHider(
    context: Context,
    mode: FileHiderMode = fileHiderMode.value,
  ): FileHider = FileHider.build(context, mode)

  private fun showErrorToast(context: Context, msgResId: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show()
    }
  }
}
