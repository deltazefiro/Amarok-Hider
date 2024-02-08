package deltazero.amarok.ui.settings;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.SetPasswordFragment;
import deltazero.amarok.utils.HashUtil;
import deltazero.amarok.utils.LauncherIconController;
import deltazero.amarok.utils.SecurityUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class PrivacyCategory extends BaseCategory {

    private MaterialSwitchPreference biometricPref;

    public PrivacyCategory(@NonNull FragmentActivity activity, PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.security);

        var appLockPref = new MaterialSwitchPreference(activity);
        appLockPref.setTitle(R.string.app_lock);
        appLockPref.setSummary(R.string.app_lock_description);
        appLockPref.setChecked(PrefMgr.getAmarokPassword() != null);
        appLockPref.setOnPreferenceClickListener(preference -> {
            if (appLockPref.isChecked()) {
                new SetPasswordFragment()
                        .setCallback(password -> {
                            PrefMgr.setAmarokPassword(password == null ? null : HashUtil.calculateHash(password));
                            SecurityUtil.unlock(); /* Avoid password right after enable the app lock */
                            appLockPref.setChecked(password != null);
                            biometricPref.setEnabled(PrefMgr.getAmarokPassword() != null);
                        })
                        .show(activity.getSupportFragmentManager(), null);
            } else {
                PrefMgr.setAmarokPassword(null);
                biometricPref.setEnabled(PrefMgr.getAmarokPassword() != null);
            }
            return false;
        });
        addPreference(appLockPref);

        biometricPref = new MaterialSwitchPreference(activity);
        biometricPref.setKey(PrefMgr.ENABLE_AMAROK_BIOMETRIC_AUTH);
        biometricPref.setTitle(R.string.biometric_auth);
        biometricPref.setSummary(R.string.biometric_auth_description);
        biometricPref.setChecked(PrefMgr.getEnableAmarokBiometricAuth());
        biometricPref.setEnabled(PrefMgr.getAmarokPassword() != null);
        addPreference(biometricPref);

        var disguisePref = new MaterialSwitchPreference(activity);
        disguisePref.setKey(PrefMgr.ENABLE_DISGUISE);
        disguisePref.setTitle(R.string.disguise);
        disguisePref.setSummary(R.string.disguise_description);
        disguisePref.setChecked(PrefMgr.getEnableDisguise());
        disguisePref.setOnPreferenceChangeListener((preference, newValue) -> {
            PrefMgr.setDoShowQuitDisguiseInstuct(true);
            if ((boolean) newValue) SecurityUtil.lockAndDisguise();
            LauncherIconController.switchDisguise(activity, (boolean) newValue);
            return true;
        });
        addPreference(disguisePref);
    }
}
