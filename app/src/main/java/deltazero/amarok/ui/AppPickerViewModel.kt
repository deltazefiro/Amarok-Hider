package deltazero.amarok.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderStateRepository
import deltazero.amarok.utils.AppInfoUtil
import deltazero.amarok.utils.AppInfoUtil.AppInfo
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppPickerUiState(
  val apps: List<AppInfo> = emptyList(),
  val managedApps: Set<String> = emptySet(),
  val hiddenApps: Set<String> = emptySet(),
  val isLoading: Boolean = false,
  val searchQuery: String = "",
  val showSystemApps: Boolean = false,
  val showRootApps: Boolean = false,
  val pendingWarning: WarningType? = null,
)

enum class WarningType {
  SYSTEM_APPS,
  ROOT_APPS,
}

@HiltViewModel
class AppPickerViewModel
@Inject
constructor(
  @ApplicationContext context: Context,
  private val hiderStateRepo: HiderStateRepository,
) : ViewModel() {
  private val appInfoUtil = AppInfoUtil(context)

  private val _isLoading = MutableStateFlow(false)
  private val _searchQuery = MutableStateFlow("")
  private val _showSystemApps = MutableStateFlow(false)
  private val _showRootApps = MutableStateFlow(false)
  private val _pendingWarning = MutableStateFlow<WarningType?>(null)

  private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

  private val _hiddenApps =
    Hider.appStates
      .map { states -> states.filterValues { it == Hider.State.HIDDEN }.keys }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Hider.appStates.value.filterValues { it == Hider.State.HIDDEN }.keys,
      )

  private val _filteredApps =
    combine(_allApps, _searchQuery, _showSystemApps, _showRootApps) { _, query, showSystem, showRoot
      ->
      appInfoUtil.getFilteredApps(query.ifEmpty { null }, showSystem, showRoot)
    }

  val uiState: StateFlow<AppPickerUiState> =
    combine(_filteredApps, hiderStateRepo.managedApps, _isLoading, _searchQuery, _pendingWarning) {
        apps,
        managed,
        loading,
        query,
        warning ->
        AppPickerUiState(
          apps = apps,
          managedApps = managed,
          isLoading = loading,
          searchQuery = query,
          pendingWarning = warning,
        )
      }
      .combine(_hiddenApps) { state, hidden -> state.copy(hiddenApps = hidden) }
      .combine(combine(_showSystemApps, _showRootApps, ::Pair)) { state, (showSystem, showRoot) ->
        state.copy(showSystemApps = showSystem, showRootApps = showRoot)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPickerUiState())

  init {
    refreshApps()
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun requestToggleSystemApps() {
    if (_showSystemApps.value) {
      _showSystemApps.value = false
    } else {
      _pendingWarning.value = WarningType.SYSTEM_APPS
    }
  }

  fun requestToggleRootApps() {
    if (_showRootApps.value) {
      _showRootApps.value = false
    } else {
      _pendingWarning.value = WarningType.ROOT_APPS
    }
  }

  fun confirmWarning() {
    when (_pendingWarning.value) {
      WarningType.SYSTEM_APPS -> _showSystemApps.value = true
      WarningType.ROOT_APPS -> _showRootApps.value = true
      null -> {}
    }
    _pendingWarning.value = null
  }

  fun dismissWarning() {
    _pendingWarning.value = null
  }

  fun toggleManagedApp(app: AppInfo) {
    val pkgName = app.packageName()
    viewModelScope.launch {
      if (pkgName in hiderStateRepo.managedApps.value) {
        hiderStateRepo.removeManagedApp(pkgName)
      } else {
        hiderStateRepo.addManagedApp(pkgName)
      }
    }
  }

  fun refreshApps() {
    _isLoading.value = true
    viewModelScope.launch(Dispatchers.IO) {
      appInfoUtil.refresh()
      _allApps.value = appInfoUtil.getFilteredApps(null, true, true)
      _isLoading.value = false
    }
  }
}
