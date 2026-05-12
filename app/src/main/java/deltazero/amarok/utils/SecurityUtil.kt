package deltazero.amarok.utils

import deltazero.amarok.AmarokApplication
import deltazero.amarok.core.Hider
import deltazero.amarok.core.SettingsRepository

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
    return isDisguiseNeeded(app.settingsRepo)
  }

  fun isDisguiseNeeded(settingsRepo: SettingsRepository): Boolean {
    val settings = settingsRepo.settings.value
    if (settings.disableSecurityWhenUnhidden && Hider.getState() == Hider.State.VISIBLE)
      return false
    return settings.disguise && disguised
  }

  @JvmStatic
  fun isUnlockRequired(app: AmarokApplication): Boolean {
    return isUnlockRequired(app.settingsRepo)
  }

  fun isUnlockRequired(settingsRepo: SettingsRepository): Boolean {
    val settings = settingsRepo.settings.value
    if (settings.disableSecurityWhenUnhidden && Hider.getState() == Hider.State.VISIBLE)
      return false
    return settings.password != null && locked
  }
}
