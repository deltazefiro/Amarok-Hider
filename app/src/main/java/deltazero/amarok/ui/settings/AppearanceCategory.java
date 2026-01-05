package deltazero.amarok.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.SwitchLocaleUtil;
import rikka.material.preference.MaterialSwitchPreference;

public class AppearanceCategory extends BaseCategory {
    public AppearanceCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.appearance);

        var dynamicColorPref = new MaterialSwitchPreference(activity);
        dynamicColorPref.setKey(PrefMgr.ENABLE_DYNAMIC_COLOR);
        dynamicColorPref.setIcon(R.drawable.palette_black_24dp);
        dynamicColorPref.setTitle(R.string.enable_dynamic_color);
        dynamicColorPref.setSummary(R.string.dynamic_color_description);
        dynamicColorPref.setChecked(PrefMgr.getEnableDynamicColor());
        dynamicColorPref.setOnPreferenceClickListener(preference -> {
            Toast.makeText(activity, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
            return true;
        });
        addPreference(dynamicColorPref);

        // Theme mode preference
        var themeModePref = new Preference(activity);
        themeModePref.setKey(PrefMgr.THEME_MODE);
        themeModePref.setIcon(R.drawable.ic_null);
        themeModePref.setTitle(R.string.theme_mode);
        themeModePref.setSummary(getThemeModeLabel(activity, PrefMgr.getThemeMode()));
        themeModePref.setOnPreferenceClickListener(preference -> {
            showThemeSelectionDialog(activity, themeModePref);
            return true;
        });
        addPreference(themeModePref);

        var languagePref = new Preference(activity);
        languagePref.setTitle(R.string.language);
        languagePref.setIcon(R.drawable.ic_language);
        languagePref.setSummary(R.string.language_description);
        languagePref.setOnPreferenceClickListener(preference -> {
            SwitchLocaleUtil.switchLocale(activity);
            return true;
        });
        addPreference(languagePref);

        var participateTranslationPref = new Preference(activity);
        participateTranslationPref.setTitle(R.string.participate_translation);
        participateTranslationPref.setIcon(R.drawable.translate_black_24dp);
        participateTranslationPref.setSummary(R.string.participate_translation_description);
        participateTranslationPref.setIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://hosted.weblate.org/engage/amarok-hider/")));
        addPreference(participateTranslationPref);

        var invertTileColorPref = new MaterialSwitchPreference(activity);
        invertTileColorPref.setKey(PrefMgr.INVERT_TILE_COLOR);
        invertTileColorPref.setIcon(R.drawable.invert_colors_24dp_5f6368_fill0_wght400_grad0_opsz24);
        invertTileColorPref.setTitle(R.string.invert_tile_color);
        invertTileColorPref.setSummary(R.string.invert_tile_color_description);
        invertTileColorPref.setChecked(PrefMgr.getInvertTileColor());
        invertTileColorPref.setOnPreferenceClickListener(preference -> {
            Toast.makeText(activity, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
            return true;
        });
        addPreference(invertTileColorPref);
    }

    private void showThemeSelectionDialog(FragmentActivity activity, Preference themeModePref) {
        String[] themeOptions = {
                activity.getString(R.string.theme_mode_system),
                activity.getString(R.string.theme_mode_light),
                activity.getString(R.string.theme_mode_dark)
        };

        int currentThemeMode = PrefMgr.getThemeMode();
        // Convert AppCompatDelegate constants to array index
        int currentSelection;
        if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            currentSelection = 1; // Light
        } else if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            currentSelection = 2; // Dark
        } else {
            currentSelection = 0; // System default
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.theme_mode)
                .setSingleChoiceItems(themeOptions, currentSelection, (dialog, which) -> {
                    int newThemeMode;
                    switch (which) {
                        case 1 -> newThemeMode = AppCompatDelegate.MODE_NIGHT_NO; // Light
                        case 2 -> newThemeMode = AppCompatDelegate.MODE_NIGHT_YES; // Dark
                        default -> newThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // System
                    }

                    PrefMgr.setThemeMode(newThemeMode);
                    AppCompatDelegate.setDefaultNightMode(newThemeMode);
                    themeModePref.setSummary(getThemeModeLabel(activity, newThemeMode));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getThemeModeLabel(FragmentActivity activity, int themeMode) {
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            return activity.getString(R.string.theme_mode_light);
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            return activity.getString(R.string.theme_mode_dark);
        } else {
            return activity.getString(R.string.theme_mode_system);
        }
    }
}
