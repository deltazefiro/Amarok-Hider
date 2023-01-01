package deltazero.amarok.AppHider;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.catchingnow.delegatedscopeclient.DSMClient;

import java.util.Set;

import deltazero.amarok.AdminReceiver;
import deltazero.amarok.R;
import deltazero.amarok.ui.SwitchAppHiderActivity;

public class DsmAppHider extends AppHiderBase {

    public static final int dsmReqCode = 700;
    private final DevicePolicyManager dpm;
    private final ComponentName admin;

    public DsmAppHider(Context context) {
        super(context);
        dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        admin = new ComponentName(context, AdminReceiver.class);
    }

    @Override
    public void hide(Set<String> pkgNames) {
        if (!dpm.isAdminActive(admin)) {
            Log.w("DsmAppHider", "Admin not active. Failed to unhide apps.");
            Toast.makeText(context, R.string.hide_app_failed_admin_inactive, Toast.LENGTH_LONG).show();
        }
        for (String p : pkgNames) {
            dpm.setApplicationHidden(null, p, true);
        }
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        if (!dpm.isAdminActive(admin)) {
            Log.w("DsmAppHider", "Admin not active. Failed to unhide apps.");
            Toast.makeText(context, R.string.hide_app_failed_admin_inactive, Toast.LENGTH_LONG).show();
        }
        for (String p : pkgNames) {
            dpm.setApplicationHidden(null, p, false);
        }
    }

    @Override
    public CheckAvailabilityResult checkAvailability() {
        if (DSMClient.getOwnerSDKVersion(context) < DSMClient.SDK_VERSION) {
            // If no dsm provider registered, sdk_ver=-1, owner_name=null
            Log.w("DsmAppHider", String.format("Invalid DSM provider: sdk_ver=%s, owner_name=%s", DSMClient.getOwnerSDKVersion(context), DSMClient.getOwnerPackageName(context)));
            return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE, R.string.invalid_dsm_provider);
        }

        if (!DSMClient.getDelegatedScopes(context).contains("delegation-package-access")) {
            Log.i("DsmAppHider", "Permission required.");
            return new CheckAvailabilityResult(CheckAvailabilityResult.Result.REQ_PERM);
        }

        return new CheckAvailabilityResult(CheckAvailabilityResult.Result.AVAILABLE);

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
                Log.i("DsmAppHider", "No delegation-package-access, start to request...");

                DSMClient.requestScopes((SwitchAppHiderActivity) context, dsmReqCode, DevicePolicyManager.DELEGATION_PACKAGE_ACCESS);
        }

    }

    @Override
    public String getName() {
        return "DSM";
    }
}
