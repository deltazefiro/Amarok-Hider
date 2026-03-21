package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FilesViewModel(application: Application) : AndroidViewModel(application) {

  private val _managedFolders = MutableStateFlow(PrefMgr.getHideFilePath().toList())
  val managedFolders: StateFlow<List<String>> = _managedFolders.asStateFlow()

  private val folderStatesFlow =
    Hider.folderStates.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      Hider.folderStates.value,
    )

  val hiddenFolders: StateFlow<Set<String>> =
    folderStatesFlow
      .map { states -> states.filterValues { it == Hider.FolderStatus.HIDDEN }.keys }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val processingFolders: StateFlow<Set<String>> =
    folderStatesFlow
      .map { states -> states.filterValues { it == Hider.FolderStatus.PROCESSING }.keys }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  fun hideFolder(path: String) {
    Hider.hideFolder(getApplication(), path)
  }

  fun unhideFolder(path: String) {
    Hider.unhideFolder(getApplication(), path)
  }

  fun toggleAllFolders() {
    val hidden = hiddenFolders.value
    val managed = PrefMgr.getHideFilePath()
    val ctx = getApplication<Application>()

    if (hidden.containsAll(managed)) {
      Hider.unhide(ctx)
    } else {
      Hider.hide(ctx)
    }
  }

  fun addFolder(path: String) {
    val current = PrefMgr.getHideFilePath()
    current.add(path)
    PrefMgr.setHideFilePath(current)
    _managedFolders.value = current.toList()
  }

  fun removeFolder(path: String) {
    val current = PrefMgr.getHideFilePath()
    current.remove(path)
    PrefMgr.setHideFilePath(current)
    _managedFolders.value = current.toList()
  }
}
