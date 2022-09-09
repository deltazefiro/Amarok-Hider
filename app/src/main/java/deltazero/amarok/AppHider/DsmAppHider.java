package deltazero.amarok.AppHider;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.catchingnow.delegatedscopeclient.DSMClient;

import java.util.Set;

import deltazero.amarok.AdminReceiver;
import deltazero.amarok.R;

public class DsmAppHider extends AppHiderBase {
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
        for (String p: pkgNames) {
            dpm.setApplicationHidden(null, p, true);
        }
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        if (!dpm.isAdminActive(admin)) {
            Log.w("DsmAppHider", "Admin not active. Failed to unhide apps.");
            Toast.makeText(context, R.string.hide_app_failed_admin_inactive, Toast.LENGTH_LONG).show();
        }
        for (String p: pkgNames) {
            dpm.setApplicationHidden(null, p, false);
        }
    }

    @Override
    public boolean checkAvailability() {
        if (DSMClient.getOwnerSDKVersion(context) < DSMClient.SDK_VERSION) {
            // If no dsm provider registered, sdk_ver=-1, owner_name=null
            Log.w("DsmAppHider", String.format("Invalid DSM provider: sdk_ver=%s, owner_name=%s",
                    DSMClient.getOwnerSDKVersion(context), DSMClient.getOwnerPackageName(context)));
            Toast.makeText(context, R.string.invalid_dsm_provider, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!DSMClient.getDelegatedScopes(context).contains("delegation-package-access")) {
            Log.i("DsmAppHider", "No delegation-package-access, start to request...");
            DSMClient.requestScopes((Activity) context, DevicePolicyManager.DELEGATION_PACKAGE_ACCESS); // TODO: Process request result
        }

        return true;

    }

    @Override
    public String getName() {
        return "DSM";
    }
}
