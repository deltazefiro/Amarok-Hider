package deltazero.amarok.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;

public class SettingsActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private Context context;
    private String appVersionName;
    private MaterialSwitch swAnalytics, swAutoUpdate;
    private MaterialToolbar tbToolBar;
    private TextView tvCurrAppHider;
    private TextView tvCurrFileHider;
    private TextView tvCurrVer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefMgr = new PrefMgr(this);
        context = this;

        // Get app version
        try {
            appVersionName = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            appVersionName = null;
            // Make compiler happy
        }
        assert appVersionName != null;

        tvCurrAppHider = findViewById(R.id.settings_tv_curr_app_hider);
        tvCurrFileHider = findViewById(R.id.settings_tv_curr_file_hider);
        tvCurrVer = findViewById(R.id.settings_tv_curr_ver);
        swAnalytics = findViewById(R.id.settings_sw_analytics);
        swAutoUpdate = findViewById(R.id.settings_sw_auto_update);
        tbToolBar = findViewById(R.id.settings_tb_toolbar);

        tvCurrAppHider.setText(getString(R.string.current_mode, prefMgr.getAppHider().getName()));
        tvCurrFileHider.setText(getString(R.string.current_mode,
                getString(R.string.obfuscate_filename) +
                        (prefMgr.getEnableCorruptFileHeader() ? " + " + getString(R.string.corrupt_file_header) : "")));
        tvCurrVer.setText(getString(R.string.check_update_description, appVersionName));

        if (AppCenterUtil.isAvailable())
        {
            swAnalytics.setChecked(AppCenterUtil.isAnalyticsEnabled());
            swAutoUpdate.setChecked(prefMgr.getEnableAutoUpdate());
        } else {
            swAnalytics.setEnabled(false);
            swAutoUpdate.setEnabled(false);
        }

        swAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCenterUtil.setAnalyticsEnabled(isChecked);
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
        });

        swAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableAutoUpdate(isChecked);

            if (isChecked) {
                AppCenterUtil.cleanUpdatePostpone();
            }

            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
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
    public void onResume() {
        super.onResume();
        tvCurrAppHider.setText(getString(R.string.current_mode, prefMgr.getAppHider().getName()));
        tvCurrFileHider.setText(getString(R.string.current_mode,
                (prefMgr.getEnableCorruptFileHeader() ? getString(R.string.filename_and_header) : getString(R.string.filename_only))));
    }

    public void showAbout(View view) {

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about))
                .setMessage(String.format(getString(R.string.app_about), appVersionName))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void switchAppHider(View view) {
        startActivity(new Intent(this, SwitchAppHiderActivity.class));
    }

    public void switchFileHider(View view) {
        startActivity(new Intent(this, SwitchFileHiderActivity.class));
    }

    public void checkUpdate(View view) {
        if (AppCenterUtil.isAvailable()) {
            AppCenterUtil.checkUpdate();
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://apt.izzysoft.de/fdroid/index/apk/deltazero.amarok")));
        }
    }

    public void openGithub(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider")));
    }

    public void openHelp(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://deltazefiro.github.io/Amarok-doc/")));

    }

}

