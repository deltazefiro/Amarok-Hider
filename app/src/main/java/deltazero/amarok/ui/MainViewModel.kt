package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.AmarokApplication
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.core.Hider
import deltazero.amarok.filehider.FileHider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as AmarokApplication
  private val hiderStateRepo = app.hiderStateRepo
  private val settingsRepo = app.settingsRepo

  val hiderState: StateFlow<Hider.State> =
    Hider.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Hider.getState())

  val managedAppCount: StateFlow<Int> =
    hiderStateRepo.managedApps
      .map { it.size }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        hiderStateRepo.managedApps.value.size,
      )

  val managedFolderCount: StateFlow<Int> =
    hiderStateRepo.managedFolders
      .map { it.size }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        hiderStateRepo.managedFolders.value.size,
      )

  val appHiderName: StateFlow<String> =
    settingsRepo.settings
      .map { settings -> AppHider.build(getApplication(), settings.appHiderMode).name }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppHider.build(application, settingsRepo.settings.value.appHiderMode).name,
      )

  val fileHiderName: StateFlow<String> =
    settingsRepo.settings
      .map { settings -> FileHider.build(getApplication(), settings.fileHiderMode).name }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FileHider.build(application, settingsRepo.settings.value.fileHiderMode).name,
      )
}
