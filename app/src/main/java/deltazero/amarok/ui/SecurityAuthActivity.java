package deltazero.amarok.ui;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.SecurityUtil;

public class SecurityAuthActivity extends AppCompatActivity {
    private PasswordAuthFragment passwordAuthFragment;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo biometricPromptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        else
            overridePendingTransition(0, 0);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);

        passwordAuthFragment = new PasswordAuthFragment().setOnVerifiedCallback(isSucceeded -> {
            if (isSucceeded) onSuccess();
            else onFail();
        });

        biometricPrompt = new BiometricPrompt(this, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                passwordAuthenticate();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                onSuccess();
            }
        });

        biometricPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_required))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build();
    }

    @Override
    protected void onResume() {
        if (!SecurityUtil.isUnlockRequired()) finish();
        super.onResume();
        if (PrefMgr.getEnableAmarokBiometricAuth()) biometricAuthenticate();
        else passwordAuthenticate();
    }

    protected void onSuccess() {
        SecurityUtil.unlock();
        finish();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out);
        else
            overridePendingTransition(0, android.R.anim.fade_out);
    }

    protected void onFail() {
        finishAffinity();
    }

    private void passwordAuthenticate() {
        if (passwordAuthFragment.isAdded()) return;
        passwordAuthFragment.show(getSupportFragmentManager(), null);
    }

    private void biometricAuthenticate() {
        biometricPrompt.authenticate(biometricPromptInfo);
    }
}