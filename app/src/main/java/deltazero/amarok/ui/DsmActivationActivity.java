package deltazero.amarok.ui;

import static deltazero.amarok.AppHider.DsmAppHider.activationCallbackListener;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.catchingnow.delegatedscopeclient.DSMClient;

import deltazero.amarok.AppHider.DsmAppHider;
import deltazero.amarok.R;

public class DsmActivationActivity extends AppCompatActivity {

    public static final int dsmReqCode = 700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
        DSMClient.requestScopes(this, dsmReqCode, DevicePolicyManager.DELEGATION_PACKAGE_ACCESS);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == dsmReqCode) {
            assert activationCallbackListener != null;
            if (resultCode == RESULT_OK) {
                activationCallbackListener.onActivateCallback(DsmAppHider.class, true, 0);
            } else if (resultCode == RESULT_CANCELED) {
                Log.w("DsmAppHider", "DsmHider: Permission denied");
                activationCallbackListener.onActivateCallback(DsmAppHider.class, false, R.string.dsm_permission_denied);
            }
        }
    }
}
