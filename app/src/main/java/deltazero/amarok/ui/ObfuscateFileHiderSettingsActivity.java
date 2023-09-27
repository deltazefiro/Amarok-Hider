package deltazero.amarok.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class ObfuscateFileHiderSettingsActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private MaterialSwitch swObfuscateFileHeader, swObfuscateTextFile, swObfuscateTextFileEnhanced;
    private MaterialToolbar tbToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obfuscate_filehider_settings);

        prefMgr = new PrefMgr(this);

        swObfuscateFileHeader = findViewById(R.id.switch_filehider_sw_obfuscate_header);
        swObfuscateTextFile = findViewById(R.id.switch_filehider_sw_obfuscate_text);
        swObfuscateTextFileEnhanced = findViewById(R.id.switch_filehider_sw_obfuscate_text_enhanced);
        tbToolBar = findViewById(R.id.switch_filehider_tb_toolbar);

        // Init UI
        swObfuscateFileHeader.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableObfuscateFileHeader(isChecked);
            if (!isChecked) {
                prefMgr.setEnableObfuscateTextFile(false);
                prefMgr.setEnableObfuscateTextFileEnhanced(false);
            }
            updateUI();
        });
        swObfuscateTextFile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableObfuscateTextFile(isChecked);
            if (!isChecked) {
                prefMgr.setEnableObfuscateTextFileEnhanced(false);
            }
            updateUI();
        });
        swObfuscateTextFileEnhanced.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableObfuscateTextFileEnhanced(isChecked);
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
        swObfuscateFileHeader.setChecked(prefMgr.getEnableObfuscateFileHeader());
        swObfuscateTextFile.setChecked(prefMgr.getEnableObfuscateTextFile());
        swObfuscateTextFileEnhanced.setChecked(prefMgr.getEnableObfuscateTextFileEnhanced());

        swObfuscateTextFile.setEnabled(prefMgr.getEnableObfuscateFileHeader());
        swObfuscateTextFileEnhanced.setEnabled(prefMgr.getEnableObfuscateFileHeader() && prefMgr.getEnableObfuscateTextFile());
    }
}