package deltazero.amarok.utils;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.PasswordAuthFragment;

public class SecurityAuth {

    public interface SecurityAuthCallback {
        void onSecurityAuthCallback(boolean succeed);
    }

    private SecurityAuthCallback callback;
    private FragmentActivity activity;
    private PrefMgr prefMgr;
    private PasswordAuthFragment passwordAuthFragment;

    public SecurityAuth(FragmentActivity activity, SecurityAuthCallback callback) {
        this.callback = callback;
        this.activity = activity;
        prefMgr = new PrefMgr(activity);
        if (passwordAuthFragment == null)
            passwordAuthFragment = new PasswordAuthFragment();
    }

    public void authenticate() {
        if (passwordAuthFragment.isAdded())
            passwordAuthFragment.dismiss();
        if (prefMgr.getAmarokPassword() != null) {
            if (prefMgr.getEnableAmarokBiometricAuth()) biometricAuthenticate();
            else passwordAuthenticate();
        } else {
            callback.onSecurityAuthCallback(true);
        }
    }

    private void passwordAuthenticate() {
        assert prefMgr.getAmarokPassword() != null;
        passwordAuthFragment
                .setOnVerifiedCallback(succeed -> callback.onSecurityAuthCallback(succeed))
                .show(activity.getSupportFragmentManager(), null);
    }

    private void biometricAuthenticate() {

        var biometricPrompt = new BiometricPrompt(activity, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                passwordAuthenticate();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                callback.onSecurityAuthCallback(true);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        var promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.unlock_required))
                .setNegativeButtonText(activity.getString(android.R.string.cancel))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
