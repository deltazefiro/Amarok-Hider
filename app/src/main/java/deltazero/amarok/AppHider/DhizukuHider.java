package deltazero.amarok.AppHider;

import static android.app.admin.DevicePolicyManager.DELEGATION_PACKAGE_ACCESS;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import java.util.Arrays;
import java.util.Set;

import deltazero.amarok.R;

public class DhizukuHider extends AppHiderBase {

    private final DevicePolicyManager devicePolicyManager;

    public DhizukuHider(Context context) {
        super(context);
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    public void hide(Set<String> pkgNames) {
        setDelegatedScopes();
        for (var pkgName : pkgNames) {
            devicePolicyManager.setApplicationHidden(null, pkgName, true);
        }
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        setDelegatedScopes();
        for (var pkgName : pkgNames) {
            devicePolicyManager.setApplicationHidden(null, pkgName, false);
        }
    }

    @Override
    public CheckAvailabilityResult checkAvailability() {
        if (!Dhizuku.init()) {
            Log.w("DhizukuHider", "Dhizuku not init.");
            return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE, R.string.dhizuku_not_init);
        }

        if (Dhizuku.getVersionCode() < 5) {
            Log.w("DhizukuHider", "Unsupported Dhizuku version: pre v5.x");
            return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE, R.string.dhizuku_pre_v5);
        }

        return Dhizuku.isPermissionGranted()
                ? new CheckAvailabilityResult(CheckAvailabilityResult.Result.AVAILABLE)
                : new CheckAvailabilityResult(CheckAvailabilityResult.Result.REQ_PERM);
    }

    @Override
    public void active(OnActivateCallbackListener onActivateCallbackListener) {
        CheckAvailabilityResult r = checkAvailability();
        switch (r.result) {
            case UNAVAILABLE ->
                    onActivateCallbackListener.onActivateCallback(this.getClass(), false, r.msgResID);
            case AVAILABLE ->
                    onActivateCallbackListener.onActivateCallback(this.getClass(), true, 0);
            case REQ_PERM -> Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d("DhizukuHider", "Permission granted.");
                        onActivateCallbackListener.onActivateCallback(DhizukuHider.class, true, 0);
                    } else {
                        Log.d("DhizukuHider", "Permission denied.");
                        onActivateCallbackListener.onActivateCallback(DhizukuHider.class, false, R.string.dhizuku_permission_denied);
                    }
                }
            });
        }
    }

    @Override
    public String getName() {
        return "Dhizuku";
    }

    private void setDelegatedScopes() {
        if (Arrays.asList(Dhizuku.getDelegatedScopes())
                .contains(DELEGATION_PACKAGE_ACCESS))
            return;
        Dhizuku.setDelegatedScopes(new String[]{DELEGATION_PACKAGE_ACCESS});
    }
}
