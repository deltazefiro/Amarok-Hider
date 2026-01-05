package deltazero.amarok.ui.settings;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;

import com.hjq.permissions.OnPermissionCallback;

import java.util.List;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.QuickHideService;
import deltazero.amarok.R;
import deltazero.amarok.ui.ColorPickerDialog;
import deltazero.amarok.utils.PermissionUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class QuickHideCategory extends BaseCategory {

    private MaterialSwitchPreference panicButtonPref, autoHideAfterScreenOffPref;
    private SeekBarPreference autoHideDelayPref;
    private Preference panicButtonColorPref;

    public QuickHideCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.quick_hide);

        var servicePref = new MaterialSwitchPreference(activity);
        servicePref.setKey(PrefMgr.ENABLE_QUICK_HIDE_SERVICE);
        servicePref.setIcon(R.drawable.notifications_black_24dp);
        servicePref.setTitle(R.string.notification);
        servicePref.setSummary(R.string.quick_hide_notification_description);
        servicePref.setChecked(PrefMgr.getEnableQuickHideService());
        servicePref.setOnPreferenceClickListener(preference -> {
            if (servicePref.isChecked()) {
                PermissionUtil.requestNotificationPermission(activity, new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean all) {
                        Log.d("QuickHideNotification", "Granted: NOTIFICATION");
                        QuickHideService.startService(activity);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean never) {
                        Log.w("QuickHideNotification", "User denied: NOTIFICATION");
                        Toast.makeText(activity, R.string.notification_permission_denied, Toast.LENGTH_LONG).show();
                        servicePref.setChecked(false);
                    }
                });
            } else {
                QuickHideService.stopService(activity);
            }
            return true;
        });
        servicePref.setOnPreferenceChangeListener((preference, newValue) -> {
            panicButtonPref.setEnabled((boolean) newValue);
            panicButtonColorPref.setEnabled((boolean) newValue && panicButtonPref.isChecked());
            autoHideAfterScreenOffPref.setEnabled((boolean) newValue);
            autoHideDelayPref.setEnabled((boolean) newValue && autoHideAfterScreenOffPref.isChecked());
            if (!(boolean) newValue)
                panicButtonPref.setChecked(false); // Avoid enabling panic button without requesting the permission
            return true;
        });
        addPreference(servicePref);

        panicButtonPref = new MaterialSwitchPreference(activity);
        panicButtonPref.setKey(PrefMgr.ENABLE_PANIC_BUTTON);
        panicButtonPref.setIcon(R.drawable.crisis_alert_black_24dp);
        panicButtonPref.setTitle(R.string.panic_button);
        panicButtonPref.setSummary(R.string.panic_button_description);
        panicButtonPref.setEnabled(PrefMgr.getEnableQuickHideService());
        panicButtonPref.setChecked(PrefMgr.getEnablePanicButton());
        panicButtonPref.setOnPreferenceClickListener(preference -> {
            if (panicButtonPref.isChecked()) {
                PermissionUtil.requestSystemAlertPermission(activity, new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean all) {
                        Log.d("PanicButton", "Granted: SYSTEM_ALERT_WINDOW");
                        QuickHideService.startService(activity);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean never) {
                        Log.w("PanicButton", "User denied: SYSTEM_ALERT_WINDOW");
                        Toast.makeText(activity, R.string.alert_permission_denied, Toast.LENGTH_LONG).show();
                        panicButtonPref.setChecked(false);
                    }
                });
            } else {
                QuickHideService.startService(activity); // Restart service
            }
            return true;
        });
        panicButtonPref.setOnPreferenceChangeListener((preference, newValue) -> {
            panicButtonColorPref.setEnabled((boolean) newValue);
            return true;
        });
        addPreference(panicButtonPref);

        // Panic button color preference
        panicButtonColorPref = new Preference(activity);
        panicButtonColorPref.setKey(PrefMgr.PANIC_BUTTON_COLOR);
        panicButtonColorPref.setIcon(R.drawable.ic_null);
        panicButtonColorPref.setTitle(R.string.panic_button_color);
        panicButtonColorPref.setSummary(R.string.panic_button_color_description);
        panicButtonColorPref.setEnabled(PrefMgr.getEnableQuickHideService() && PrefMgr.getEnablePanicButton());
        panicButtonColorPref.setOnPreferenceClickListener(preference -> {
            new ColorPickerDialog.Builder(activity)
                    .setTitle(R.string.panic_button_color)
                    .setCurrentColor(PrefMgr.getPanicButtonColor())
                    .setOnColorSelectedListener(color -> {
                        PrefMgr.setPanicButtonColor(color);
                        QuickHideService.startService(activity); // Restart service to apply color
                    })
                    .show();
            return true;
        });
        addPreference(panicButtonColorPref);

        autoHideAfterScreenOffPref = new MaterialSwitchPreference(activity);
        autoHideAfterScreenOffPref.setKey(PrefMgr.ENABLE_AUTO_HIDE);
        autoHideAfterScreenOffPref.setIcon(R.drawable.lock_clock_fill0_wght400_grad0_opsz24);
        autoHideAfterScreenOffPref.setTitle(R.string.auto_hide);
        autoHideAfterScreenOffPref.setSummary(R.string.auto_hide_description);
        autoHideAfterScreenOffPref.setEnabled(PrefMgr.getEnableQuickHideService());
        autoHideAfterScreenOffPref.setChecked(PrefMgr.getEnableAutoHide());
        autoHideAfterScreenOffPref.setOnPreferenceChangeListener((preference, newValue) -> {
            autoHideDelayPref.setEnabled((boolean) newValue);
            return true;
        });
        addPreference(autoHideAfterScreenOffPref);

        autoHideDelayPref = new SeekBarPreference(activity);
        autoHideDelayPref.setKey(PrefMgr.AUTO_HIDE_DELAY);
        autoHideDelayPref.setIcon(R.drawable.timer_fill0_wght400_grad0_opsz24);
        autoHideDelayPref.setTitle(R.string.auto_hide_delay);
        autoHideDelayPref.setSummary(R.string.auto_hide_delay_description);
        autoHideDelayPref.setEnabled(autoHideAfterScreenOffPref.isEnabled() && autoHideAfterScreenOffPref.isChecked());
        autoHideDelayPref.setValue(PrefMgr.getAutoHideDelay());
        autoHideDelayPref.setShowSeekBarValue(true);
        autoHideDelayPref.setMin(0);
        autoHideDelayPref.setMax(30);
        addPreference(autoHideDelayPref);
    }
}
