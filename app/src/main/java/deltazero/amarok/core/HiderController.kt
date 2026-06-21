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
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class HiderController
@Inject
constructor(
  @param:ApplicationContext private val applicationContext: Context,
  private val settingsRepo: SettingsRepository,
  private val hiderStateRepo: HiderStateRepository,
  private val activityRepo: ActivityRepository,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val currentJobs = Collections.newSetFromMap(ConcurrentHashMap<Job, Boolean>())
  private lateinit var appHider: AppHider
  private lateinit var fileHider: FileHider

  private val _processingFolders = MutableStateFlow<Set<String>>(emptySet())
  private val _processingApps = MutableStateFlow<Set<String>>(emptySet())
  private val processingFolderCounts = mutableMapOf<String, Int>()
  private val processingAppCounts = mutableMapOf<String, Int>()
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
      val appHider = appHider
      val fileHider = fileHider
      val hide = action == Hider.Action.HIDE
      val managedApps = hiderStateRepo.managedApps.value
      val managedFolders = hiderStateRepo.managedFolders.value
      val cancelledJobs = cancelledJobsToJoin()
      val premarkedApps = if (cancelledJobs.isEmpty()) emptySet() else managedApps
      val premarkedFolders = if (cancelledJobs.isEmpty()) emptySet() else managedFolders
      var appsToProcess = emptySet<String>()
      var foldersToProcess = emptySet<String>()

      Log.i(TAG, "Process '${if (hide) "hide" else "unhide"}' start.")
      addProcessing(_processingApps, processingAppCounts, premarkedApps)
      addProcessing(_processingFolders, processingFolderCounts, premarkedFolders)
      try {
        cancelledJobs.joinAll()
        appsToProcess =
          if (cancelledJobs.isEmpty()) {
            targetsForAction(
              managed = managedApps,
              states = _appStates.value,
              processing = _processingApps.value,
              action = action,
            )
          } else {
            cancelledTargetsForAction(managedApps, hiderStateRepo.hiddenApps.value, action)
          }
        foldersToProcess =
          if (cancelledJobs.isEmpty()) {
            targetsForAction(
              managed = managedFolders,
              states = _folderStates.value,
              processing = _processingFolders.value,
              action = action,
            )
          } else {
            cancelledTargetsForAction(managedFolders, hiderStateRepo.hiddenFolders.value, action)
          }
        val changed = appsToProcess.isNotEmpty() || foldersToProcess.isNotEmpty()
        addProcessing(_processingApps, processingAppCounts, appsToProcess - premarkedApps)
        addProcessing(
          _processingFolders,
          processingFolderCounts,
          foldersToProcess - premarkedFolders,
        )
        removeProcessing(_processingApps, processingAppCounts, premarkedApps - appsToProcess)
        removeProcessing(
          _processingFolders,
          processingFolderCounts,
          premarkedFolders - foldersToProcess,
        )
        if (!activateAppHider(appContext, appHider)) return@launchTrackedProcess
        if (!activateFileHider(appContext, fileHider)) return@launchTrackedProcess
        coroutineScope {
          awaitAll(
            async {
              if (appsToProcess.isEmpty()) return@async
              withContext(Dispatchers.IO) { appHider.process(appsToProcess, action) }
              if (hide) hiderStateRepo.addHiddenApps(appsToProcess)
              else hiderStateRepo.removeHiddenApps(appsToProcess)
            },
            async {
              if (foldersToProcess.isEmpty()) return@async
              withContext(Dispatchers.IO) { fileHider.process(foldersToProcess, action) }
              if (hide) hiderStateRepo.addHiddenFolders(foldersToProcess)
              else hiderStateRepo.removeHiddenFolders(foldersToProcess)
            },
          )
        }
        recordActivity(action, changed)
      } finally {
        removeProcessing(_processingApps, processingAppCounts, appsToProcess)
        removeProcessing(_processingFolders, processingFolderCounts, foldersToProcess)
        removeProcessing(_processingApps, processingAppCounts, premarkedApps - appsToProcess)
        removeProcessing(
          _processingFolders,
          processingFolderCounts,
          premarkedFolders - foldersToProcess,
        )
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
      val appHider = appHider
      val appsToProcess =
        targetsForAction(
          managed = pkgNames.intersect(hiderStateRepo.managedApps.value),
          states = _appStates.value,
          processing = _processingApps.value,
          action = action,
        )
      val changed =
        processTargets(
          processingFlow = _processingApps,
          keys = appsToProcess,
          persist = { apps ->
            if (action == Hider.Action.HIDE) hiderStateRepo.addHiddenApps(apps)
            else hiderStateRepo.removeHiddenApps(apps)
          },
        ) {
          if (!activateAppHider(appContext, appHider)) return@processTargets false
          withContext(Dispatchers.IO) { appHider.process(appsToProcess, action) }
          true
        }
      recordActivity(action, changed)
    }
  }

  fun processFolders(context: Context, paths: Set<String>, action: Hider.Action) {
    if (paths.isEmpty()) return
    val appContext = context.applicationContext
    launchTrackedProcess("processFolders") {
      val fileHider = fileHider
      val foldersToProcess =
        targetsForAction(
          managed = paths.intersect(hiderStateRepo.managedFolders.value),
          states = _folderStates.value,
          processing = _processingFolders.value,
          action = action,
        )
      val changed =
        processTargets(
          processingFlow = _processingFolders,
          keys = foldersToProcess,
          persist = { folders ->
            if (action == Hider.Action.HIDE) hiderStateRepo.addHiddenFolders(folders)
            else hiderStateRepo.removeHiddenFolders(folders)
          },
        ) {
          if (!activateFileHider(appContext, fileHider)) return@processTargets false
          withContext(Dispatchers.IO) { fileHider.process(foldersToProcess, action) }
          true
        }
      recordActivity(action, changed)
    }
  }

  // Records hide/unhide activity for the dashboard. Only stamps when something was actually
  // processed, so a no-op toggle (already in the target state) doesn't count as a reveal.
  private suspend fun recordActivity(action: Hider.Action, changed: Boolean) {
    if (!changed) return
    when (action) {
      Hider.Action.HIDE -> activityRepo.recordHide()
      Hider.Action.UNHIDE -> activityRepo.recordReveal()
    }
  }

  fun cancelProcess() {
    for (job in currentJobs.toList()) job.cancel()
  }

  suspend fun switchAppHider(mode: AppHiderMode) {
    settingsRepo.setAppHiderMode(mode)
  }

  suspend fun switchFileHider(mode: FileHiderMode) {
    settingsRepo.setFileHiderMode(mode)
  }

  private suspend fun activateAppHider(
    context: Context,
    hider: AppHider = appHider,
    showToast: Boolean = true,
  ): Boolean {
    val result = withContext(Dispatchers.IO) { hider.activate() }
    if (!result.success) {
      _appHiderError.value = result.msgResId
      if (showToast) showErrorToast(context, result.msgResId)
      return false
    }
    _appHiderError.value = 0
    return true
  }

  private suspend fun activateFileHider(
    context: Context,
    hider: FileHider = fileHider,
    showToast: Boolean = true,
  ): Boolean {
    val result = withContext(Dispatchers.IO) { hider.activate() }
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
          val hider = buildAppHider(context, config.mode)
          appHider = hider
          activateAppHider(context, hider, showToast = false)
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
          val hider = buildFileHider(context, config.mode)
          fileHider = hider
          activateFileHider(context, hider, showToast = false)
        }
    }
  }

  private fun computeState(
    folderStates: Map<String, Hider.State>,
    appStates: Map<String, Hider.State>,
  ): Hider.State = computeStateForTest(folderStates, appStates)

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
    val job =
      scope.launch(start = CoroutineStart.LAZY) {
        try {
          block()
        } catch (e: CancellationException) {
          Log.w(TAG, "'$name' cancelled.")
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "'$name' failed.", e)
        }
      }

    currentJobs += job
    job.invokeOnCompletion { scope.launch { currentJobs -= job } }
    job.start()
  }

  private suspend fun cancelledJobsToJoin(): List<Job> {
    val currentJob = coroutineContext[Job]
    return currentJobs.filter { it !== currentJob && it.isCancelled }
  }

  private fun cancelledTargetsForAction(
    managed: Set<String>,
    hidden: Set<String>,
    action: Hider.Action,
  ): Set<String> = cancelledTargetsForActionForTest(managed, hidden, action)

  private fun targetsForAction(
    managed: Set<String>,
    states: Map<String, Hider.State>,
    processing: Set<String>,
    action: Hider.Action,
  ): Set<String> = targetsForActionForTest(managed, states, processing, action)

  private suspend fun processTargets(
    processingFlow: MutableStateFlow<Set<String>>,
    keys: Set<String>,
    persist: suspend (Set<String>) -> Unit,
    process: suspend () -> Boolean,
  ): Boolean {
    if (keys.isEmpty()) return false
    addProcessing(processingFlow, processingCounters(processingFlow), keys)

    try {
      val processed = process()
      if (processed) persist(keys)
      return processed
    } finally {
      removeProcessing(processingFlow, processingCounters(processingFlow), keys)
    }
  }

  private fun processingCounters(
    processingFlow: MutableStateFlow<Set<String>>
  ): MutableMap<String, Int> =
    when (processingFlow) {
      _processingApps -> processingAppCounts
      _processingFolders -> processingFolderCounts
      else -> error("Unknown processing flow")
    }

  private fun addProcessing(
    processingFlow: MutableStateFlow<Set<String>>,
    counts: MutableMap<String, Int>,
    keys: Set<String>,
  ) {
    if (keys.isEmpty()) return
    for (key in keys) counts[key] = (counts[key] ?: 0) + 1
    processingFlow.value = counts.keys.toSet()
  }

  private fun removeProcessing(
    processingFlow: MutableStateFlow<Set<String>>,
    counts: MutableMap<String, Int>,
    keys: Set<String>,
  ) {
    if (keys.isEmpty()) return
    for (key in keys) {
      val count = (counts[key] ?: continue) - 1
      if (count > 0) counts[key] = count else counts.remove(key)
    }
    processingFlow.value = counts.keys.toSet()
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
    FileHider.build(context, mode, settingsRepo.settings.value)

  private data class AppHiderConfig(val mode: AppHiderMode, val disableOnly: Boolean) {
    companion object {
      fun from(settings: SettingsSnapshot) =
        AppHiderConfig(
          mode = settings.appHiderMode,
          disableOnly = settings.xHideEnabled && settings.disableOnlyWithXHide,
        )
    }
  }

  private data class FileHiderConfig(
    val mode: FileHiderMode,
    val obfuscateFileHeader: Boolean,
    val obfuscateTextFile: Boolean,
    val obfuscateTextFileEnhanced: Boolean,
  ) {
    companion object {
      fun from(settings: SettingsSnapshot) =
        FileHiderConfig(
          mode = settings.fileHiderMode,
          obfuscateFileHeader = settings.obfuscateFileHeader,
          obfuscateTextFile = settings.obfuscateTextFile,
          obfuscateTextFileEnhanced = settings.obfuscateTextFileEnhanced,
        )
    }
  }

  private fun showErrorToast(context: Context, msgResId: Int) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msgResId, Toast.LENGTH_LONG).show()
    }
  }

  companion object {
    private const val TAG = "Hider"

    internal fun computeStateForTest(
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

    internal fun targetsForActionForTest(
      managed: Set<String>,
      states: Map<String, Hider.State>,
      processing: Set<String>,
      action: Hider.Action,
    ): Set<String> =
      managed.filterTo(mutableSetOf()) { key ->
        if (key in processing) return@filterTo false
        val state = states[key] ?: Hider.State.VISIBLE
        when (action) {
          Hider.Action.HIDE -> state != Hider.State.HIDDEN
          Hider.Action.UNHIDE -> state == Hider.State.HIDDEN
        }
      }

    internal fun cancelledTargetsForActionForTest(
      managed: Set<String>,
      hidden: Set<String>,
      action: Hider.Action,
    ): Set<String> =
      when (action) {
        Hider.Action.HIDE -> managed.filterTo(mutableSetOf()) { it !in hidden }
        Hider.Action.UNHIDE -> managed
      }
  }
}
