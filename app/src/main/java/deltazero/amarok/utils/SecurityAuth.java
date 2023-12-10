package deltazero.amarok.utils;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.PasswordAuthFragment;

public class SecurityAuth {

    public static boolean locked = true;
    public static SecurityAuth instance;

    public interface SecurityAuthCallback {
        void onSecurityAuthCallback(boolean succeed);
    }

    private final SecurityAuthCallback callback;
    private final FragmentActivity activity;
    private PasswordAuthFragment passwordAuthFragment;
    private BiometricPrompt biometricPrompt;

    public SecurityAuth(FragmentActivity activity, SecurityAuthCallback callback) {
        this.callback = callback;
        this.activity = activity;

        if (instance != null) instance.cancel();
        instance = this;
    }

    public void authenticate() {

        if (PrefMgr.getAmarokPassword() == null || !locked) {
            callback.onSecurityAuthCallback(true);
            return;
        }

        Log.i("SecurityAuth", "Authentication triggered");
        if (PrefMgr.getEnableAmarokBiometricAuth()) biometricAuthenticate();
        else passwordAuthenticate();
    }

    public void cancel() {
        if (passwordAuthFragment != null && passwordAuthFragment.isAdded())
            passwordAuthFragment.dismiss();
        if (biometricPrompt != null)
            biometricPrompt.cancelAuthentication();
        callback.onSecurityAuthCallback(false);
    }

    private void passwordAuthenticate() {
        assert PrefMgr.getAmarokPassword() != null;

        passwordAuthFragment = new PasswordAuthFragment()
                .setOnVerifiedCallback(succeed -> {
                    if (succeed) locked = false;
                    callback.onSecurityAuthCallback(succeed);
                });

        passwordAuthFragment.show(activity.getSupportFragmentManager(), null);
    }

    private void biometricAuthenticate() {

        biometricPrompt = new BiometricPrompt(activity, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                passwordAuthenticate();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                locked = false;
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
