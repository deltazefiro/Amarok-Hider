package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.FileHider.BaseFileHider;
import deltazero.amarok.FileHider.ChmodFileHider;
import deltazero.amarok.FileHider.NoMediaFileHider;
import deltazero.amarok.FileHider.NoneFileHider;
import deltazero.amarok.FileHider.ObfuscateFileHider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SwitchFileHiderActivity extends AppCompatActivity {

    PrefMgr prefMgr;
    MaterialToolbar tbToolBar;
    RadioButton rbDisabled, rbObfuscate, rbChmod, rbNoMedia;
    ImageView ivObfuscateSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_filehider);

        prefMgr = new PrefMgr(this);

        rbDisabled = findViewById(R.id.switch_filehider_radio_disabled);
        rbObfuscate = findViewById(R.id.switch_filehider_radio_obfuscate);
        rbChmod = findViewById(R.id.switch_filehider_radio_chmod);
        rbNoMedia = findViewById(R.id.switch_filehider_radio_nomedia);
        tbToolBar = findViewById(R.id.switch_filehider_tb_toolbar);
        ivObfuscateSettings = findViewById(R.id.switch_filehider_iv_obfuscate_settings);

        setCheckedRadioButton(prefMgr.getFileHider().getClass());

        tbToolBar.setNavigationOnClickListener(v -> finish());
        ivObfuscateSettings.setOnClickListener(v -> startActivity(new Intent(this, ObfuscateFileHiderSettingsActivity.class)));
    }


    @Override
    protected void onResume() {
        super.onResume();
        setCheckedRadioButton(prefMgr.getFileHider().getClass());
    }


    public void onCheckFileHiderRadioButton(View view) {
        int buttonID = view.getId();
        if (((RadioButton) view).isChecked()) {
            if (buttonID == R.id.switch_filehider_radio_disabled) {
                new NoneFileHider(this).tryToActive(this::onActivationCallback);
            } else if (buttonID == R.id.switch_filehider_radio_obfuscate) {
                new ObfuscateFileHider(this).tryToActive(this::onActivationCallback);
            } else if (buttonID == R.id.switch_filehider_radio_chmod) {
                new ChmodFileHider(this).tryToActive(this::onActivationCallback);
            } else if (buttonID == R.id.switch_filehider_radio_nomedia) {
                new NoMediaFileHider(this).tryToActive(this::onActivationCallback);
            }
        }
    }

    public void onClickLearnMoreButton(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.hideapp_doc_url))));
    }

    public void onClickOKButton(View view) {
        finish();
    }

    public void onActivationCallback(Class<? extends BaseFileHider> fileHider, boolean success, @Nullable Integer msgResID) {
        if (success) {
            prefMgr.setFileHiderMode(fileHider);
            setCheckedRadioButton(fileHider);
        } else {
            assert msgResID != null && msgResID != 0;

            prefMgr.setFileHiderMode(NoneFileHider.class);
            setCheckedRadioButton(NoneFileHider.class);

            runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.filehider_not_ava_title)
                    .setMessage(msgResID)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(R.string.help, (dialog, which)
                            -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.common_error_doc_url)))))
                    .show());
        }
    }


    private void setCheckedRadioButton(Class<? extends BaseFileHider> fileHider) {

        rbDisabled.setChecked(false);
        rbObfuscate.setChecked(false);
        rbNoMedia.setChecked(false);
        rbChmod.setChecked(false);

        if (fileHider.isAssignableFrom(NoneFileHider.class)) {
            rbDisabled.setChecked(true);
        } else if (fileHider.isAssignableFrom(ObfuscateFileHider.class)) {
            rbObfuscate.setChecked(true);
        } else if (fileHider.isAssignableFrom(ChmodFileHider.class)) {
            rbChmod.setChecked(true);
        } else if (fileHider.isAssignableFrom(NoMediaFileHider.class)) {
            rbNoMedia.setChecked(true);
        }
    }
}