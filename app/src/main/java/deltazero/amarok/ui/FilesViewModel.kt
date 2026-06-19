package deltazero.amarok.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderStateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FilesViewModel
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val hiderStateRepo: HiderStateRepository,
) : ViewModel() {

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
    Hider.processFolders(context, setOf(path), Hider.Action.HIDE)
  }

  fun unhideFolder(path: String) {
    Hider.processFolders(context, setOf(path), Hider.Action.UNHIDE)
  }

  fun toggleAllFolders() {
    val hidden = hiddenFolders.value
    val managed = hiderStateRepo.managedFolders.value

    val action = if (hidden.containsAll(managed)) Hider.Action.UNHIDE else Hider.Action.HIDE
    Hider.processFolders(context, managed, action)
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
