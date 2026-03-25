package deltazero.amarok.core

sealed interface HideAction {
  data class Hide(val disableOnly: Boolean = false) : HideAction

  data object Unhide : HideAction
}
