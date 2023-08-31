package deltazero.amarok.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;

public class LauncherIconController {
    public static void switchDisguise(Activity activity, boolean enableDisguise) {
        activity.getPackageManager().setComponentEnabledSetting(
                new ComponentName(activity.getPackageName(), "deltazero.amarok.launcher.calendar"),
                enableDisguise ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        activity.getPackageManager().setComponentEnabledSetting(
                new ComponentName(activity.getPackageName(), "deltazero.amarok.launcher.default"),
                enableDisguise ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static boolean checkIsDisguised(Activity activity) {
        var status = activity.getPackageManager().getComponentEnabledSetting(new ComponentName(activity.getPackageName(), "deltazero.amarok.launcher.calendar"));
        return status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }
}
