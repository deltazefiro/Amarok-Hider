package deltazero.amarok.ui.settings;


import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.CountdownConfirmDialog;
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
        appLockPref.setIcon(R.drawable.lock_black_24dp);
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
        biometricPref.setIcon(R.drawable.fingerprint_24dp_1f1f1f_fill0_wght400_grad0_opsz24);
        biometricPref.setTitle(R.string.biometric_auth);
        biometricPref.setSummary(R.string.biometric_auth_description);
        biometricPref.setChecked(PrefMgr.getEnableAmarokBiometricAuth());
        biometricPref.setEnabled(PrefMgr.getAmarokPassword() != null);
        addPreference(biometricPref);

        var disguisePref = new MaterialSwitchPreference(activity);
        disguisePref.setKey(PrefMgr.ENABLE_DISGUISE);
        disguisePref.setIcon(R.drawable.calendar_month_24dp_1f1f1f_fill0_wght400_grad0_opsz24);
        disguisePref.setTitle(R.string.disguise);
        disguisePref.setSummary(R.string.disguise_description);
        disguisePref.setChecked(PrefMgr.getEnableDisguise());
        disguisePref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enableDisguise = (boolean) newValue;
            PrefMgr.setDoShowQuitDisguiseInstuct(true);
            if (enableDisguise)
                SecurityUtil.lockAndDisguise();
            LauncherIconController.setIconState(activity,
                    enableDisguise ? LauncherIconController.IconState.DISGUISED : LauncherIconController.IconState.VISIBLE);
            return true;
        });
        addPreference(disguisePref);

        var hideAmarokIconPref = new MaterialSwitchPreference(activity);
        hideAmarokIconPref.setKey(PrefMgr.HIDE_AMAROK_ICON);
        hideAmarokIconPref.setIcon(R.drawable.hide_source_black_24dp);
        hideAmarokIconPref.setTitle(R.string.hide_amarok_icon);
        hideAmarokIconPref.setSummary(R.string.hide_amarok_icon_description);
        hideAmarokIconPref.setChecked(PrefMgr.getHideAmarokIcon());
        hideAmarokIconPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean hideIcon = (boolean) newValue;
            if (hideIcon) {
                // Show countdown dialog before hiding icon
                new CountdownConfirmDialog.Builder(activity)
                        .setTitle(R.string.hide_amarok_icon_dialog_title)
                        .setMessage(R.string.hide_amarok_icon_dialog_message)
                        .setCountdownSeconds(10)
                        .setOnConfirmAction(() -> {
                            // When hiding icon, disable disguise and turn it off
                            disguisePref.setChecked(false);
                            disguisePref.setEnabled(false);
                            LauncherIconController.setIconState(activity, LauncherIconController.IconState.HIDDEN);
                            hideAmarokIconPref.setChecked(true);
                        })
                        .setOnCancelAction(() -> {
                            // User cancelled, revert the switch state
                            hideAmarokIconPref.setChecked(false);
                        })
                        .show();
                return false; // Don't change the preference yet
            } else {
                // When showing icon, re-enable disguise option
                disguisePref.setEnabled(true);
                LauncherIconController.setIconState(activity, LauncherIconController.IconState.VISIBLE);
                return true;
            }
        });
        // Set initial state: if icon is hidden, disable disguise option
        disguisePref.setEnabled(!PrefMgr.getHideAmarokIcon());
        addPreference(hideAmarokIconPref);

        var hideFromRecentsPref = new MaterialSwitchPreference(activity);
        hideFromRecentsPref.setKey(PrefMgr.HIDE_FROM_RECENTS);
        hideFromRecentsPref.setIcon(R.drawable.search_activity_24dp_1f1f1f_fill0_wght400_grad0_opsz24);
        hideFromRecentsPref.setTitle(R.string.hide_from_recents);
        hideFromRecentsPref.setSummary(R.string.hide_from_recents_description);
        hideFromRecentsPref.setChecked(PrefMgr.getHideFromRecents());
        hideFromRecentsPref.setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(activity, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
            return true;
        });
        addPreference(hideFromRecentsPref);

        var allowScreenshotPref = new MaterialSwitchPreference(activity);
        allowScreenshotPref.setKey(PrefMgr.BLOCK_SCREENSHOTS);
        allowScreenshotPref.setIcon(R.drawable.cancel_presentation_24dp_1f1f1f_fill0_wght400_grad0_opsz24);
        allowScreenshotPref.setTitle(R.string.block_screenshots);
        allowScreenshotPref.setSummary(R.string.block_screenshots_description);
        allowScreenshotPref.setChecked(PrefMgr.getBlockScreenshots());
        allowScreenshotPref.setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(activity, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
            return true;
        });
        addPreference(allowScreenshotPref);

        var disableSecurityWhenUnhiddenPref = new MaterialSwitchPreference(activity);
        disableSecurityWhenUnhiddenPref.setKey(PrefMgr.DISABLE_SECURITY_WHEN_UNHIDDEN);
        disableSecurityWhenUnhiddenPref.setIcon(R.drawable.encrypted_off_24dp);
        disableSecurityWhenUnhiddenPref.setTitle(R.string.disable_security_when_unhidden);
        disableSecurityWhenUnhiddenPref.setSummary(R.string.disable_security_when_unhidden_description);
        disableSecurityWhenUnhiddenPref.setChecked(PrefMgr.getDisableSecurityWhenUnhidden());
        addPreference(disableSecurityWhenUnhiddenPref);

        var disableToastsPref = new MaterialSwitchPreference(activity);
        disableToastsPref.setKey(PrefMgr.DISABLE_TOASTS);
        disableToastsPref.setIcon(R.drawable.speaker_notes_off_24dp);
        disableToastsPref.setTitle(R.string.disable_toasts);
        disableToastsPref.setSummary(R.string.disable_toasts_description);
        disableToastsPref.setChecked(PrefMgr.getDisableToasts());
        addPreference(disableToastsPref);
    }
}
