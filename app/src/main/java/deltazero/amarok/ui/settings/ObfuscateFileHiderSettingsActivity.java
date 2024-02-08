package deltazero.amarok.ui.settings;

import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class ObfuscateFileHiderSettingsActivity extends AmarokActivity {

    private MaterialSwitch swObfuscateFileHeader, swObfuscateTextFile, swObfuscateTextFileEnhanced;
    private MaterialToolbar tbToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obfuscate_filehider_settings);

        swObfuscateFileHeader = findViewById(R.id.switch_filehider_sw_obfuscate_header);
        swObfuscateTextFile = findViewById(R.id.switch_filehider_sw_obfuscate_text);
        swObfuscateTextFileEnhanced = findViewById(R.id.switch_filehider_sw_obfuscate_text_enhanced);
        tbToolBar = findViewById(R.id.switch_filehider_tb_toolbar);

        // Init UI
        swObfuscateFileHeader.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefMgr.setEnableObfuscateFileHeader(isChecked);
            if (!isChecked) {
                PrefMgr.setEnableObfuscateTextFile(false);
                PrefMgr.setEnableObfuscateTextFileEnhanced(false);
            }
            updateUI();
        });
        swObfuscateTextFile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefMgr.setEnableObfuscateTextFile(isChecked);
            if (!isChecked) {
                PrefMgr.setEnableObfuscateTextFileEnhanced(false);
            }
            updateUI();
        });
        swObfuscateTextFileEnhanced.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefMgr.setEnableObfuscateTextFileEnhanced(isChecked);
            updateUI();
        });

        // Enable back button
        tbToolBar.setNavigationOnClickListener(v -> finish());

        // Update UI
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        swObfuscateFileHeader.setChecked(PrefMgr.getEnableObfuscateFileHeader());
        swObfuscateTextFile.setChecked(PrefMgr.getEnableObfuscateTextFile());
        swObfuscateTextFileEnhanced.setChecked(PrefMgr.getEnableObfuscateTextFileEnhanced());

        swObfuscateTextFile.setEnabled(PrefMgr.getEnableObfuscateFileHeader());
        swObfuscateTextFileEnhanced.setEnabled(PrefMgr.getEnableObfuscateFileHeader() && PrefMgr.getEnableObfuscateTextFile());
    }
}