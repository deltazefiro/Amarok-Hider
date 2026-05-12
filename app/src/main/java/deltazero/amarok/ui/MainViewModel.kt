package deltazero.amarok.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.apphider.AppHider
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderStateRepository
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.filehider.FileHider
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val hiderStateRepo: HiderStateRepository,
  private val settingsRepo: SettingsRepository,
) : ViewModel() {

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
      .map { settings -> AppHider.build(context, settings.appHiderMode).name }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppHider.build(context, settingsRepo.settings.value.appHiderMode).name,
      )

  val fileHiderName: StateFlow<String> =
    settingsRepo.settings
      .map { settings -> FileHider.build(context, settings.fileHiderMode).name }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FileHider.build(context, settingsRepo.settings.value.fileHiderMode).name,
      )
}
