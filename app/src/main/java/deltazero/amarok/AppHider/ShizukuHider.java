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
            } catch (Exception e) {
                Log.e("ShizukuHider", e.toString());
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
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        if (!Shizuku.pingBinder()) {
            Log.w("ShizukuHider", "Binder not available.");
            return;
        }
        setAppDisabled(false, pkgNames);
    }

    @Override
    public boolean checkAvailability() {
        try {
            if (Shizuku.isPreV11()) {
                Log.w("ShizukuHider", "checkAvailability: Shizuku is running pre v11.");
                Toast.makeText(context, R.string.shizuku_pre_v11, Toast.LENGTH_LONG).show();
                return false;
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Log.d("ShizukuHider", "checkAvailability: Shizuku available.");
                return true;
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                // Users choose "Deny and don't ask again"
                Log.w("ShizukuHider", "checkAvailability: permission denied.");
                Toast.makeText(context, R.string.shizuku_permission_denied, Toast.LENGTH_LONG).show();
                return false;
            } else {
                // Request the permission
                Shizuku.requestPermission(shizukuReqCode);
                return false;
            }
        } catch (IllegalStateException e) {
            Log.w("ShizukuHider", "checkAvailability: Shizuku not available: ", e);
            Toast.makeText(context, R.string.shizuku_not_working, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    public String getName() {
        return "Shizuku";
    }

}
