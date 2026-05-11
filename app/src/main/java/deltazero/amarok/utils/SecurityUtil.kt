package deltazero.amarok.utils

import deltazero.amarok.AmarokApplication
import deltazero.amarok.core.Hider

object SecurityUtil {
  private var locked = true
  private var disguised = true

  @JvmStatic
  fun lockAndDisguise() {
    locked = true
    disguised = true
  }

  @JvmStatic
  fun unlock() {
    locked = false
  }

  @JvmStatic
  fun dismissDisguise() {
    disguised = false
  }

  @JvmStatic
  fun isDisguiseNeeded(app: AmarokApplication): Boolean {
    val settings = app.settingsRepo.settings.value
    if (settings.disableSecurityWhenUnhidden && Hider.getState() == Hider.State.VISIBLE)
      return false
    return settings.disguise && disguised
  }

  @JvmStatic
  fun isUnlockRequired(app: AmarokApplication): Boolean {
    val settings = app.settingsRepo.settings.value
    if (settings.disableSecurityWhenUnhidden && Hider.getState() == Hider.State.VISIBLE)
      return false
    return settings.password != null && locked
  }
}
