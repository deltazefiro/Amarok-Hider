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

public class ShizukuAppHider extends BaseAppHider {
    public static final int shizukuReqCode = 600;

    public ShizukuAppHider(Context context) {
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
    public void tryToActivate(ActivationCallbackListener activationCallbackListener) {
        try {

            // Check if Shizuku is running pre v11
            if (Shizuku.isPreV11()) {
                Log.w("ShizukuHider", "checkAvailability: Shizuku is running pre v11.");
                activationCallbackListener.onActivateCallback(this.getClass(), false, R.string.shizuku_pre_v11);
                return;
            }

            // Check if Shizuku is available
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.pingBinder()) {
                    Log.i("ShizukuHider", "checkAvailability: Shizuku available.");
                    activationCallbackListener.onActivateCallback(this.getClass(), true, 0);
                } else {
                    Log.w("ShizukuHider", "checkAvailability: Binder not available.");
                    activationCallbackListener.onActivateCallback(this.getClass(), false, R.string.shizuku_service_not_running);
                }
                return;
            }

            // Check if user has denied permission forever
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                // Users choose "Deny and don't ask again"
                Log.w("ShizukuHider", "checkAvailability: permission denied.");
                activationCallbackListener.onActivateCallback(this.getClass(), false, R.string.shizuku_permission_denied);
                return;
            }

            // Request permission
            var listener = new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.i("ShizukuHider", "Permission granted. Set hider to ShizukuHider.");
                        activationCallbackListener.onActivateCallback(ShizukuAppHider.class, true, 0);
                    } else {
                        Log.i("ShizukuHider", "Permission denied.");
                        activationCallbackListener.onActivateCallback(ShizukuAppHider.class, false, R.string.shizuku_permission_denied);
                    }
                    Shizuku.removeRequestPermissionResultListener(this);
                }
            };
            Shizuku.addRequestPermissionResultListener(listener);
            Shizuku.requestPermission(shizukuReqCode);

        } catch (IllegalStateException e) {
            Log.w("ShizukuHider", "checkAvailability: Shizuku not available: ", e);
            activationCallbackListener.onActivateCallback(this.getClass(), false, R.string.shizuku_not_working);
        }
    }

    @Override
    public String getName() {
        return "Shizuku";
    }

}
