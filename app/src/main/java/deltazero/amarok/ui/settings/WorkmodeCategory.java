package deltazero.amarok.ui.settings;


import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class WorkmodeCategory extends BaseCategory {

    Preference switchAppHiderPref, switchFileHiderPref;

    public WorkmodeCategory(@NonNull FragmentActivity activity, PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.workmode);

        switchAppHiderPref = new Preference(activity);
        switchAppHiderPref.setKey(PrefMgr.APP_HIDER_MODE);
        switchAppHiderPref.setTitle(R.string.switch_app_hider);
        switchAppHiderPref.setIntent(new Intent(activity, SwitchAppHiderActivity.class));
        switchAppHiderPref.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(activity.getString(R.string.current_mode, PrefMgr.getAppHider(activity).getName()));
            return true;
        });
        switchAppHiderPref.setIcon(R.drawable.apps_black_24dp);
        addPreference(switchAppHiderPref);

        switchFileHiderPref = new Preference(activity);
        switchFileHiderPref.setKey(PrefMgr.FILE_HIDER_MODE);
        switchFileHiderPref.setTitle(R.string.switch_file_hider);
        switchFileHiderPref.setIntent(new Intent(activity, SwitchFileHiderActivity.class));
        switchFileHiderPref.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(activity.getString(R.string.current_mode, PrefMgr.getFileHider(activity).getName()));
            return true;
        });
        switchFileHiderPref.setIcon(R.drawable.folder_black_24dp);
        addPreference(switchFileHiderPref);
    }

    @Override
    protected void onUpdate() {
        switchAppHiderPref.callChangeListener(PrefMgr.getAppHider(activity).getName());
        switchFileHiderPref.callChangeListener(PrefMgr.getFileHider(activity).getName());
    }
}
