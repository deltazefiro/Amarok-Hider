package deltazero.amarok.ui.settings;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.UpdateUtil;
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

        // Check for update preference
        var checkUpdatePref = new Preference(activity);
        checkUpdatePref.setTitle(R.string.check_update);
        checkUpdatePref.setIcon(R.drawable.update_black_24dp);
        checkUpdatePref.setSummary(activity.getString(R.string.check_update_description, appVersionName));
        checkUpdatePref.setOnPreferenceClickListener(preference -> {
            UpdateUtil.checkAndNotify(activity, false);
            return true;
        });
        addPreference(checkUpdatePref);

        // Update channel preference
        var updateChannelPref = new DropDownPreference(activity);
        updateChannelPref.setKey(PrefMgr.UPDATE_CHANNEL);
        updateChannelPref.setIcon(R.drawable.alt_route_24dp_1f1f1f_fill0_wght400_grad0_opsz24);
        updateChannelPref.setTitle(R.string.update_channel);
        updateChannelPref.setSummary("%s");
        updateChannelPref.setEntries(new CharSequence[]{
                activity.getString(R.string.update_channel_release),
                activity.getString(R.string.update_channel_beta)
        });
        updateChannelPref.setEntryValues(new CharSequence[]{
                UpdateUtil.UpdateChannel.RELEASE.name(),
                UpdateUtil.UpdateChannel.BETA.name()
        });
        updateChannelPref.setDefaultValue(UpdateUtil.UpdateChannel.RELEASE.name());
        addPreference(updateChannelPref);

        // Auto update switch
        var autoUpdatePref = new MaterialSwitchPreference(activity);
        autoUpdatePref.setKey(PrefMgr.IS_ENABLE_AUTO_UPDATE);
        autoUpdatePref.setIcon(R.drawable.autorenew_black_24dp);
        autoUpdatePref.setTitle(R.string.check_update_on_start);
        autoUpdatePref.setSummary(R.string.check_update_on_start_description);
        autoUpdatePref.setDefaultValue(true);
        addPreference(autoUpdatePref);
    }
}