package deltazero.amarok.utils

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class TargetUtilityContractTest {
  @After
  fun resetSecurityState() {
    SecurityUtil.lockAndDisguise()
  }

  @Test
  fun securityLockAndDisguiseStateTransitionsAreIndependentUntilResetTogether() {
    SecurityUtil.lockAndDisguise()

    assertTrue(securityFlag("locked"))
    assertTrue(securityFlag("disguised"))

    SecurityUtil.unlock()

    assertFalse(securityFlag("locked"))
    assertTrue(securityFlag("disguised"))

    SecurityUtil.dismissDisguise()

    assertFalse(securityFlag("locked"))
    assertFalse(securityFlag("disguised"))

    SecurityUtil.lockAndDisguise()

    assertTrue(securityFlag("locked"))
    assertTrue(securityFlag("disguised"))
  }

  @Test
  fun appInfoContainsIgnoreCaseHandlesNullEmptyAndAsciiCaseFolding() {
    assertFalse(appInfoContainsIgnoreCase(null, "app"))
    assertFalse(appInfoContainsIgnoreCase("Amarok", null))
    assertTrue(appInfoContainsIgnoreCase("Amarok", ""))
    assertTrue(appInfoContainsIgnoreCase("Calendar Hider", "calendar"))
    assertTrue(appInfoContainsIgnoreCase("deltazero.amarok.foss", "AMAROK"))
    assertFalse(appInfoContainsIgnoreCase("Calculator", "calendar"))
  }

  @Test
  fun appInfoRecordStoresConstructorValuesAndUsesRecordEquality() {
    val first = AppInfoUtil.AppInfo("pkg.name", "Label", true, false, null)
    val same = AppInfoUtil.AppInfo("pkg.name", "Label", true, false, null)
    val different = AppInfoUtil.AppInfo("pkg.other", "Label", true, false, null)

    assertEquals("pkg.name", first.packageName())
    assertEquals("Label", first.label())
    assertTrue(first.isSystemApp)
    assertFalse(first.isRootApp)
    assertEquals(first, same)
    assertFalse(first == different)
  }

  @Test
  fun updateChannelFromStringDefaultsToReleaseAndIgnoresCaseForKnownValues() {
    assertEquals(UpdateUtil.UpdateChannel.RELEASE, UpdateUtil.UpdateChannel.fromString("release"))
    assertEquals(UpdateUtil.UpdateChannel.BETA, UpdateUtil.UpdateChannel.fromString("BeTa"))
    assertEquals(UpdateUtil.UpdateChannel.RELEASE, UpdateUtil.UpdateChannel.fromString("nightly"))
    assertEquals(UpdateUtil.UpdateChannel.RELEASE, UpdateUtil.UpdateChannel.fromString(""))
  }

  @Test
  fun updateVersionComparisonStripsLeadingVAndUsesComparableVersionOrdering() {
    assertTrue(isNewerVersion("0.10.0", "v0.10.1"))
    assertTrue(isNewerVersion("v0.10.1-beta1", "v0.10.1"))
    assertFalse(isNewerVersion("v0.10.1", "0.10.1"))
    assertFalse(isNewerVersion("0.10.2", "v0.10.1"))
  }

  private fun securityFlag(name: String): Boolean {
    val field = SecurityUtil::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.getBoolean(null)
  }

  private fun appInfoContainsIgnoreCase(str: String?, searchStr: String?): Boolean {
    val method =
      AppInfoUtil::class
        .java
        .getDeclaredMethod("containsIgnoreCase", String::class.java, String::class.java)
    method.isAccessible = true
    return method.invoke(null, str, searchStr) as Boolean
  }

  private fun isNewerVersion(current: String, newVersion: String): Boolean {
    val method =
      UpdateUtil::class
        .java
        .getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
    method.isAccessible = true
    return method.invoke(null, current, newVersion) as Boolean
  }
}
