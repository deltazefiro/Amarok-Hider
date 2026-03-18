package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.utils.AppInfoUtil
import deltazero.amarok.utils.AppInfoUtil.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppListViewModel(application: Application) : AndroidViewModel(application) {
  private val appInfoUtil = AppInfoUtil(application)

  private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
  val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _showSystemApps = MutableStateFlow(false)
  val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

  private val _showRootApps = MutableStateFlow(false)
  val showRootApps: StateFlow<Boolean> = _showRootApps.asStateFlow()

  private var searchQuery = ""

  init {
    refreshApps()
  }

  fun setSearchQuery(query: String) {
    searchQuery = query
    updateAppList()
  }

  fun toggleSystemApps() {
    _showSystemApps.value = !_showSystemApps.value
    updateAppList()
  }

  fun toggleRootApps() {
    _showRootApps.value = !_showRootApps.value
    updateAppList()
  }

  fun refreshApps() {
    _isLoading.value = true
    viewModelScope.launch(Dispatchers.IO) {
      appInfoUtil.refresh()
      updateAppList()
      _isLoading.value = false
    }
  }

  fun toggleAppHidden(app: AppInfo) {
    val hiddenApps = PrefMgr.getHideApps()
    if (hiddenApps.contains(app.packageName())) {
      hiddenApps.remove(app.packageName())
    } else {
      hiddenApps.add(app.packageName())
    }
    PrefMgr.setHideApps(hiddenApps)
  }

  private fun updateAppList() {
    viewModelScope.launch(Dispatchers.IO) {
      val filtered =
        appInfoUtil.getFilteredApps(
          searchQuery.ifEmpty { null },
          _showSystemApps.value,
          _showRootApps.value,
        )
      _appList.value = filtered
    }
  }
}
