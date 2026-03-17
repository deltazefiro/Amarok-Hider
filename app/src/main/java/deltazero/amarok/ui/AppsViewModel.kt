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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val appInfoUtil = AppInfoUtil(application)

    private val _managedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val managedApps: StateFlow<List<AppInfo>> = _managedApps.asStateFlow()

    val hiddenApps: StateFlow<Set<String>> = Hider.hiddenApps
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            Hider.hiddenApps.value ?: emptySet())

    private val _processingApps = MutableStateFlow<Set<String>>(emptySet())
    val processingApps: StateFlow<Set<String>> = _processingApps.asStateFlow()

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
        _processingApps.value = _processingApps.value + pkgName
        val ctx = getApplication<Application>()
        Hider.hideApp(ctx, pkgName)
        // Remove from processing after a short delay (backend is async)
        viewModelScope.launch(Dispatchers.IO) {
            // Wait for the hider thread to process
            Thread.sleep(500)
            _processingApps.value = _processingApps.value - pkgName
        }
    }

    fun unhideApp(pkgName: String) {
        _processingApps.value = _processingApps.value + pkgName
        val ctx = getApplication<Application>()
        Hider.unhideApp(ctx, pkgName)
        viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            _processingApps.value = _processingApps.value - pkgName
        }
    }

    fun toggleAllApps() {
        val hidden = hiddenApps.value
        val managed = PrefMgr.getHideApps()
        val ctx = getApplication<Application>()

        if (hidden.containsAll(managed)) {
            // All hidden -> unhide all
            Hider.unhide(ctx)
        } else {
            // Some visible -> hide all
            Hider.hide(ctx)
        }
    }
}
