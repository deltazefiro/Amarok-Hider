package deltazero.amarok.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HiderControllerTest {
  @Test
  fun targetsForActionSkipsOnlyKeysAlreadyProcessing() {
    val states =
      mapOf(
        "visible" to Hider.State.VISIBLE,
        "processing" to Hider.State.PROCESSING,
        "hidden" to Hider.State.HIDDEN,
      )
    val processing = setOf("processing")
    val managed = setOf("visible", "processing", "hidden")

    val toHide =
      HiderController.targetsForActionForTest(
        managed = managed,
        states = states,
        processing = processing,
        action = Hider.Action.HIDE,
      )
    val toUnhide =
      HiderController.targetsForActionForTest(
        managed = managed,
        states = states,
        processing = processing,
        action = Hider.Action.UNHIDE,
      )

    assertEquals(setOf("visible"), toHide)
    assertEquals(setOf("hidden"), toUnhide)
  }

  @Test
  fun cancelledUnhideTargetsAllManagedKeysRegardlessOfPersistedHiddenState() {
    val managed = setOf("visible", "hidden")
    val hidden = setOf("hidden")

    val toHide =
      HiderController.cancelledTargetsForActionForTest(
        managed = managed,
        hidden = hidden,
        action = Hider.Action.HIDE,
      )
    val toUnhide =
      HiderController.cancelledTargetsForActionForTest(
        managed = managed,
        hidden = hidden,
        action = Hider.Action.UNHIDE,
      )

    assertEquals(setOf("visible"), toHide)
    assertEquals(managed, toUnhide)
  }

  @Test
  fun aggregateStateUsesPerItemProcessingWithoutGlobalFlag() {
    assertEquals(
      Hider.State.PROCESSING,
      HiderController.computeStateForTest(
        folderStates = mapOf("/folder" to Hider.State.PROCESSING),
        appStates = mapOf("app" to Hider.State.HIDDEN),
      ),
    )
    assertEquals(
      Hider.State.HIDDEN,
      HiderController.computeStateForTest(
        folderStates = mapOf("/folder" to Hider.State.HIDDEN),
        appStates = mapOf("app" to Hider.State.HIDDEN),
      ),
    )
    assertEquals(
      Hider.State.VISIBLE,
      HiderController.computeStateForTest(
        folderStates = mapOf("/folder" to Hider.State.VISIBLE),
        appStates = mapOf("app" to Hider.State.HIDDEN),
      ),
    )
  }
}
