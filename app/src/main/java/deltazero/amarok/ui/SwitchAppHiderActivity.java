package deltazero.amarok.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.DsmAppHider;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.AppHider.RootAppHider;
import deltazero.amarok.AppHider.ShizukuHider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import rikka.shizuku.Shizuku;

public class SwitchAppHiderActivity extends AppCompatActivity {

    PrefMgr prefMgr;
    MaterialToolbar tbToolBar;
    RadioButton rbDisabled, rbRoot, rbShizuku, rbDSM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_apphider);

        rbDisabled = findViewById(R.id.switch_apphider_radio_disabled);
        rbRoot = findViewById(R.id.switch_apphider_radio_root);
        rbShizuku = findViewById(R.id.switch_apphider_radio_shizuku);
        rbDSM = findViewById(R.id.switch_apphider_radio_dsm);
        tbToolBar = findViewById(R.id.switch_apphider_tb_toolbar);

        prefMgr = new PrefMgr(this);
        setCheckedRadioButton(prefMgr.getAppHider().getClass());

        // Setup Shizuku permission callback listener
        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.i("ShizukuHider", "Permission granted. Set hider to ShizukuHider.");
                onActivationCallback(ShizukuHider.class, true, 0);
            } else {
                Log.i("ShizukuHider", "Permission denied.");
                onActivationCallback(ShizukuHider.class, false, R.string.shizuku_permission_denied);
            }
        });

        // Enable back button
        tbToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        setCheckedRadioButton(prefMgr.getAppHider().getClass());
    }


    public void onCheckAppHiderRadioButton(View view) {
        int buttonID = view.getId();
        if (((RadioButton) view).isChecked()) {
            if (buttonID == R.id.switch_apphider_radio_disabled) {
                new NoneAppHider(this).active(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_root) {
                new RootAppHider(this).active(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_shizuku) {
                new ShizukuHider(this).active(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_dsm) {
                new DsmAppHider(this).active(this::onActivationCallback);
            }
        }
    }

    public void onClickLearnMoreButton(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.hideapp_doc_url))));
    }

    public void onClickOKButton(View view) {
        finish();
    }

    public void onActivationCallback(Class<? extends AppHiderBase> appHider, boolean success, int msgResID) {
        if (success) {

            prefMgr.setAppHiderMode(appHider);
            setCheckedRadioButton(appHider);

        } else {

            prefMgr.setAppHiderMode(NoneAppHider.class);
            setCheckedRadioButton(NoneAppHider.class);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.apphider_not_ava_title)
                    .setMessage(msgResID)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(R.string.help, (dialog, which)
                            -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.common_error_doc_url)))))
                    .show();
        }
    }

    // Callback for DSM permission request
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DsmAppHider.dsmReqCode) {
            if (resultCode == RESULT_OK) {
                onActivationCallback(DsmAppHider.class, true, 0);
            } else if (resultCode == RESULT_CANCELED) {
                Log.w("DsmAppHider", "DsmHider: Permission denied");
                onActivationCallback(DsmAppHider.class, false, R.string.dsm_permission_denied);
            }
        }
    }


    private void setCheckedRadioButton(Class<? extends AppHiderBase> appHider) {
        // Thank you, ChatGPT!
        if (appHider.isAssignableFrom(NoneAppHider.class)) {
            rbDisabled.setChecked(true);
            rbRoot.setChecked(false);
            rbShizuku.setChecked(false);
            rbDSM.setChecked(false);
        } else if (appHider.isAssignableFrom(RootAppHider.class)) {
            rbDisabled.setChecked(false);
            rbRoot.setChecked(true);
            rbShizuku.setChecked(false);
            rbDSM.setChecked(false);
        } else if (appHider.isAssignableFrom(ShizukuHider.class)) {
            rbDisabled.setChecked(false);
            rbRoot.setChecked(false);
            rbShizuku.setChecked(true);
            rbDSM.setChecked(false);
        } else if (appHider.isAssignableFrom(DsmAppHider.class)) {
            rbDisabled.setChecked(false);
            rbRoot.setChecked(false);
            rbShizuku.setChecked(false);
            rbDSM.setChecked(true);
        }
    }
}