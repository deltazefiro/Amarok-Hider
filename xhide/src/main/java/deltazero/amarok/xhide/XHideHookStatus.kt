package deltazero.amarok.xhide

object XHideHookStatus {
  @Volatile
  var hookCount = 0
    private set

  @Volatile
  var errorMessage = ""
    private set

  fun recordSuccess() {
    hookCount++
  }

  fun recordFailure(message: String) {
    errorMessage = if (errorMessage.isEmpty()) message else "$errorMessage; $message"
  }
}
