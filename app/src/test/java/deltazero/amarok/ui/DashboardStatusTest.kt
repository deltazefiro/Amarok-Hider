package deltazero.amarok.ui

import deltazero.amarok.core.Hider
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardStatusTest {
  @Test
  fun processingStateWinsOverPersistedItemCounts() {
    assertEquals(DashboardStatus.PROCESSING, dashboardStatus(Hider.State.PROCESSING, 0, 10))
    assertEquals(DashboardStatus.PROCESSING, dashboardStatus(Hider.State.PROCESSING, 10, 10))
  }

  @Test
  fun hiddenRequiresEveryManagedItemHidden() {
    assertEquals(DashboardStatus.HIDDEN, dashboardStatus(Hider.State.HIDDEN, 4, 4))
    assertEquals(DashboardStatus.PARTIALLY_HIDDEN, dashboardStatus(Hider.State.HIDDEN, 3, 4))
  }

  @Test
  fun visibleAndPartiallyHiddenComeFromHiddenItemCounts() {
    assertEquals(DashboardStatus.VISIBLE, dashboardStatus(Hider.State.VISIBLE, 0, 4))
    assertEquals(DashboardStatus.PARTIALLY_HIDDEN, dashboardStatus(Hider.State.VISIBLE, 1, 4))
    assertEquals(DashboardStatus.VISIBLE, dashboardStatus(Hider.State.HIDDEN, 0, 0))
  }
}
