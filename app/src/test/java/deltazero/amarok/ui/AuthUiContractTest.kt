package deltazero.amarok.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import deltazero.amarok.AmarokActivity
import deltazero.amarok.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class AuthUiContractTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun mainActivityStillInheritsAmarokActivityGuards() {
    assertSame(AmarokActivity::class.java, MainActivity::class.java.superclass)
  }

  @Test
  fun securityAuthActivitiesManifestContractIsStable() {
    val securityAuthInfo = activityInfo(SecurityAuthActivity::class.java)
    val quickSettingsAuthInfo = activityInfo(SecurityAuthForQSActivity::class.java)

    assertFalse(securityAuthInfo.exported)
    assertTrue(securityAuthInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
    assertTrue(securityAuthInfo.flags and ActivityInfo.FLAG_NO_HISTORY != 0)

    assertSame(SecurityAuthActivity::class.java, SecurityAuthForQSActivity::class.java.superclass)
    assertFalse(quickSettingsAuthInfo.exported)
    assertTrue(quickSettingsAuthInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
    assertTrue(quickSettingsAuthInfo.flags and ActivityInfo.FLAG_NO_HISTORY != 0)
    assertEquals(R.style.Theme_Amarok_Transparent, quickSettingsAuthInfo.themeResource)
  }

  @Test
  fun passwordAuthCallbackSetterIsFluentAndStoresCallback() {
    val fragment = PasswordAuthFragment()
    val callback = PasswordAuthFragment.OnVerifiedCallback {}

    assertSame(fragment, fragment.setOnVerifiedCallback(callback))
    assertSame(callback, privateField(fragment, "onVerifiedCallback"))
  }

  @Test
  fun setPasswordCallbackSetterIsFluentAndStoresCallback() {
    val fragment = SetPasswordFragment()
    val callback = SetPasswordFragment.OnSetPasswordCallback {}

    assertSame(fragment, fragment.setCallback(callback))
    assertSame(callback, privateField(fragment, "callback"))
  }

  @Test
  fun countdownConfirmDialogBuilderStoresConfiguredStateWithoutShowingDialog() {
    val onConfirm = Runnable {}
    val onCancel = Runnable {}

    val dialog =
      CountdownConfirmDialog.Builder(context)
        .setTitle("Danger")
        .setMessage(R.string.password_length_error)
        .setCountdownSeconds(3)
        .setOnConfirmAction(onConfirm)
        .setOnCancelAction(onCancel)
        .build()

    assertSame(context, privateField(dialog, "context"))
    assertEquals("Danger", privateField(dialog, "title"))
    assertEquals(context.getString(R.string.password_length_error), privateField(dialog, "message"))
    assertEquals(3, privateField(dialog, "countdownSeconds"))
    assertSame(onConfirm, privateField(dialog, "onConfirmAction"))
    assertSame(onCancel, privateField(dialog, "onCancelAction"))
  }

  @Test
  fun countdownConfirmDialogBuilderDefaultsMatchCurrentFields() {
    val dialog = CountdownConfirmDialog.Builder(context).build()

    assertEquals("", privateField(dialog, "title"))
    assertEquals("", privateField(dialog, "message"))
    assertEquals(5, privateField(dialog, "countdownSeconds"))
  }

  @Test
  fun directAuthAndDialogUiFlowsRemainBuildOnlyCharacterized() {
    // Showing these flows exercises app settings, biometric prompts, bottom sheets, or delayed
    // dialog handlers. Keep this contract check lightweight and rely on compilation plus the
    // stable manifest/layout/builder contracts above rather than brittle full UI execution.
    assertNotNull(SecurityAuthActivity::class.java.getDeclaredMethod("onSuccess"))
    assertNotNull(SecurityAuthActivity::class.java.getDeclaredMethod("onFail"))
    assertNotNull(CountdownConfirmDialog::class.java.getDeclaredMethod("show"))
  }

  private fun activityInfo(activityClass: Class<*>) =
    context.packageManager.getActivityInfo(
      ComponentName(context, activityClass),
      PackageManager.GET_META_DATA,
    )

  private fun privateField(target: Any, fieldName: String): Any? {
    val field = target.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(target)
  }
}
