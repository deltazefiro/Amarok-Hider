package deltazero.amarok.utils

import org.junit.Assert.assertFalse
import org.junit.Test

class AppCenterUtilTest {
  @Test
  fun fossFlavorReportsAppCenterUnavailable() {
    assertFalse(AppCenterUtil.isAvailable())
  }

  @Test
  fun fossFlavorKeepsAnalyticsDisabled() {
    AppCenterUtil.setAnalyticsEnabled(true)

    assertFalse(AppCenterUtil.isAnalyticsEnabled())
  }

  @Test
  fun fossFlavorUpdateOperationsAreNoOps() {
    AppCenterUtil.cleanUpdatePostpone()
    AppCenterUtil.checkUpdate()
  }
}
