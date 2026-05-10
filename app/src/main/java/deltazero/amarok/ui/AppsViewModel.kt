package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.AmarokApplication
import deltazero.amarok.core.Hider
import deltazero.amarok.utils.AppInfoUtil
import deltazero.amarok.utils.AppInfoUtil.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {
  private val appInfoUtil = AppInfoUtil(application)
  private val hiderStateRepo = (application as AmarokApplication).hiderStateRepo

  private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

  val managedApps: StateFlow<List<AppInfo>> =
    combine(_allApps, hiderStateRepo.managedApps) { allApps, managed ->
        allApps.filter { it.packageName() in managed }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val appStatesFlow =
    Hider.appStates.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      Hider.appStates.value,
    )

  val hiddenApps: StateFlow<Set<String>> =
    appStatesFlow
      .map { states -> states.filterValues { it == Hider.State.HIDDEN }.keys }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  init {
    loadManagedApps()
  }

  fun loadManagedApps() {
    viewModelScope.launch(Dispatchers.IO) {
      appInfoUtil.refresh()
      _allApps.value = appInfoUtil.getFilteredApps(null, true, true)
    }
  }

  fun hideApp(pkgName: String) {
    Hider.processApps(getApplication(), setOf(pkgName), Hider.Action.HIDE)
  }

  fun unhideApp(pkgName: String) {
    Hider.processApps(getApplication(), setOf(pkgName), Hider.Action.UNHIDE)
  }

  fun toggleAllApps() {
    val hidden = hiddenApps.value
    val managed = hiderStateRepo.managedApps.value
    val ctx = getApplication<Application>()

    if (hidden.containsAll(managed)) {
      Hider.processAll(ctx, Hider.Action.UNHIDE)
    } else {
      Hider.processAll(ctx, Hider.Action.HIDE)
    }
  }
}
