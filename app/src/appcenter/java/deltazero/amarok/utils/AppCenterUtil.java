package deltazero.amarok.utils;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.DistributeListener;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.UpdateAction;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.settings.SettingsActivity;

public class AppCenterUtil {

    private static final String appSecret = "6bcd9547-9df2-4023-bfcd-6e1a0f0f9e12";

    public static class AmarokDistributeListener implements DistributeListener {

        @Override
        public boolean onReleaseAvailable(Activity activity, ReleaseDetails releaseDetails) {

            String versionName = releaseDetails.getShortVersion();
            int versionCode = releaseDetails.getVersion();

            Log.i("CheckUpdate", "Found new update: Amarok " + versionName);

            try {

                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.update_ava)
                        .setMessage(activity.getString(R.string.update_description, versionName, versionCode))
                        .setPositiveButton(R.string.update, (dialog, which) -> Distribute.notifyUpdateAction(UpdateAction.UPDATE))
                        .setNeutralButton(R.string.never, (dialog, which) -> {
                            PrefMgr.setEnableAutoUpdate(false);
                            Distribute.notifyUpdateAction(UpdateAction.POSTPONE);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> Distribute.notifyUpdateAction(UpdateAction.POSTPONE))
                        .setCancelable(false)
                        .show();

            } catch (Exception e) {
                Log.e("AppCenterUtil", "Failed to update: ", e);
                Toast.makeText(activity, R.string.in_app_update_failed, Toast.LENGTH_LONG).show();
                Distribute.notifyUpdateAction(UpdateAction.POSTPONE);
            }

            return true;
        }

        @Override
        public void onNoReleaseAvailable(Activity activity) {
            if (activity instanceof SettingsActivity)
                Toast.makeText(activity, activity.getString(R.string.no_update_ava), Toast.LENGTH_SHORT).show();
            Log.i("CheckUpdate", "No available update yet.");
        }
    }

    public static void cleanUpdatePostpone() {
        // To clean postpone
        Distribute.setEnabled(false);
        Distribute.setEnabled(true);
    }

    public static void checkUpdate() {
        cleanUpdatePostpone();
        Distribute.checkForUpdate();
    }

    public static void setAnalyticsEnabled(boolean enabled) {
        Crashes.setEnabled(enabled);
        Analytics.setEnabled(enabled);
    }

    public static boolean isAnalyticsEnabled() {
        return Crashes.isEnabled().get();
    }

    public static void startAppCenter(Application application) {
        assert PrefMgr.initialized;

        Distribute.setEnabledForDebuggableBuild(false);
        if (!PrefMgr.getEnableAutoUpdate())
            Distribute.disableAutomaticCheckForUpdate();

        Distribute.setListener(new AmarokDistributeListener());
        AppCenter.start(application, appSecret,
                Analytics.class, Crashes.class, Distribute.class);
    }

    public static boolean isAvailable() {
        return true;
    }
}