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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Hider {

  private const val TAG = "Hider"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var currentJob: Job? = null
  private lateinit var appHider: AppHider
  private lateinit var fileHider: FileHider
  private lateinit var settingsRepo: SettingsRepository
  private lateinit var hiderStateRepo: HiderStateRepository

  private val _processingFolders = MutableStateFlow<Set<String>>(emptySet())
  private val _processingApps = MutableStateFlow<Set<String>>(emptySet())
  private val _folderStates = MutableStateFlow<Map<String, State>>(emptyMap())
  private val _appStates = MutableStateFlow<Map<String, State>>(emptyMap())
  private val _state = MutableStateFlow(State.VISIBLE)
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
    get() = _state.asStateFlow()

  val folderStates: StateFlow<Map<String, State>>
    get() = _folderStates.asStateFlow()

  val appStates: StateFlow<Map<String, State>>
    get() = _appStates.asStateFlow()

  val appHiderError: StateFlow<Int>
    get() = _appHiderError.asStateFlow()

  val fileHiderError: StateFlow<Int>
    get() = _fileHiderError.asStateFlow()

  // LiveData bridges (Java consumers)

  @JvmStatic val stateLiveData: LiveData<State> by lazy { _state.asLiveData() }

  @JvmStatic val appStatesLiveData: LiveData<Map<String, State>> by lazy { _appStates.asLiveData() }

  // Initialization

  @JvmStatic
  fun init(
    context: Context,
    settingsRepository: SettingsRepository,
    hiderStateRepository: HiderStateRepository,
  ) {
    if (initialized) return
    settingsRepo = settingsRepository
    hiderStateRepo = hiderStateRepository
    buildStateFlows()
    val settings = settingsRepo.settings.value
    appHider = buildAppHider(context, settings.appHiderMode)
    fileHider = buildFileHider(context, settings.fileHiderMode)
    startConfigObservers(context.applicationContext)
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
      val managedApps = hiderStateRepo.managedApps.value
      val managedFolders = hiderStateRepo.managedFolders.value
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
      processTargets(
        processingFlow = _processingApps,
        keys = appsToProcess,
        persist = { apps ->
          if (hide) hiderStateRepo.addHiddenApps(apps) else hiderStateRepo.removeHiddenApps(apps)
        },
      ) {
        withContext(Dispatchers.IO) { appHider.process(appsToProcess, action) }
      }
      processTargets(
        processingFlow = _processingFolders,
        keys = foldersToProcess,
        persist = { folders ->
          if (hide) hiderStateRepo.addHiddenFolders(folders)
          else hiderStateRepo.removeHiddenFolders(folders)
        },
      ) {
        withContext(Dispatchers.IO) { fileHider.process(foldersToProcess, action) }
      }

      Log.i(TAG, "Process '${if (hide) "hide" else "unhide"}' finish.")
      if (!settingsRepo.settings.value.disableToasts) {
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
      val managedPkgNames = pkgNames.intersect(hiderStateRepo.managedApps.value)
      processTargets(
        processingFlow = _processingApps,
        keys = managedPkgNames,
        persist = { apps ->
          if (action == Action.HIDE) hiderStateRepo.addHiddenApps(apps)
          else hiderStateRepo.removeHiddenApps(apps)
        },
      ) {
        withContext(Dispatchers.IO) { appHider.process(managedPkgNames, action) }
      }
    }
  }

  @JvmStatic
  fun processFolders(context: Context, paths: Set<String>, action: Action) {
    if (paths.isEmpty()) return
    launchTrackedProcess("processFolders") {
      if (!activateFileHider(context)) return@launchTrackedProcess
      val managedPaths = paths.intersect(hiderStateRepo.managedFolders.value)
      processTargets(
        processingFlow = _processingFolders,
        keys = managedPaths,
        persist = { folders ->
          if (action == Action.HIDE) hiderStateRepo.addHiddenFolders(folders)
          else hiderStateRepo.removeHiddenFolders(folders)
        },
      ) {
        withContext(Dispatchers.IO) { fileHider.process(managedPaths, action) }
      }
    }
  }

  @JvmStatic
  fun cancelProcess() {
    currentJob?.cancel()
  }

  @JvmStatic
  suspend fun switchAppHider(mode: AppHiderMode) {
    settingsRepo.setAppHiderMode(mode)
  }

  @JvmStatic
  suspend fun switchFileHider(mode: FileHiderMode) {
    settingsRepo.setFileHiderMode(mode)
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

  private fun buildStateFlows() {
    _folderStates.value =
      deriveStates(
        hiderStateRepo.managedFolders.value,
        hiderStateRepo.hiddenFolders.value,
        _processingFolders.value,
      )
    _appStates.value =
      deriveStates(
        hiderStateRepo.managedApps.value,
        hiderStateRepo.hiddenApps.value,
        _processingApps.value,
      )
    _state.value = computeState(_folderStates.value, _appStates.value)

    scope.launch {
      combine(hiderStateRepo.managedFolders, hiderStateRepo.hiddenFolders, _processingFolders) {
          managed,
          hidden,
          processing ->
          deriveStates(managed, hidden, processing)
        }
        .collect { _folderStates.value = it }
    }

    scope.launch {
      combine(hiderStateRepo.managedApps, hiderStateRepo.hiddenApps, _processingApps) {
          managed,
          hidden,
          processing ->
          deriveStates(managed, hidden, processing)
        }
        .collect { _appStates.value = it }
    }

    scope.launch {
      combine(_folderStates, _appStates) { folderStates, appStates ->
          computeState(folderStates, appStates)
        }
        .collect { _state.value = it }
    }
  }

  private fun startConfigObservers(context: Context) {
    scope.launch {
      var appliedConfig = AppHiderConfig.from(settingsRepo.settings.value)
      settingsRepo.settings
        .map { AppHiderConfig.from(it) }
        .distinctUntilChanged()
        .collect { config ->
          if (config == appliedConfig) return@collect
          appliedConfig = config
          appHider = buildAppHider(context, config.mode)
          activateAppHider(context, showToast = false)
        }
    }

    scope.launch {
      var appliedConfig = FileHiderConfig.from(settingsRepo.settings.value)
      settingsRepo.settings
        .map { FileHiderConfig.from(it) }
        .distinctUntilChanged()
        .collect { config ->
          if (config == appliedConfig) return@collect
          appliedConfig = config
          fileHider = buildFileHider(context, config.mode)
          activateFileHider(context, showToast = false)
        }
    }
  }

  private fun computeState(folderStates: Map<String, State>, appStates: Map<String, State>): State {
    if (
      folderStates.any { it.value == State.PROCESSING } ||
        appStates.any { it.value == State.PROCESSING }
    ) {
      return State.PROCESSING
    }

    val anyHidden =
      appStates.any { it.value == State.HIDDEN } || folderStates.any { it.value == State.HIDDEN }
    if (!anyHidden) return State.VISIBLE

    val allAppsHidden = appStates.all { it.value == State.HIDDEN }
    val allFoldersHidden = folderStates.all { it.value == State.HIDDEN }

    return if (allAppsHidden && allFoldersHidden) State.HIDDEN else State.VISIBLE
  }

  private fun deriveStates(
    managed: Set<String>,
    hidden: Set<String>,
    processing: Set<String>,
  ): Map<String, State> =
    managed.associateWith { key ->
      when (key) {
        in processing -> State.PROCESSING
        in hidden -> State.HIDDEN
        else -> State.VISIBLE
      }
    }

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
    processingFlow: MutableStateFlow<Set<String>>,
    keys: Set<String>,
    persist: suspend (Set<String>) -> Unit,
    process: suspend () -> Unit,
  ) {
    if (keys.isEmpty()) return
    processingFlow.value += keys

    try {
      process()
      persist(keys)
    } finally {
      processingFlow.value -= keys
    }
  }

  private fun buildAppHider(context: Context, mode: AppHiderMode): AppHider {
    val settings = settingsRepo.settings.value
    return AppHider.build(
      context = context,
      mode = mode,
      options =
        AppHiderOptions(disableOnly = settings.xHideEnabled && settings.disableOnlyWithXHide),
    )
  }

  private fun buildFileHider(context: Context, mode: FileHiderMode): FileHider =
    FileHider.build(context, mode)

  private data class AppHiderConfig(val mode: AppHiderMode, val disableOnly: Boolean) {
    companion object {
      fun from(settings: SettingsSnapshot) =
        AppHiderConfig(
          mode = settings.appHiderMode,
          disableOnly = settings.xHideEnabled && settings.disableOnlyWithXHide,
        )
    }
  }

  private data class FileHiderConfig(val mode: FileHiderMode, val obfuscateLevel: Int) {
    companion object {
      fun from(settings: SettingsSnapshot) =
        FileHiderConfig(mode = settings.fileHiderMode, obfuscateLevel = settings.obfuscateLevel)
    }
  }

  private fun showErrorToast(context: Context, msgResId: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show()
    }
  }
}
