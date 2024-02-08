package deltazero.amarok.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class UpdateCategory extends BaseCategory {
    public UpdateCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.update);

        String appVersionName = null;
        try {
            appVersionName = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException ignore) {
        }

        var checkUpdatePref = new Preference(activity);
        checkUpdatePref.setTitle(R.string.check_update);
        checkUpdatePref.setSummary(activity.getString(R.string.check_update_description, appVersionName));
        checkUpdatePref.setOnPreferenceClickListener(preference -> {
            if (AppCenterUtil.isAvailable())
                AppCenterUtil.checkUpdate();
            else
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/deltazefiro/Amarok-Hider/releases")));
            return true;
        });
        addPreference(checkUpdatePref);

        var autoUpdatePref = new MaterialSwitchPreference(activity);
        autoUpdatePref.setKey(PrefMgr.IS_ENABLE_AUTO_UPDATE);
        autoUpdatePref.setTitle(R.string.check_update_on_start);
        autoUpdatePref.setSummary(R.string.check_update_on_start_description);
        autoUpdatePref.setEnabled(AppCenterUtil.isAvailable());
        autoUpdatePref.setChecked(PrefMgr.getEnableAutoUpdate());
        autoUpdatePref.setOnPreferenceClickListener(preference -> {
            if (autoUpdatePref.isChecked()) AppCenterUtil.cleanUpdatePostpone();
            return true;
        });
        addPreference(autoUpdatePref);
    }
}
