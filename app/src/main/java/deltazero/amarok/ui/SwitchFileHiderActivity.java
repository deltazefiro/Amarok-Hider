package deltazero.amarok.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SwitchFileHiderActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private MaterialSwitch swObfuscateFileHeader, swObfuscateTextFile, swObfuscateTextFileEnhanced;
    private RelativeLayout rlObfuscateTextFile, rlObfuscateTextFileEnhanced;
    private MaterialToolbar tbToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_filehider);

        prefMgr = new PrefMgr(this);

        swObfuscateFileHeader = findViewById(R.id.switch_filehider_sw_obfuscate_header);
        swObfuscateTextFile = findViewById(R.id.switch_filehider_sw_obfuscate_text);
        swObfuscateTextFileEnhanced = findViewById(R.id.switch_filehider_sw_obfuscate_text_enhanced);
        rlObfuscateTextFile = findViewById(R.id.switch_filehider_rl_obfuscate_text);
        rlObfuscateTextFileEnhanced = findViewById(R.id.switch_filehider_rl_obfuscate_text_enhanced);
        tbToolBar = findViewById(R.id.switch_filehider_tb_toolbar);

        updateUI();

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
        if (prefMgr.getIsHidden()) {
            swObfuscateFileHeader.setEnabled(false);
            Snackbar.make(tbToolBar, R.string.option_unava_when_hidden, Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    private void updateUI() {
        swObfuscateFileHeader.setChecked(prefMgr.getEnableObfuscateFileHeader());
        swObfuscateTextFile.setChecked(prefMgr.getEnableObfuscateTextFile());
        swObfuscateTextFileEnhanced.setChecked(prefMgr.getEnableObfuscateTextFileEnhanced());

        if (prefMgr.getEnableObfuscateFileHeader()) {
            rlObfuscateTextFile.setVisibility(View.VISIBLE);
        } else {
            rlObfuscateTextFile.setVisibility(View.GONE);
        }

        if (prefMgr.getEnableObfuscateFileHeader() && prefMgr.getEnableObfuscateTextFile()) {
            rlObfuscateTextFileEnhanced.setVisibility(View.VISIBLE);
        } else {
            rlObfuscateTextFileEnhanced.setVisibility(View.GONE);
        }

        // Disable switches if hidden.
        if (prefMgr.getIsHidden()) {
            swObfuscateFileHeader.setEnabled(false);
            swObfuscateTextFile.setEnabled(false);
            swObfuscateTextFileEnhanced.setEnabled(false);
            Snackbar.make(tbToolBar, R.string.option_unava_when_hidden, Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}