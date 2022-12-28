package deltazero.amarok.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SwitchFileHiderActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private MaterialSwitch swEnableCorruptFileHider;
    private MaterialToolbar tbToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_filehider);

        prefMgr = new PrefMgr(this);

        swEnableCorruptFileHider = findViewById(R.id.switch_filehider_sw_corrupt_header);
        tbToolBar = findViewById(R.id.switch_filehider_tb_toolbar);

        swEnableCorruptFileHider.setChecked(prefMgr.getEnableCorruptFileHeader());
        swEnableCorruptFileHider.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableCorruptFileHeader(isChecked);
        });

        // Enable back button
        tbToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Disable switches if hidden.
        if (prefMgr.getIsHidden()) {
            swEnableCorruptFileHider.setEnabled(false);
            Snackbar.make(tbToolBar, R.string.option_unava_when_hidden, Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefMgr.getIsHidden()) {
            swEnableCorruptFileHider.setEnabled(false);
            Snackbar.make(tbToolBar, R.string.option_unava_when_hidden, Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}