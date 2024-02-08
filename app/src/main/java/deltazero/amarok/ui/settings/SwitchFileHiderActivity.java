package deltazero.amarok.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.filehider.BaseFileHider;
import deltazero.amarok.filehider.ChmodFileHider;
import deltazero.amarok.filehider.NoMediaFileHider;
import deltazero.amarok.filehider.NoneFileHider;
import deltazero.amarok.filehider.ObfuscateFileHider;

public class SwitchFileHiderActivity extends AmarokActivity {

    MaterialToolbar tbToolBar;
    RadioButton rbDisabled, rbObfuscate, rbChmod, rbNoMedia;
    ImageView ivObfuscateSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_filehider);

        rbDisabled = findViewById(R.id.switch_filehider_radio_disabled);
        rbObfuscate = findViewById(R.id.switch_filehider_radio_obfuscate);
        rbChmod = findViewById(R.id.switch_filehider_radio_chmod);
        rbNoMedia = findViewById(R.id.switch_filehider_radio_nomedia);
        tbToolBar = findViewById(R.id.switch_filehider_tb_toolbar);
        ivObfuscateSettings = findViewById(R.id.switch_filehider_iv_obfuscate_settings);

        setCheckedRadioButton(PrefMgr.getFileHider(this).getClass());

        tbToolBar.setNavigationOnClickListener(v -> finish());
        ivObfuscateSettings.setOnClickListener(v -> startActivity(new Intent(this, ObfuscateFileHiderSettingsActivity.class)));
    }


    @Override
    protected void onResume() {
        super.onResume();
        setCheckedRadioButton(PrefMgr.getFileHider(this).getClass());
    }


    public void onCheckFileHiderRadioButton(View view) {
        int buttonID = view.getId();
        if (((RadioButton) view).isChecked()) {
            if (buttonID == R.id.switch_filehider_radio_disabled) {
                new NoneFileHider(this).tryToActive(this::onActivationCallback);
            } else if (buttonID == R.id.switch_filehider_radio_obfuscate) {
                new ObfuscateFileHider(this).tryToActive(this::onActivationCallback);
            } else if (buttonID == R.id.switch_filehider_radio_chmod) {
                // FIXME: Samsung devices may failed to boot with chmod mode
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.chmod_samsung_warning)
                        .setPositiveButton(R.string.ok, (dialog, which) -> new ChmodFileHider(this).tryToActive(this::onActivationCallback))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> new NoneFileHider(this).tryToActive(this::onActivationCallback))
                        .show();
                // new ChmodFileHider(this).tryToActive(this::onActivationCallback);
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
            PrefMgr.setFileHiderMode(fileHider);
            setCheckedRadioButton(fileHider);
        } else {
            assert msgResID != null && msgResID != 0;

            PrefMgr.setFileHiderMode(NoneFileHider.class);
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