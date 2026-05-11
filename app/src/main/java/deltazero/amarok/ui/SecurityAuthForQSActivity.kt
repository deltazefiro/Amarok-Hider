package deltazero.amarok.ui

import deltazero.amarok.core.Hider

class SecurityAuthForQSActivity : SecurityAuthActivity() {
  override fun onSuccess() {
    Hider.processAll(this, Hider.Action.UNHIDE)
    super.onSuccess()
  }
}
