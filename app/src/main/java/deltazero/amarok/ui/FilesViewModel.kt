package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.AmarokApplication
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderStateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilesViewModel(application: Application) : AndroidViewModel(application) {

  private val hiderStateRepo: HiderStateRepository =
    (application as AmarokApplication).hiderStateRepo

  val managedFolders: StateFlow<List<String>> =
    hiderStateRepo.managedFolders
      .map { it.toList() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        hiderStateRepo.managedFolders.value.toList(),
      )

  private val folderStatesFlow =
    Hider.folderStates.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      Hider.folderStates.value,
    )

  val hiddenFolders: StateFlow<Set<String>> =
    folderStatesFlow
      .map { states -> states.filterValues { it == Hider.State.HIDDEN }.keys }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val processingFolders: StateFlow<Set<String>> =
    folderStatesFlow
      .map { states -> states.filterValues { it == Hider.State.PROCESSING }.keys }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  fun hideFolder(path: String) {
    Hider.processFolders(getApplication(), setOf(path), Hider.Action.HIDE)
  }

  fun unhideFolder(path: String) {
    Hider.processFolders(getApplication(), setOf(path), Hider.Action.UNHIDE)
  }

  fun toggleAllFolders() {
    val hidden = hiddenFolders.value
    val managed = hiderStateRepo.managedFolders.value
    val ctx = getApplication<Application>()

    if (hidden.containsAll(managed)) {
      Hider.processAll(ctx, Hider.Action.UNHIDE)
    } else {
      Hider.processAll(ctx, Hider.Action.HIDE)
    }
  }

  fun addFolder(path: String) {
    viewModelScope.launch {
      if (path in hiderStateRepo.managedFolders.value) return@launch
      hiderStateRepo.addManagedFolder(path)
    }
  }

  fun removeFolder(path: String) {
    viewModelScope.launch {
      if (path !in hiderStateRepo.managedFolders.value) return@launch
      hiderStateRepo.removeManagedFolder(path)
    }
  }
}
