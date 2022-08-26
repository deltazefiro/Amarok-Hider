package deltazero.amarok.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.XXPermissions;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "Main";

    private Hider hider;
    private PrefMgr prefMgr;
    private MaterialButton btPrimary, btSecondary;
    private TextView tvStatusInfo, tvStatus;
    private String appVersionName;

    private ActivityResultLauncher<Uri> mDirRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        hider = new Hider(this);
        prefMgr = hider.prefMgr;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init UI
        btPrimary = findViewById(R.id.main_bt_primary);
        btSecondary = findViewById(R.id.main_bt_secondary);
        tvStatus = findViewById(R.id.main_tv_status);
        tvStatusInfo = findViewById(R.id.main_tv_statusinfo);
        updateUi();

        // Get app version
        try {
            appVersionName = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Make compiler happy
        }

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

                        Set<String> hideFilePath =  new HashSet<String>(); // TODO: Support multiple path
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

    public void buttonUnhide(View view) {
        hider.unhide();
        updateUi();
    }

    public void buttonHide(View view) {
        hider.hide();
        updateUi();
    }

    public void buttonSetHideApps(View view) {

        if (prefMgr.getIsHidden()) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hider.appHider.isAvailable) {
            Toast.makeText(this, R.string.apphider_not_ava, Toast.LENGTH_LONG).show();
            Log.i(TAG, "AppHider not available");
        }

        startActivity(new Intent(this, SelectHideAppActivity.class));

    }

    public void buttonShowAbout(View view) {

        String hideAppAva = hider.appHider.isAvailable ? "Available" : "Unavailable";

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about))
                .setMessage(String.format(getString(R.string.app_about), appVersionName, hideAppAva))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void buttonSetHideFile(View view) {

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
            btPrimary.setText(getText(R.string.hide));
            btPrimary.setOnClickListener(this::buttonHide);
            btSecondary.setText(getText(R.string.unhide));
            btSecondary.setOnClickListener(this::buttonUnhide);

            tvStatus.setText(getText(R.string.visible_status));
            tvStatusInfo.setText(getText(R.string.visible_moto));
        } else {
            // Hidden
            btPrimary.setText(getText(R.string.unhide));
            btPrimary.setOnClickListener(this::buttonUnhide);
            btSecondary.setText(getText(R.string.hide));
            btSecondary.setOnClickListener(this::buttonHide);

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




