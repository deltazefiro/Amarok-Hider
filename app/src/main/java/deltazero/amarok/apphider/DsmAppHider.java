package deltazero.amarok.apphider;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.catchingnow.delegatedscopeclient.DSMClient;

import java.util.Set;

import deltazero.amarok.receivers.AdminReceiver;
import deltazero.amarok.R;
import deltazero.amarok.ui.DsmActivationActivity;

public class DsmAppHider extends BaseAppHider {
    public static ActivationCallbackListener activationCallbackListener;
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
    public void tryToActivate(ActivationCallbackListener activationCallbackListener) {

        if (DSMClient.getOwnerSDKVersion(context) < DSMClient.SDK_VERSION) {
            // If no dsm provider registered, sdk_ver=-1, owner_name=null
            Log.w("DsmAppHider", String.format("Invalid DSM provider: sdk_ver=%s, owner_name=%s", DSMClient.getOwnerSDKVersion(context), DSMClient.getOwnerPackageName(context)));
            activationCallbackListener.onActivateCallback(this.getClass(), false, R.string.invalid_dsm_provider);
            return;
        }

        if (!DSMClient.getDelegatedScopes(context).contains("delegation-package-access")) {
            Log.i("DsmAppHider", "Permission required.");
            DsmAppHider.activationCallbackListener = activationCallbackListener;
            context.startActivity(new Intent(context, DsmActivationActivity.class));
            return;
        }

        activationCallbackListener.onActivateCallback(this.getClass(), true, 0);
    }

    @Override
    public String getName() {
        return "DSM";
    }
}
