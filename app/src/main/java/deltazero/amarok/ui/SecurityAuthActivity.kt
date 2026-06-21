package deltazero.amarok.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import dagger.hilt.android.AndroidEntryPoint
import deltazero.amarok.R
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.utils.SecurityUtil
import javax.inject.Inject

@AndroidEntryPoint
open class SecurityAuthActivity : AppCompatActivity() {
  @Inject lateinit var settingsRepo: SettingsRepository

  private lateinit var passwordAuthFragment: PasswordAuthFragment
  private lateinit var biometricPrompt: BiometricPrompt
  private lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo

  override fun onCreate(savedInstanceState: Bundle?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
    } else {
      @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    super.onCreate(savedInstanceState)
    // The password prompt is a BottomSheetDialogFragment rendered in its own window, and the
    // biometric prompt is system UI; this activity is just their transient host, so its content is
    // an empty Compose surface (the window background acts as the scrim behind the sheet).
    setContent {}

    passwordAuthFragment =
      PasswordAuthFragment().setOnVerifiedCallback { isSucceeded ->
        if (isSucceeded) onSuccess() else onFail()
      }

    biometricPrompt =
      BiometricPrompt(
        this,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            passwordAuthenticate()
          }

          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
          }
        },
      )

    biometricPromptInfo =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.unlock_required))
        .setNegativeButtonText(getString(android.R.string.cancel))
        .build()
  }

  override fun onResume() {
    if (!SecurityUtil.isUnlockRequired(settingsRepo)) finish()
    super.onResume()
    if (settingsRepo.settings.value.biometricAuth) biometricAuthenticate()
    else passwordAuthenticate()
  }

  protected open fun onSuccess() {
    SecurityUtil.unlock()
    finish()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
    } else {
      @Suppress("DEPRECATION") overridePendingTransition(0, android.R.anim.fade_out)
    }
  }

  protected open fun onFail() {
    finishAffinity()
  }

  private fun passwordAuthenticate() {
    if (passwordAuthFragment.isAdded) return
    passwordAuthFragment.show(supportFragmentManager, null)
  }

  private fun biometricAuthenticate() {
    biometricPrompt.authenticate(biometricPromptInfo)
  }
}
