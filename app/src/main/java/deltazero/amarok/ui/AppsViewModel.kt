package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.utils.AppInfoUtil
import deltazero.amarok.utils.AppInfoUtil.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {
  private val appInfoUtil = AppInfoUtil(application)

  private val _managedApps = MutableStateFlow<List<AppInfo>>(emptyList())
  val managedApps: StateFlow<List<AppInfo>> = _managedApps.asStateFlow()

  val hiddenApps: StateFlow<Set<String>> =
    Hider.hiddenApps.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      Hider.hiddenApps.value,
    )

  init {
    loadManagedApps()
  }

  fun loadManagedApps() {
    viewModelScope.launch(Dispatchers.IO) {
      appInfoUtil.refresh()
      val managed = PrefMgr.getHideApps()
      val allApps = appInfoUtil.getFilteredApps(null, true, true)
      _managedApps.value = allApps.filter { managed.contains(it.packageName()) }
    }
  }

  fun hideApp(pkgName: String) {
    Hider.hideApp(getApplication(), pkgName)
  }

  fun unhideApp(pkgName: String) {
    Hider.unhideApp(getApplication(), pkgName)
  }

  fun toggleAllApps() {
    val hidden = hiddenApps.value
    val managed = PrefMgr.getHideApps()
    val ctx = getApplication<Application>()

    if (hidden.containsAll(managed)) {
      Hider.unhide(ctx)
    } else {
      Hider.hide(ctx)
    }
  }
}
