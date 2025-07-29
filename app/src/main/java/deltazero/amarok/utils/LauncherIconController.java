package deltazero.amarok.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import deltazero.amarok.BuildConfig;

public class LauncherIconController {
    private static final String LAUNCHER_DEFAULT = "deltazero.amarok.launcher.default";
    private static final String LAUNCHER_CALENDAR = "deltazero.amarok.launcher.calendar";

    public enum IconState {
        VISIBLE,    // Normal Amarok icon visible
        DISGUISED,  // Calendar icon visible (disguised)
        HIDDEN      // No icon visible
    }

    public static void setIconState(Activity activity, IconState state) {
        PackageManager pm = activity.getPackageManager();

        switch (state) {
            case VISIBLE -> {
                // Show normal Amarok icon
                setComponentState(pm, LAUNCHER_DEFAULT, true);
                setComponentState(pm, LAUNCHER_CALENDAR, false);
            }
            case DISGUISED -> {
                // Show calendar icon (disguised)
                setComponentState(pm, LAUNCHER_CALENDAR, true);
                setComponentState(pm, LAUNCHER_DEFAULT, false);
            }
            case HIDDEN -> {
                // Hide all icons
                setComponentState(pm, LAUNCHER_DEFAULT, false);
                setComponentState(pm, LAUNCHER_CALENDAR, false);
            }
        }
    }

    private static void setComponentState(PackageManager pm, String componentName, boolean enabled) {
        pm.setComponentEnabledSetting(
                new ComponentName(BuildConfig.APPLICATION_ID, componentName),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
