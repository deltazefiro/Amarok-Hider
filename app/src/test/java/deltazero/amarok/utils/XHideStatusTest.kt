package deltazero.amarok.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class XHideStatusTest {
  @Test
  fun deriveReturnsNotInstalledWhenModulePackageIsMissing() {
    assertEquals(XHideStatus.NotInstalled, derive(installed = false))
  }

  @Test
  fun deriveReturnsNotActivatedWhenModuleHasNotBoundToXposed() {
    assertEquals(XHideStatus.NotActivated, derive(moduleActive = false))
  }

  @Test
  fun deriveReturnsIncompatibleWhenProtocolDiffers() {
    assertEquals(
      XHideStatus.Incompatible(moduleProtocol = 2, appProtocol = 1),
      derive(moduleProtocol = 2, appProtocol = 1),
    )
  }

  @Test
  fun deriveReturnsPendingRebootWhenHooksAreNotLive() {
    assertEquals(XHideStatus.PendingReboot, derive(hooksLive = false))
  }

  @Test
  fun deriveReturnsErrorWhenNoHooksAttached() {
    assertEquals(XHideStatus.Error("No hooks attached"), derive(hookCount = 0))
  }

  @Test
  fun deriveReturnsHookErrorMessageWhenHooksReportFailure() {
    assertEquals(XHideStatus.Error("hook failed"), derive(hookError = "hook failed"))
  }

  @Test
  fun deriveReturnsActiveWhenModuleProtocolAndHooksAreHealthy() {
    assertEquals(
      XHideStatus.Active(
        apiVersion = 101,
        frameworkName = "LSPosed",
        frameworkVersion = "1.9.3",
        lastSyncTime = 1234L,
      ),
      derive(hookCount = 2),
    )
  }

  private fun derive(
    installed: Boolean = true,
    moduleActive: Boolean = true,
    moduleProtocol: Int = 1,
    appProtocol: Int = 1,
    hooksLive: Boolean = true,
    hookCount: Int = 1,
    hookError: String = "",
  ): XHideStatus =
    deriveXHideStatus(
      installed = installed,
      moduleActive = moduleActive,
      moduleProtocol = moduleProtocol,
      appProtocol = appProtocol,
      apiVersion = 101,
      frameworkName = "LSPosed",
      frameworkVersion = "1.9.3",
      lastSyncTime = 1234L,
      hooksLive = hooksLive,
      hookCount = hookCount,
      hookError = hookError,
    )
}
