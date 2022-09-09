package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.hjq.permissions.XXPermissions;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "Main";

    private Hider hider;
    private PrefMgr prefMgr;

    private ImageView ivStatusImg;
    private TextView tvStatusInfo, tvStatus;
    private MaterialButton btChangeStatus, btSetHideFiles, btSetHideApps;
    private CircularProgressIndicator piProcessStatus;

    private ActivityResultLauncher<Uri> mDirRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Start App-center Analytics
        AppCenter.start(getApplication(), "6bcd9547-9df2-4023-bfcd-6e1a0f0f9e12",
                Analytics.class, Crashes.class);

        // Init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hider = new Hider(this);
        prefMgr = hider.prefMgr;

        // Apply Analytics Switch
        AppCenter.setEnabled(prefMgr.getEnableAnalytics());


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

        // Register file-picker result handler
        mDirRequest = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        // call this to persist permission across device reboots
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        // set hide path
                        String path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];

                        Set<String> hideFilePath = new HashSet<String>(); // TODO: Support multiple path
                        hideFilePath.add(path);
                        prefMgr.setHideFilePath(hideFilePath);

                        Log.i(TAG, "Set file hide path: " + path);

                    } else {
                        // request denied by user
                        Log.i(TAG, "Set file hide path cancelled");
                    }
                }
        );

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

        if (prefMgr.getAppHider() instanceof NoneAppHider || !prefMgr.getAppHider().checkAvailability()) {
            Toast.makeText(this, R.string.apphider_not_ava, Toast.LENGTH_LONG).show();
            Log.i(TAG, "AppHider not available");
            return;
        }

        startActivity(new Intent(this, SelectHideAppActivity.class));

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
        mDirRequest.launch(null);
    }


    public void updateUi() {

        if (!prefMgr.getIsHidden()) {
            // Visible
            ivStatusImg.setImageResource(R.drawable.img_status_visible);
            btChangeStatus.setText(R.string.hide);
            btChangeStatus.setIconResource(R.drawable.ic_button_hide);
            btSetHideFiles.setEnabled(true);
            btSetHideApps.setEnabled(true);
            tvStatus.setText(getText(R.string.visible_status));
            tvStatusInfo.setText(getText(R.string.visible_moto));
        } else {
            // Hidden
            ivStatusImg.setImageResource(R.drawable.img_status_hidden);
            btChangeStatus.setText(R.string.unhide);
            btChangeStatus.setIconResource(R.drawable.ic_button_unhide);
            btSetHideFiles.setEnabled(false);
            btSetHideApps.setEnabled(false);
            tvStatus.setText(getText(R.string.hidden_status));
            tvStatusInfo.setText(getText(R.string.hidden_moto));
        }
    }


    @Override
    protected void onResume() {
        updateUi();
        super.onResume();
    }

}




