package deltazero.amarok.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import deltazero.amarok.Hider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel : ViewModel() {
    val hiderState: StateFlow<Hider.State> = Hider.state
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Hider.getState())
}
