package deltazero.amarok.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.R
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.apphider.AppHiderMode
import deltazero.amarok.apphider.AppHiderOptions
import deltazero.amarok.filehider.FileHider
import deltazero.amarok.filehider.FileHiderMode
import javax.inject.Inject
import javax.inject.Singleton
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

@Singleton
class HiderController
@Inject
constructor(
  @param:ApplicationContext private val applicationContext: Context,
  private val settingsRepo: SettingsRepository,
  private val hiderStateRepo: HiderStateRepository,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var currentJob: Job? = null
  private lateinit var appHider: AppHider
  private lateinit var fileHider: FileHider

  private val _processingFolders = MutableStateFlow<Set<String>>(emptySet())
  private val _processingApps = MutableStateFlow<Set<String>>(emptySet())
  private val _folderStates = MutableStateFlow<Map<String, Hider.State>>(emptyMap())
  private val _appStates = MutableStateFlow<Map<String, Hider.State>>(emptyMap())
  private val _state = MutableStateFlow(Hider.State.VISIBLE)
  private val _appHiderError = MutableStateFlow(0)
  private val _fileHiderError = MutableStateFlow(0)

  var initialized = false
    private set

  val state: StateFlow<Hider.State>
    get() = _state.asStateFlow()

  val folderStates: StateFlow<Map<String, Hider.State>>
    get() = _folderStates.asStateFlow()

  val appStates: StateFlow<Map<String, Hider.State>>
    get() = _appStates.asStateFlow()

  val appHiderError: StateFlow<Int>
    get() = _appHiderError.asStateFlow()

  val fileHiderError: StateFlow<Int>
    get() = _fileHiderError.asStateFlow()

  val stateLiveData: LiveData<Hider.State> by lazy { _state.asLiveData() }

  val appStatesLiveData: LiveData<Map<String, Hider.State>> by lazy { _appStates.asLiveData() }

  fun init(context: Context = applicationContext) {
    if (initialized) return
    val appContext = context.applicationContext
    buildStateFlows()
    val settings = settingsRepo.settings.value
    appHider = buildAppHider(appContext, settings.appHiderMode)
    fileHider = buildFileHider(appContext, settings.fileHiderMode)
    startConfigObservers(appContext)
    initialized = true
    scope.launch { activateAppHider(appContext, showToast = false) }
    scope.launch { activateFileHider(appContext, showToast = false) }
  }

  fun getState(): Hider.State = _state.value

  fun processAll(context: Context, action: Hider.Action) {
    val appContext = context.applicationContext
    launchTrackedProcess("processAll") {
      if (!activateAppHider(appContext)) return@launchTrackedProcess
      if (!activateFileHider(appContext)) return@launchTrackedProcess
      val hide = action == Hider.Action.HIDE
      val managedApps = hiderStateRepo.managedApps.value
      val managedFolders = hiderStateRepo.managedFolders.value
      val currentAppStates = _appStates.value
      val currentFolderStates = _folderStates.value

      val appsToProcess =
        managedApps.filterTo(mutableSetOf()) { pkgName ->
          val state = currentAppStates[pkgName] ?: Hider.State.VISIBLE
          if (hide) state != Hider.State.HIDDEN else state == Hider.State.HIDDEN
        }
      val foldersToProcess =
        managedFolders.filterTo(mutableSetOf()) { path ->
          val state = currentFolderStates[path] ?: Hider.State.VISIBLE
          if (hide) state != Hider.State.HIDDEN else state == Hider.State.HIDDEN
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
        Toast.makeText(appContext, msgRes, Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun processApps(context: Context, pkgNames: Set<String>, action: Hider.Action) {
    if (pkgNames.isEmpty()) return
    val appContext = context.applicationContext
    launchTrackedProcess("processApps") {
      if (!activateAppHider(appContext)) return@launchTrackedProcess
      val managedPkgNames = pkgNames.intersect(hiderStateRepo.managedApps.value)
      processTargets(
        processingFlow = _processingApps,
        keys = managedPkgNames,
        persist = { apps ->
          if (action == Hider.Action.HIDE) hiderStateRepo.addHiddenApps(apps)
          else hiderStateRepo.removeHiddenApps(apps)
        },
      ) {
        withContext(Dispatchers.IO) { appHider.process(managedPkgNames, action) }
      }
    }
  }

  fun processFolders(context: Context, paths: Set<String>, action: Hider.Action) {
    if (paths.isEmpty()) return
    val appContext = context.applicationContext
    launchTrackedProcess("processFolders") {
      if (!activateFileHider(appContext)) return@launchTrackedProcess
      val managedPaths = paths.intersect(hiderStateRepo.managedFolders.value)
      processTargets(
        processingFlow = _processingFolders,
        keys = managedPaths,
        persist = { folders ->
          if (action == Hider.Action.HIDE) hiderStateRepo.addHiddenFolders(folders)
          else hiderStateRepo.removeHiddenFolders(folders)
        },
      ) {
        withContext(Dispatchers.IO) { fileHider.process(managedPaths, action) }
      }
    }
  }

  fun cancelProcess() {
    currentJob?.cancel()
  }

  suspend fun switchAppHider(mode: AppHiderMode) {
    settingsRepo.setAppHiderMode(mode)
  }

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

  private fun computeState(
    folderStates: Map<String, Hider.State>,
    appStates: Map<String, Hider.State>,
  ): Hider.State {
    if (
      folderStates.any { it.value == Hider.State.PROCESSING } ||
        appStates.any { it.value == Hider.State.PROCESSING }
    ) {
      return Hider.State.PROCESSING
    }

    val anyHidden =
      appStates.any { it.value == Hider.State.HIDDEN } ||
        folderStates.any { it.value == Hider.State.HIDDEN }
    if (!anyHidden) return Hider.State.VISIBLE

    val allAppsHidden = appStates.all { it.value == Hider.State.HIDDEN }
    val allFoldersHidden = folderStates.all { it.value == Hider.State.HIDDEN }

    return if (allAppsHidden && allFoldersHidden) Hider.State.HIDDEN else Hider.State.VISIBLE
  }

  private fun deriveStates(
    managed: Set<String>,
    hidden: Set<String>,
    processing: Set<String>,
  ): Map<String, Hider.State> =
    managed.associateWith { key ->
      when (key) {
        in processing -> Hider.State.PROCESSING
        in hidden -> Hider.State.HIDDEN
        else -> Hider.State.VISIBLE
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

  private companion object {
    private const val TAG = "Hider"
  }
}
