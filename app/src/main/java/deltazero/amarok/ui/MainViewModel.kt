package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import deltazero.amarok.apphider.BaseAppHider
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.filehider.BaseFileHider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val hiderState: StateFlow<Hider.State> =
    Hider.state
      .asFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Hider.getState())

  val managedAppCount: StateFlow<Int> = MutableStateFlow(PrefMgr.getHideApps().size).asStateFlow()
  val managedFolderCount: StateFlow<Int> =
    MutableStateFlow(PrefMgr.getHideFilePath().size).asStateFlow()

  private val _appHiderName =
    MutableStateFlow(BaseAppHider.fromMode(application, PrefMgr.getAppHiderMode()).name)
  val appHiderName: StateFlow<String> = _appHiderName.asStateFlow()

  private val _fileHiderName =
    MutableStateFlow(BaseFileHider.fromMode(application, PrefMgr.getFileHiderMode()).name)
  val fileHiderName: StateFlow<String> = _fileHiderName.asStateFlow()

  fun refreshCounts() {
    (managedAppCount as MutableStateFlow).value = PrefMgr.getHideApps().size
    (managedFolderCount as MutableStateFlow).value = PrefMgr.getHideFilePath().size
  }

  fun refreshHiderNames() {
    val ctx = getApplication<Application>()
    _appHiderName.value = BaseAppHider.fromMode(ctx, PrefMgr.getAppHiderMode()).name
    _fileHiderName.value = BaseFileHider.fromMode(ctx, PrefMgr.getFileHiderMode()).name
  }
}
