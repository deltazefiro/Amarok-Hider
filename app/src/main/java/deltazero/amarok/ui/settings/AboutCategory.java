package deltazero.amarok.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.Hider;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class AboutCategory extends BaseCategory {
    public AboutCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.about);

        var analyticsPref = new MaterialSwitchPreference(activity);
        analyticsPref.setTitle(R.string.check_update_on_start);
        analyticsPref.setSummary(R.string.check_update_on_start_description);
        analyticsPref.setEnabled(AppCenterUtil.isAvailable());
        analyticsPref.setChecked(AppCenterUtil.isAnalyticsEnabled());
        analyticsPref.setOnPreferenceClickListener(preference -> {
            AppCenterUtil.setAnalyticsEnabled(analyticsPref.isChecked());
            Toast.makeText(activity, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
            return true;
        });
        addPreference(analyticsPref);

        var forceUnhidePref = new Preference(activity);
        forceUnhidePref.setTitle(R.string.force_unhide);
        forceUnhidePref.setSummary(R.string.force_unhide_description);
        forceUnhidePref.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.force_unhide)
                    .setMessage(R.string.force_unhide_confirm_msg)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        Hider.forceUnhide(activity);
                        Toast.makeText(activity, R.string.performing_force_unhide, Toast.LENGTH_LONG).show();
                        activity.finish();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });
        addPreference(forceUnhidePref);

        var githubRepoPref = new Preference(activity);
        githubRepoPref.setTitle(R.string.view_github_repo);
        githubRepoPref.setSummary(R.string.view_github_repo_description);
        githubRepoPref.setIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/deltazefiro/Amarok-Hider")));
        addPreference(githubRepoPref);

        var telegramGroupPref = new Preference(activity);
        telegramGroupPref.setTitle(R.string.join_developer_channel);
        telegramGroupPref.setSummary(R.string.developer_channel_telegram);
        telegramGroupPref.setIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://t.me/amarok_dev")));
        addPreference(telegramGroupPref);

        var usagePref = new Preference(activity);
        usagePref.setTitle(R.string.usage);
        usagePref.setSummary(R.string.usage_description);
        usagePref.setIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(activity.getString(R.string.doc_url))));
        addPreference(usagePref);
    }
}
