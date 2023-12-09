package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.AppHider.DhizukuAppHider;
import deltazero.amarok.AppHider.DsmAppHider;
import deltazero.amarok.AppHider.BaseAppHider;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.AppHider.RootAppHider;
import deltazero.amarok.AppHider.ShizukuAppHider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SwitchAppHiderActivity extends AppCompatActivity {

    MaterialToolbar tbToolBar;
    RadioButton rbDisabled, rbRoot, rbShizuku, rbDSM, rbDhizuku;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_apphider);

        rbDisabled = findViewById(R.id.switch_apphider_radio_disabled);
        rbRoot = findViewById(R.id.switch_apphider_radio_root);
        rbShizuku = findViewById(R.id.switch_apphider_radio_shizuku);
        rbDSM = findViewById(R.id.switch_apphider_radio_dsm);
        rbDhizuku = findViewById(R.id.switch_apphider_radio_dhizuku);
        tbToolBar = findViewById(R.id.switch_apphider_tb_toolbar);

        setCheckedRadioButton(PrefMgr.getAppHider(this).getClass());

        // Enable back button
        tbToolBar.setNavigationOnClickListener(v -> finish());
    }


    @Override
    protected void onResume() {
        super.onResume();
        setCheckedRadioButton(PrefMgr.getAppHider(this).getClass());
    }


    public void onCheckAppHiderRadioButton(View view) {
        int buttonID = view.getId();
        if (((RadioButton) view).isChecked()) {
            if (buttonID == R.id.switch_apphider_radio_disabled) {
                new NoneAppHider(this).tryToActivate(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_root) {
                new RootAppHider(this).tryToActivate(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_shizuku) {
                new ShizukuAppHider(this).tryToActivate(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_dsm) {
                new DsmAppHider(this).tryToActivate(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_dhizuku) {
                new DhizukuAppHider(this).tryToActivate(this::onActivationCallback);
            }
        }
    }

    public void onClickLearnMoreButton(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.hideapp_doc_url))));
    }

    public void onClickOKButton(View view) {
        finish();
    }

    public void onActivationCallback(Class<? extends BaseAppHider> appHider, boolean success, @Nullable Integer msgResID) {
        if (success) {
            PrefMgr.setAppHiderMode(appHider);
            setCheckedRadioButton(appHider);
        } else {
            assert msgResID != null && msgResID != 0;

            PrefMgr.setAppHiderMode(NoneAppHider.class);
            setCheckedRadioButton(NoneAppHider.class);

            runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.apphider_not_ava_title)
                    .setMessage(msgResID)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(R.string.help, (dialog, which)
                            -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.common_error_doc_url)))))
                    .show());
        }
    }


    private void setCheckedRadioButton(Class<? extends BaseAppHider> appHider) {

        rbDisabled.setChecked(false);
        rbRoot.setChecked(false);
        rbShizuku.setChecked(false);
        rbDSM.setChecked(false);
        rbDhizuku.setChecked(false);

        if (appHider.isAssignableFrom(NoneAppHider.class)) {
            rbDisabled.setChecked(true);
        } else if (appHider.isAssignableFrom(RootAppHider.class)) {
            rbRoot.setChecked(true);
        } else if (appHider.isAssignableFrom(ShizukuAppHider.class)) {
            rbShizuku.setChecked(true);
        } else if (appHider.isAssignableFrom(DsmAppHider.class)) {
            rbDSM.setChecked(true);
        } else if (appHider.isAssignableFrom(DhizukuAppHider.class)) {
            rbDhizuku.setChecked(true);
        }
    }
}