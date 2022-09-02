package deltazero.amarok.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.switchmaterial.SwitchMaterial;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SettingsActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private MaterialSwitch swAnalytics;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefMgr = new PrefMgr(this);
        context = this;

        swAnalytics = findViewById(R.id.settings_sw_analytics);
        swAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableAnalytics(isChecked);
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
        });
        swAnalytics.setChecked(prefMgr.getEnableAnalytics());
    }

    public void showAbout(View view) {

        // Get app version
        String appVersionName;
        try {
            appVersionName = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            appVersionName = null;
            // Make compiler happy
        }
        assert appVersionName != null;

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about))
                .setMessage(String.format(getString(R.string.app_about), appVersionName))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void switchAppHider(View view) {
    }

}