package deltazero.amarok.AppHider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.system.Os;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Set;

import deltazero.amarok.BuildConfig;
import deltazero.amarok.R;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class ShizukuHider extends AppHiderBase {
    public static final int shizukuReqCode = 600;

    public ShizukuHider(Context context) {
        super(context);
    }

    @SuppressLint("PrivateApi")
    private void setAppDisabled(boolean disabled, Set<String> pkgNames) {
        /*
        Call android.content.pm.IPackageManager.setApplicationEnabledSetting with reflection.
        Through Shizuku wrapper.
        Reference:
            - https://www.xda-developers.com/implementing-shizuku/
            - https://github.dev/aistra0528/Hail
         */

        Method mSetApplicationEnabledSetting;
        Object iPmInstance;

        try {
            Class<?> iPmClass = Class.forName("android.content.pm.IPackageManager");

            Class<?> iPmStub = Class.forName("android.content.pm.IPackageManager$Stub");
            Method asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder.class);
            iPmInstance = asInterfaceMethod.invoke(null, new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));

            mSetApplicationEnabledSetting = iPmClass.getMethod("setApplicationEnabledSetting", String.class, int.class, int.class, int.class, String.class);
        } catch (Exception e) {
            Log.e("ShizukuHider", e.toString());
            Toast.makeText(context, R.string.shizuku_hidden_api_error, Toast.LENGTH_LONG).show();
            return;
        }

        for (String p : pkgNames) {
            try {
                mSetApplicationEnabledSetting.invoke(iPmInstance,
                        p,
                        (disabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT),
                        0,
                        Os.getuid() / 100000,
                        BuildConfig.APPLICATION_ID);
                Log.i("ShizukuHider", "Hid app: " + p);
            } catch (Exception e) {
                Log.w("ShizukuHider", e.toString());
            }
        }

    }

    @SuppressLint("PrivateApi")
    private void setAppHidden(boolean hidden, Set<String> pkgNames) {
        /*
        Call android.content.pm.IPackageManager.setApplicationHiddenSettingAsUser with reflection.
        Via Shizuku wrapper.
         */

        Method mSetApplicationHiddenSettingAsUser;
        Object iPmInstance;

        try {
            Class<?> iPmClass = Class.forName("android.content.pm.IPackageManager");

            Class<?> iPmStub = Class.forName("android.content.pm.IPackageManager$Stub");
            Method asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder.class);
            iPmInstance = asInterfaceMethod.invoke(null, new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));

            mSetApplicationHiddenSettingAsUser = iPmClass.getMethod("setApplicationHiddenSettingAsUser", String.class, boolean.class, int.class);
        } catch (Exception e) {
            Log.e("ShizukuHider", e.toString());
            Toast.makeText(context, R.string.shizuku_hidden_api_error, Toast.LENGTH_LONG).show();
            return;
        }

        for (String p : pkgNames) {
            try {
                mSetApplicationHiddenSettingAsUser.invoke(iPmInstance,
                        p,
                        hidden,
                        Os.getuid() / 100000);
                Log.i("ShizukuHider", "Hid app: " + p);
            } catch (Exception e) {
                Log.w("ShizukuHider", e.toString());
            }
        }

    }

    @Override
    public void hide(Set<String> pkgNames) {
        if (!Shizuku.pingBinder()) {
            Log.w("ShizukuHider", "Binder not available.");
            return;
        }
        setAppDisabled(true, pkgNames);
        setAppHidden(true, pkgNames);
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        if (!Shizuku.pingBinder()) {
            Log.w("ShizukuHider", "Binder not available.");
            return;
        }
        setAppDisabled(false, pkgNames);
        setAppHidden(false, pkgNames);
    }

    @Override
    public CheckAvailabilityResult checkAvailability() {
        try {
            if (Shizuku.isPreV11()) {
                Log.w("ShizukuHider", "checkAvailability: Shizuku is running pre v11.");
                return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE,
                        R.string.shizuku_pre_v11);
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                if (!Shizuku.pingBinder()) {
                    Log.w("ShizukuHider", "checkAvailability: Binder not available.");
                    return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE,
                            R.string.shizuku_service_not_running);
                }
                Log.i("ShizukuHider", "checkAvailability: Shizuku available.");
                return new CheckAvailabilityResult(CheckAvailabilityResult.Result.AVAILABLE);
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                // Users choose "Deny and don't ask again"
                Log.w("ShizukuHider", "checkAvailability: permission denied.");
                return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE,
                        R.string.shizuku_permission_denied);
            } else {
                // Request the permission
                return new CheckAvailabilityResult(CheckAvailabilityResult.Result.REQ_PERM);
            }
        } catch (IllegalStateException e) {
            Log.w("ShizukuHider", "checkAvailability: Shizuku not available: ", e);
            return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE,
                    R.string.shizuku_not_working);
        }
    }

    @Override
    public void active(OnActivateCallbackListener onActivateCallbackListener) {
        CheckAvailabilityResult r = checkAvailability();
        switch (r.result) {
            case UNAVAILABLE:
                onActivateCallbackListener.onActivateCallback(this.getClass(), false, r.msgResID);
                break;
            case AVAILABLE:
                onActivateCallbackListener.onActivateCallback(this.getClass(), true, 0);
                break;
            case REQ_PERM:
                Shizuku.requestPermission(shizukuReqCode);
        }
    }

    @Override
    public String getName() {
        return "Shizuku";
    }

}
