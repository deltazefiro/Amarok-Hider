package deltazero.amarok.ui.settings;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;

import com.hjq.permissions.OnPermissionCallback;

import java.util.List;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.QuickHideService;
import deltazero.amarok.R;
import deltazero.amarok.utils.PermissionUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class QuickHideCategory extends BaseCategory {

    private MaterialSwitchPreference panicButtonPref;

    public QuickHideCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.quick_hide);

        var servicePref = new MaterialSwitchPreference(activity);
        servicePref.setKey(PrefMgr.ENABLE_QUICK_HIDE_SERVICE);
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
            if (!(boolean) newValue) panicButtonPref.setChecked(false);
            return true;
        });
        addPreference(servicePref);

        panicButtonPref = new MaterialSwitchPreference(activity);
        panicButtonPref.setKey(PrefMgr.ENABLE_PANIC_BUTTON);
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
        addPreference(panicButtonPref);
    }
}
