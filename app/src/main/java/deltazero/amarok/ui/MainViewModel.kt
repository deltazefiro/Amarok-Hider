package deltazero.amarok.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.R
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.core.ActivityRepository
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderStateRepository
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.filehider.FileHider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val hiderStateRepo: HiderStateRepository,
  private val settingsRepo: SettingsRepository,
  private val activityRepo: ActivityRepository,
) : ViewModel() {

  private val appHiderName: Flow<String> =
    settingsRepo.settings.map { AppHider.build(context, it.appHiderMode).name }

  private val fileHiderName: Flow<String> =
    settingsRepo.settings.map { FileHider.build(context, it.fileHiderMode, it).name }

  private val appPreviews: Flow<List<PreviewItem>> =
    hiderStateRepo.managedApps
      .map { pkgs -> pkgs.sorted().take(MAX_PREVIEWS).map(::loadAppPreview) }
      .flowOn(Dispatchers.IO)

  private val folderPreviews: Flow<List<PreviewItem>> =
    hiderStateRepo.managedFolders.map { paths ->
      paths.sorted().take(MAX_PREVIEWS).map { PreviewItem.Folder(folderName(it)) }
    }

  private data class Core(
    val state: Hider.State,
    val appCount: Int,
    val folderCount: Int,
    val appHiderName: String,
    val fileHiderName: String,
  )

  private data class Activity(
    val lastHiddenAt: Long?,
    val lastRevealedAt: Long?,
    val revealDays: Set<LocalDate>,
  )

  private val coreFlow: Flow<Core> =
    combine(
      Hider.state,
      hiderStateRepo.managedApps,
      hiderStateRepo.managedFolders,
      appHiderName,
      fileHiderName,
    ) { state, managedApps, managedFolders, appName, fileName ->
      Core(state, managedApps.size, managedFolders.size, appName, fileName)
    }

  private val activityFlow: Flow<Activity> =
    combine(activityRepo.lastHiddenAt, activityRepo.lastRevealedAt, activityRepo.revealDays) {
      lastHidden,
      lastRevealed,
      revealDays ->
      Activity(lastHidden, lastRevealed, revealDays)
    }

  private val previewsFlow: Flow<Pair<List<PreviewItem>, List<PreviewItem>>> =
    combine(appPreviews, folderPreviews) { apps, folders -> apps to folders }

  val uiState: StateFlow<DashboardUiState> =
    combine(coreFlow, activityFlow, previewsFlow) { core, activity, previews ->
        buildUiState(core, activity, previews)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

  private fun buildUiState(
    core: Core,
    activity: Activity,
    previews: Pair<List<PreviewItem>, List<PreviewItem>>,
  ): DashboardUiState {
    val today = LocalDate.now()

    // Hero "X min ago" reflects the transition into the current state.
    val actionTs =
      if (core.state == Hider.State.HIDDEN) activity.lastHiddenAt else activity.lastRevealedAt
    val lastActionLabel = actionTs?.let(::relativeTime).orEmpty()

    val lastRevealedDate = activity.lastRevealedAt?.let(::toLocalDate)
    val daysSinceReveal = lastRevealedDate?.let { ChronoUnit.DAYS.between(it, today).toInt() }
    val lastRevealedLabel =
      lastRevealedDate?.format(dateFormatter) ?: context.getString(R.string.dashboard_never)

    val revealDaysThisMonth =
      activity.revealDays
        .filterTo(mutableSetOf()) { it.year == today.year && it.month == today.month }
        .map { it.dayOfMonth }
        .toSet()

    val revealsLast30 = activity.revealDays.count { it.isAfter(today.minusDays(30)) }
    val frequencyLabel =
      context.getString(
        R.string.dashboard_frequency_value,
        String.format(Locale.getDefault(), "%.1f", revealsLast30 / 4.0),
      )

    return DashboardUiState(
      state = core.state,
      appCount = core.appCount,
      folderCount = core.folderCount,
      appHiderName = core.appHiderName,
      fileHiderName = core.fileHiderName,
      appPreviews = previews.first,
      folderPreviews = previews.second,
      lastActionLabel = lastActionLabel,
      daysSinceReveal = daysSinceReveal,
      lastRevealedLabel = lastRevealedLabel,
      frequencyLabel = frequencyLabel,
      revealDaysThisMonth = revealDaysThisMonth,
      today = today.dayOfMonth,
      daysInMonth = today.lengthOfMonth(),
    )
  }

  private fun loadAppPreview(pkgName: String): PreviewItem.App =
    try {
      val pm = context.packageManager
      val info = pm.getApplicationInfo(pkgName, 0)
      PreviewItem.App(
        icon = pm.getApplicationIcon(info),
        label = pm.getApplicationLabel(info).toString(),
      )
    } catch (e: Exception) {
      PreviewItem.App(icon = null, label = pkgName.substringAfterLast('.'))
    }

  private fun folderName(path: String): String =
    path.trimEnd('/').substringAfterLast('/').ifEmpty { path }

  private fun relativeTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
      )
      .toString()

  private fun toLocalDate(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

  private companion object {
    const val MAX_PREVIEWS = 3
    val dateFormatter: DateTimeFormatter =
      DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
  }
}
