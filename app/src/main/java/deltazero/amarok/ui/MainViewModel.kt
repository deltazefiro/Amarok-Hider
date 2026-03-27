package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.filehider.FileHider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val hiderState: StateFlow<Hider.State> =
    Hider.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Hider.getState())

  val managedAppCount: StateFlow<Int> = MutableStateFlow(PrefMgr.getHideApps().size).asStateFlow()
  val managedFolderCount: StateFlow<Int> =
    MutableStateFlow(PrefMgr.getHideFilePath().size).asStateFlow()

  val appHiderName: StateFlow<String> =
    Hider.appHiderMode
      .map { mode -> AppHider.build(getApplication(), mode).name }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppHider.build(application, Hider.getAppHiderMode()).name,
      )

  val fileHiderName: StateFlow<String> =
    Hider.fileHiderMode
      .map { mode -> FileHider.build(getApplication(), mode).name }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FileHider.build(application, Hider.getFileHiderMode()).name,
      )

  fun refreshCounts() {
    (managedAppCount as MutableStateFlow).value = PrefMgr.getHideApps().size
    (managedFolderCount as MutableStateFlow).value = PrefMgr.getHideFilePath().size
  }
}
