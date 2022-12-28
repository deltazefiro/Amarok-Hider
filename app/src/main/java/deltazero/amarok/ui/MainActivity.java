package deltazero.amarok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.hjq.permissions.XXPermissions;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;
import deltazero.amarok.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "Main";

    private Hider hider;
    private PrefMgr prefMgr;

    private ImageView ivStatusImg;
    private TextView tvStatusInfo, tvStatus;
    private MaterialButton btChangeStatus, btSetHideFiles, btSetHideApps;
    private CircularProgressIndicator piProcessStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hider = new Hider(this);
        prefMgr = hider.prefMgr;

        // Start App-center
        AppCenterUtil.startAppCenter(this);

        // Init UI
        ivStatusImg = findViewById(R.id.main_iv_status);
        tvStatus = findViewById(R.id.main_tv_status);
        tvStatusInfo = findViewById(R.id.main_tv_statusinfo);
        btChangeStatus = findViewById(R.id.main_bt_change_status);
        btSetHideApps = findViewById(R.id.main_bt_set_hide_apps);
        btSetHideFiles = findViewById(R.id.main_bt_set_hide_files);
        piProcessStatus = findViewById(R.id.main_pi_process_status);
        updateUi();

        // Process Permissions
        PermissionUtil.requestStoragePermission(this);
        checkAppHiderAvailability();
    }

    private class onHiderCallback implements Hider.HiderCallback {
        // For background thread call back
        @Override
        public void onComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    piProcessStatus.hide();
                    btChangeStatus.setEnabled(true);
                    updateUi();
                }
            });
        }
    }

    public void changeStatus(View view) {
        piProcessStatus.show();
        btChangeStatus.setEnabled(false);
        if (prefMgr.getIsHidden()) {
            hider.Unhide(new onHiderCallback());
        } else {
            hider.Hide(new onHiderCallback());
        }
    }

    public void setHideApps(View view) {

        if (prefMgr.getIsHidden()) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show();
            return;
        }

        if (prefMgr.getAppHider() instanceof NoneAppHider) {
            Toast.makeText(this, R.string.no_apphider, Toast.LENGTH_LONG).show();
            return;
        }

        startActivity(new Intent(this, SetHideAppActivity.class));

    }

    public void showMoreSettings(View view) {

        startActivity(new Intent(this, SettingsActivity.class));

    }

    public void setHideFile(View view) {

        if (!XXPermissions.isGranted(this, com.hjq.permissions.Permission.MANAGE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show();
            return;
        }

        if (prefMgr.getIsHidden()) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(this, SetHideFilesActivity.class));
    }


    public void updateUi() {

        if (!prefMgr.getIsHidden()) {
            // Visible
            ivStatusImg.setImageResource(R.drawable.img_status_visible);
            btChangeStatus.setText(R.string.hide);
            btChangeStatus.setIconResource(R.drawable.ic_paw);
            btSetHideFiles.setEnabled(true);
            btSetHideApps.setEnabled(true);
            tvStatus.setText(getText(R.string.visible_status));
            tvStatusInfo.setText(getText(R.string.visible_moto));
        } else {
            // Hidden
            ivStatusImg.setImageResource(R.drawable.img_status_hidden);
            btChangeStatus.setText(R.string.unhide);
            btChangeStatus.setIconResource(R.drawable.ic_wolf);
            btSetHideFiles.setEnabled(false);
            btSetHideApps.setEnabled(false);
            tvStatus.setText(getText(R.string.hidden_status));
            tvStatusInfo.setText(getText(R.string.hidden_moto));
        }
    }


    public void checkAppHiderAvailability() {
        if (prefMgr.getAppHider().checkAvailability().result
                != AppHiderBase.CheckAvailabilityResult.Result.AVAILABLE) {

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.apphider_not_ava_title)
                    .setMessage(R.string.apphider_not_ava)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();

            prefMgr.setAppHiderMode(NoneAppHider.class);
        }
    }


    @Override
    protected void onResume() {
        updateUi();
        super.onResume();
    }

}




