package deltazero.amarok.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import deltazero.amarok.core.Hider
import deltazero.amarok.core.PrefMgr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _managedFolders = MutableStateFlow(PrefMgr.getHideFilePath().toList())
    val managedFolders: StateFlow<List<String>> = _managedFolders.asStateFlow()

    val hiddenFolders: StateFlow<Set<String>> = Hider.hiddenFolders
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            Hider.hiddenFolders.value ?: emptySet())

    private val _processingFolders = MutableStateFlow<Set<String>>(emptySet())
    val processingFolders: StateFlow<Set<String>> = _processingFolders.asStateFlow()

    fun hideFolder(path: String) {
        _processingFolders.value = _processingFolders.value + path
        Hider.hideFolder(getApplication(), path)
        viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            _processingFolders.value = _processingFolders.value - path
        }
    }

    fun unhideFolder(path: String) {
        _processingFolders.value = _processingFolders.value + path
        Hider.unhideFolder(getApplication(), path)
        viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            _processingFolders.value = _processingFolders.value - path
        }
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
