package deltazero.amarok.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.hjq.permissions.XXPermissions;

import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.FileHider.NoneFileHider;
import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;
import deltazero.amarok.utils.BetterActivityLauncher;
import deltazero.amarok.utils.PermissionUtil;
import deltazero.amarok.utils.SecurityAuth;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "Main";

    private ScrollView svMainLayout;
    private ImageView ivStatusImg;
    private TextView tvStatusInfo, tvStatus;
    private MaterialButton btChangeStatus, btSetHideFiles, btSetHideApps;
    private CircularProgressIndicator piProcessStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start App-center
        AppCenterUtil.startAppCenter(this);

        // Binding views
        svMainLayout = findViewById(R.id.main_sv_main_layout);
        ivStatusImg = findViewById(R.id.main_iv_status);
        tvStatus = findViewById(R.id.main_tv_status);
        tvStatusInfo = findViewById(R.id.main_tv_statusinfo);
        btChangeStatus = findViewById(R.id.main_bt_change_status);
        btSetHideApps = findViewById(R.id.main_bt_set_hide_apps);
        btSetHideFiles = findViewById(R.id.main_bt_set_hide_files);
        piProcessStatus = findViewById(R.id.main_pi_process_status);

        // Init UI
        svMainLayout.setVisibility(View.GONE);
        refreshUi(Hider.getState());

        // Setup observer
        Hider.state.observe(this, this::refreshUi);

        // Launch disguise activity if needed
        var activityLauncher = BetterActivityLauncher.registerActivityForResult(this);
        if (PrefMgr.getEnableDisguise()) {
            activityLauncher.launch(new Intent(this, CalendarActivity.class), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    new SecurityAuth(this, succeed -> {
                        if (succeed) init();
                        else finish();
                    }).authenticate();
                } else {
                    finish();
                }
            });
        } else {
            // Show security check fragment
            new SecurityAuth(this, succeed -> {
                if (succeed) init();
                else finish();
            }).authenticate();
        }
    }

    public void init() {

        // Show main UI
        svMainLayout.setVisibility(View.VISIBLE);

        if (PrefMgr.getShowWelcome()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.welcome_title)
                    .setMessage(R.string.welcome_msg)
                    .setPositiveButton(R.string.ok, (dialog, which)
                            -> PermissionUtil.requestStoragePermission(this))
                    .setNegativeButton(R.string.view_github_repo, (dialog, which) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/deltazefiro/Amarok-Hider")));
                        PermissionUtil.requestStoragePermission(this);
                    })
                    .setOnCancelListener(dialog -> PermissionUtil.requestStoragePermission(this))
                    .show();
            PrefMgr.setShowWelcome(false);
        } else {
            PermissionUtil.requestStoragePermission(this);
        }

        // Check AppHider availability
        PrefMgr.getAppHider(this).tryToActivate((appHiderClass, succeed, msg) -> {
            if (!succeed) {
                PrefMgr.setAppHiderMode(NoneAppHider.class);
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.apphider_not_ava_title)
                        .setMessage(msg)
                        .setPositiveButton(R.string.switch_app_hider, (dialog, which)
                                -> startActivity(new Intent(this, SwitchAppHiderActivity.class)))
                        .setNegativeButton(getString(R.string.ok), null)
                        .show();
            }
        });

        PrefMgr.getFileHider(this).tryToActive((fileHiderClass, succeed, msg) -> {
            if (!succeed) {
                PrefMgr.setFileHiderMode(NoneFileHider.class);
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.filehider_not_ava_title)
                        .setMessage(msg)
                        .setPositiveButton(R.string.switch_file_hider, (dialog, which)
                                -> startActivity(new Intent(this, SwitchFileHiderActivity.class)))
                        .setNegativeButton(getString(R.string.ok), null)
                        .show();
            }
        });
    }

    public void changeStatus(View view) {
        if (Hider.getState() == Hider.State.HIDDEN) Hider.unhide(this);
        else Hider.hide(this);
    }

    public void setHideApps(View view) {

        if (Hider.getState() == Hider.State.HIDDEN) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show();
            return;
        }

        if (PrefMgr.getAppHider(this) instanceof NoneAppHider) {
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

        if (Hider.getState() == Hider.State.HIDDEN) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(this, SetHideFilesActivity.class));
    }

    public void refreshUi(Hider.State state) {
        switch (state) {
            case HIDDEN -> {
                // Not Processing
                piProcessStatus.hide();
                btChangeStatus.setEnabled(true);
                // Hidden
                ivStatusImg.setImageResource(R.drawable.img_status_hidden);
                ivStatusImg.setImageTintList(getColorStateList(com.google.android.material.R.color.material_on_background_emphasis_high_type));
                btChangeStatus.setText(R.string.unhide);
                btChangeStatus.setIconResource(R.drawable.ic_wolf);
                btSetHideFiles.setEnabled(false);
                btSetHideApps.setEnabled(false);
                tvStatus.setText(getText(R.string.hidden_status));
                tvStatusInfo.setText(getText(R.string.hidden_moto));
            }
            case VISIBLE -> {
                // Not Processing
                piProcessStatus.hide();
                btChangeStatus.setEnabled(true);
                // Visible
                ivStatusImg.setImageResource(R.drawable.img_status_visible);
                ivStatusImg.setImageTintList(null);
                btChangeStatus.setText(R.string.hide);
                btChangeStatus.setIconResource(R.drawable.ic_paw);
                btSetHideFiles.setEnabled(true);
                btSetHideApps.setEnabled(true);
                tvStatus.setText(getText(R.string.visible_status));
                tvStatusInfo.setText(getText(R.string.visible_moto));
            }
            case PROCESSING -> {
                // Processing
                piProcessStatus.show();
                btChangeStatus.setEnabled(false);
            }
        }
    }

    @Override
    protected void onResume() {
        refreshUi(Hider.getState());
        super.onResume();
    }
}




