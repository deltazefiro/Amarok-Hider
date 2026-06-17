package deltazero.amarok.core

/**
 * When the app should re-arm its lock and disguise after being unlocked.
 *
 * The triggers form levels of strictness rather than independent toggles: [APP_BACKGROUND] also
 * covers the screen turning off (the foreground activity stops either way), and [ON_REOPEN] relies
 * on the fact that the in-memory lock flags default to locked on a cold start, so no runtime
 * trigger is needed for it.
 */
enum class LockTrigger(val key: String) {
  /** Lock whenever Amarok leaves the foreground — app switch or screen off (strictest). */
  APP_BACKGROUND("app_background"),
  /** Lock when the screen turns off; stays unlocked across app switches (default). */
  SCREEN_OFF("screen_off"),
  /** Never lock during a session — only re-locks when the process is killed and reopened. */
  ON_REOPEN("on_reopen");

  companion object {
    fun fromKey(key: String): LockTrigger = entries.firstOrNull { it.key == key } ?: SCREEN_OFF
  }
}
