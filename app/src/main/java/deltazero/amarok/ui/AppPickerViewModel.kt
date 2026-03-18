package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.utils.AppInfoUtil
import deltazero.amarok.utils.AppInfoUtil.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppPickerUiState(
  val apps: List<AppInfo> = emptyList(),
  val hiddenApps: Set<String> = emptySet(),
  val actuallyHiddenApps: Set<String> = emptySet(),
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

class AppPickerViewModel(application: Application) : AndroidViewModel(application) {
  private val appInfoUtil = AppInfoUtil(application)

  private val _isLoading = MutableStateFlow(false)
  private val _searchQuery = MutableStateFlow("")
  private val _showSystemApps = MutableStateFlow(false)
  private val _showRootApps = MutableStateFlow(false)
  private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
  private val _pendingWarning = MutableStateFlow<WarningType?>(null)

  private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

  private val _actuallyHiddenApps =
    Hider.hiddenApps
      .asFlow()
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Hider.hiddenApps.value ?: emptySet(),
      )

  private val _filteredApps =
    combine(_allApps, _searchQuery, _showSystemApps, _showRootApps) { _, query, showSystem, showRoot
      ->
      appInfoUtil.getFilteredApps(query.ifEmpty { null }, showSystem, showRoot)
    }

  val uiState: StateFlow<AppPickerUiState> =
    combine(_filteredApps, _hiddenApps, _isLoading, _searchQuery, _pendingWarning) {
        apps,
        hidden,
        loading,
        query,
        warning ->
        AppPickerUiState(
          apps = apps,
          hiddenApps = hidden,
          isLoading = loading,
          searchQuery = query,
          pendingWarning = warning,
        )
      }
      .combine(_actuallyHiddenApps) { state, actuallyHidden ->
        state.copy(actuallyHiddenApps = actuallyHidden)
      }
      .combine(combine(_showSystemApps, _showRootApps, ::Pair)) { state, (showSystem, showRoot) ->
        state.copy(showSystemApps = showSystem, showRootApps = showRoot)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPickerUiState())

  init {
    _hiddenApps.value = PrefMgr.getHideApps().toSet()
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

  fun toggleAppHidden(app: AppInfo) {
    val hiddenApps = PrefMgr.getHideApps()
    if (hiddenApps.contains(app.packageName())) {
      hiddenApps.remove(app.packageName())
    } else {
      hiddenApps.add(app.packageName())
    }
    PrefMgr.setHideApps(hiddenApps)
    _hiddenApps.value = hiddenApps.toSet()
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
