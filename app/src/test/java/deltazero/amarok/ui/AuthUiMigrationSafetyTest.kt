package deltazero.amarok.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
class AuthUiMigrationSafetyTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val themedContext = ContextThemeWrapper(context, R.style.Theme_Amarok)
  private val inflater: LayoutInflater = LayoutInflater.from(themedContext)

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
  fun passwordAuthLayoutKeepsIdsAndNumericPasswordInputUsedByFragment() {
    val view = inflater.inflate(R.layout.dialog_security, null, false)

    assertNotNull(view.findViewById<TextInputLayout>(R.id.security_dialog_til_password_input))
    val passwordInput = view.findViewById<TextInputEditText>(R.id.security_dialog_et_password_input)
    assertNotNull(passwordInput)
    assertTrue(passwordInput.inputType and InputType.TYPE_CLASS_NUMBER != 0)
    assertTrue(passwordInput.inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0)
    assertNotNull(view.findViewById<MaterialButton>(R.id.security_dialog_bt_cancel))
    assertNotNull(view.findViewById<MaterialButton>(R.id.security_dialog_bt_unlock))
  }

  @Test
  fun passwordAuthCallbackSetterIsFluentAndStoresCallback() {
    val fragment = PasswordAuthFragment()
    val callback = PasswordAuthFragment.OnVerifiedCallback {}

    assertSame(fragment, fragment.setOnVerifiedCallback(callback))
    assertSame(callback, privateField(fragment, "onVerifiedCallback"))
  }

  @Test
  fun setPasswordLayoutKeepsIdsAndNumericPasswordInputsUsedByFragment() {
    val view = inflater.inflate(R.layout.dialog_set_password, null, false)

    assertNotNull(view.findViewById<TextInputLayout>(R.id.set_password_dialog_til_password))
    assertNotNull(view.findViewById<TextInputLayout>(R.id.set_password_dialog_til_confirm_password))
    val passwordInput = view.findViewById<TextInputEditText>(R.id.set_password_dialog_et_password)
    val confirmInput =
      view.findViewById<TextInputEditText>(R.id.set_password_dialog_et_confirm_password)

    assertNotNull(passwordInput)
    assertNotNull(confirmInput)
    assertTrue(passwordInput.inputType and InputType.TYPE_CLASS_NUMBER != 0)
    assertTrue(passwordInput.inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0)
    assertTrue(confirmInput.inputType and InputType.TYPE_CLASS_NUMBER != 0)
    assertTrue(confirmInput.inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0)
    assertNotNull(view.findViewById<MaterialButton>(R.id.set_password_dialog_bt_cancel))
    assertNotNull(view.findViewById<MaterialButton>(R.id.set_password_dialog_bt_ok))
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
  fun countdownConfirmDialogBuilderDefaultsMatchCurrentJavaFields() {
    val dialog = CountdownConfirmDialog.Builder(context).build()

    assertEquals("", privateField(dialog, "title"))
    assertEquals("", privateField(dialog, "message"))
    assertEquals(5, privateField(dialog, "countdownSeconds"))
  }

  @Test
  fun directAuthAndDialogUiFlowsRemainBuildOnlyCharacterized() {
    // Showing these flows exercises app settings, biometric prompts, bottom sheets, or delayed
    // dialog handlers. Keep this migration guard lightweight and rely on compilation plus the
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
