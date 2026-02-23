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

    /** Night modes ordered by dialog index: System, Light, Dark */
    private static final int[] NIGHT_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    private static final int[] DARK_THEME_LABELS = {
            R.string.dark_theme_follow_system,
            R.string.dark_theme_light,
            R.string.dark_theme_dark
    };

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

        // Dark theme preference
        var darkThemePref = new Preference(activity);
        darkThemePref.setKey(PrefMgr.DARK_THEME);
        darkThemePref.setIcon(R.drawable.contrast_24dp_1f1f1f_fill0_wght400_grad0_opsz24);
        darkThemePref.setTitle(R.string.dark_theme);
        darkThemePref.setSummary(getDarkThemeLabel(PrefMgr.getDarkTheme()));
        darkThemePref.setOnPreferenceClickListener(preference -> {
            showDarkThemeDialog(activity, darkThemePref);
            return true;
        });
        addPreference(darkThemePref);

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

    private void showDarkThemeDialog(FragmentActivity activity, Preference darkThemePref) {
        String[] options = new String[NIGHT_MODES.length];
        for (int i = 0; i < DARK_THEME_LABELS.length; i++)
            options[i] = activity.getString(DARK_THEME_LABELS[i]);

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dark_theme)
                .setSingleChoiceItems(options, nightModeToIndex(PrefMgr.getDarkTheme()), (dialog, which) -> {
                    int mode = NIGHT_MODES[which];
                    PrefMgr.setDarkTheme(mode);
                    AppCompatDelegate.setDefaultNightMode(mode);
                    darkThemePref.setSummary(getDarkThemeLabel(mode));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static int nightModeToIndex(int nightMode) {
        for (int i = 0; i < NIGHT_MODES.length; i++) {
            if (NIGHT_MODES[i] == nightMode) return i;
        }
        return 0; // Default to system
    }

    private int getDarkThemeLabel(int nightMode) {
        return DARK_THEME_LABELS[nightModeToIndex(nightMode)];
    }
}
