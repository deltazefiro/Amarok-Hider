package deltazero.amarok.utils;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.Crashes;

import deltazero.amarok.PrefMgr;
import kotlin.NotImplementedError;

public class AppCenterUtil {

    private static final String appSecret = "6bcd9547-9df2-4023-bfcd-6e1a0f0f9e12";

    public static void cleanUpdatePostpone() {
        throw new NotImplementedError("Not available in Play Store version.");
    }

    public static void checkUpdate() {
        throw new NotImplementedError("Not available in Play Store version.");
    }

    public static void setAnalyticsEnabled(boolean enabled) {
        Crashes.setEnabled(enabled);
    }

    public static boolean isAnalyticsEnabled() {
        return Crashes.isEnabled().get();
    }

    public static void startAppCenter(Application application) {
        assert PrefMgr.initialized;
        AppCenter.start(application, appSecret, Crashes.class);
    }

    public static boolean isAvailable() {
        return false;
    }
}