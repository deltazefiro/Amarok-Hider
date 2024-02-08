package deltazero.amarok.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

public abstract class BaseCategory {

    protected PreferenceCategory category;
    protected PreferenceScreen screen;
    protected FragmentActivity activity;

    public BaseCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        this.screen = screen;
        this.activity = activity;
        category = new PreferenceCategory(activity);
        screen.addPreference(category);
    }

    public void notifyUpdate() {
        onUpdate();
    }

    protected void onUpdate() {
    }

    protected void setTitle(int titleResId) {
        category.setTitle(titleResId);
    }

    protected void addPreference(Preference preference) {
        category.addPreference(preference);
    }
}
