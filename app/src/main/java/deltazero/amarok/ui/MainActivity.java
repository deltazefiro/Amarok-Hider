package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.hjq.permissions.XXPermissions;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.apphider.NoneAppHider;
import deltazero.amarok.filehider.NoneFileHider;
import deltazero.amarok.ui.settings.SettingsActivity;
import deltazero.amarok.ui.settings.SwitchAppHiderActivity;
import deltazero.amarok.ui.settings.SwitchFileHiderActivity;
import deltazero.amarok.utils.PermissionUtil;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class MainActivity extends AmarokActivity {

    public final static String TAG = "Main";
    private ImageView ivStatusImg;
    private TextView tvStatusInfo, tvStatus, tvMoto;
    private MaterialButton btChangeStatus, btSetHideFiles, btSetHideApps;
    private CircularProgressIndicator piProcessStatus;
    private KonfettiView konfettiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Binding views
        ivStatusImg = findViewById(R.id.main_iv_status);
        tvStatus = findViewById(R.id.main_tv_status);
        tvStatusInfo = findViewById(R.id.main_tv_statusinfo);
        tvMoto = findViewById(R.id.main_tv_moto);
        btChangeStatus = findViewById(R.id.main_bt_change_status);
        btSetHideApps = findViewById(R.id.main_bt_set_hide_apps);
        btSetHideFiles = findViewById(R.id.main_bt_set_hide_files);
        piProcessStatus = findViewById(R.id.main_pi_process_status);
        konfettiView = findViewById(R.id.main_konfetti_view);

        // Init UI
        refreshUi(Hider.getState());

        // Setup observer
        Hider.state.observe(this, this::refreshUi);

        // Show welcome dialog
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

        // Check Hiders availability
        PrefMgr.getAppHider(this).tryToActivate((appHiderClass, succeed, msg) -> {
            if (succeed) return;
            Hider.showNoHiderDialog(this, msg);
        });

        PrefMgr.getFileHider(this).tryToActive((fileHiderClass, succeed, msg) -> {
            if (succeed) return;
            PrefMgr.setFileHiderMode(NoneFileHider.class);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.filehider_not_ava_title)
                    .setMessage(msg)
                    .setPositiveButton(R.string.switch_file_hider, (dialog, which)
                            -> startActivity(new Intent(this, SwitchFileHiderActivity.class)))
                    .setNegativeButton(getString(R.string.ok), null)
                    .show();
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
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.apphider_not_activated_title)
                    .setMessage(R.string.apphider_not_activated_msg)
                    .setPositiveButton(R.string.switch_app_hider, (dialog, which)
                            -> startActivity(new Intent(this, SwitchAppHiderActivity.class)))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
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
        tvMoto.setText(R.string.moto);
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




