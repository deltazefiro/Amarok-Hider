package deltazero.amarok.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.Locale;

import deltazero.amarok.R;

public class SwitchLocaleUtil {
    public static void switchLocale(Context context) {
        View radioGroupView = LayoutInflater.from(context).inflate(R.layout.dialog_scrollable_button_group, null);
        RadioGroup rgRadioGroup = radioGroupView.findViewById(R.id.dialog_rg_radio_group);

        // Setup buttons
        for (int i = 0; i < LangList.LOCALES.length; ++i) {
            var radioButton = new RadioButton(context);

            String displayName;
            if (LangList.LOCALES[i].equals("SYSTEM")) {
                displayName = context.getString(R.string.follow_system);
            } else {
                var locale = Locale.forLanguageTag(LangList.LOCALES[i]);
                displayName = locale.getDisplayName(locale);
                // displayName = String.format("%s: %s", locale.getDisplayName(locale), locale.getDisplayName());
            }

            radioButton.setText(displayName);
            radioButton.setId(i);
            rgRadioGroup.addView(radioButton);
        }

        // Apply current active locale
        var activeLocale = AppCompatDelegate.getApplicationLocales();
        if (activeLocale.equals(LocaleListCompat.getEmptyLocaleList()))
            rgRadioGroup.check(0);
        else
            rgRadioGroup.check(Arrays.asList(LangList.LOCALES).indexOf(activeLocale.toLanguageTags()));

        // Listener and switch locale
        rgRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d("Locales", String.format("Active locale: %s", LangList.LOCALES[checkedId]));
            if (checkedId == 0) { /* Follow system */
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            } else {
                LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(LangList.LOCALES[checkedId]);
                AppCompatDelegate.setApplicationLocales(appLocale);
            }
        });

        // Show dialog
        new MaterialAlertDialogBuilder(context)
                .setView(radioGroupView)
                .show();
    }

    public static Locale getActiveLocale(Context context) {
        var activeLocale = AppCompatDelegate.getApplicationLocales();
        if (activeLocale.equals(LocaleListCompat.getEmptyLocaleList()))
            return context.getResources().getConfiguration().getLocales().get(0);
        else
            return Locale.forLanguageTag(activeLocale.toLanguageTags());
    }
}
