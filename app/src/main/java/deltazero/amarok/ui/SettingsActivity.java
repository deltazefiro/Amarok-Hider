package deltazero.amarok.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import deltazero.amarok.AmarokApp;
import deltazero.amarok.BuildConfig;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;

public class SettingsActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private Context context;
    private String appVersionName;
    private MaterialSwitch swAnalytics, swAutoUpdate, swPanicButton;
    private MaterialToolbar tbToolBar;
    private TextView tvCurrAppHider;
    private TextView tvCurrFileHider;
    private TextView tvCurrVer;
    private RelativeLayout rlDebugInfo;

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
        swPanicButton = findViewById(R.id.settings_sw_panic_button);
        swAnalytics = findViewById(R.id.settings_sw_analytics);
        swAutoUpdate = findViewById(R.id.settings_sw_auto_update);
        tbToolBar = findViewById(R.id.settings_tb_toolbar);
        rlDebugInfo = findViewById(R.id.settings_rl_debug_info);

        // Init view
        tvCurrAppHider.setText(getString(R.string.current_mode, prefMgr.getAppHider().getName()));
        tvCurrFileHider.setText(getString(R.string.current_mode,
                getString(R.string.obfuscate_filename) +
                        (prefMgr.getEnableObfuscateFileHeader() ? " + " + getString(R.string.obfuscate_file_header) : "")));
        tvCurrVer.setText(getString(R.string.check_update_description, appVersionName));

        swPanicButton.setChecked(prefMgr.getEnablePanicButton());
        if (AppCenterUtil.isAvailable()) {
            swAnalytics.setChecked(AppCenterUtil.isAnalyticsEnabled());
            swAutoUpdate.setChecked(prefMgr.getEnableAutoUpdate());
        } else {
            swAnalytics.setEnabled(false);
            swAutoUpdate.setEnabled(false);
        }

        // Set Listener
        swPanicButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnablePanicButton(isChecked);
            ((AmarokApp) getApplication()).panicButton.updateToastState();
        });
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

        // Show debug button in debug mode
        if (BuildConfig.DEBUG) {
            rlDebugInfo.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        tvCurrAppHider.setText(getString(R.string.current_mode, prefMgr.getAppHider().getName()));
        tvCurrFileHider.setText(getString(R.string.current_mode,
                (prefMgr.getEnableObfuscateTextFile() ? getString(R.string.filename_and_header) : getString(R.string.filename_only))));
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
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider/releases")));
        }
    }

    public void openGithub(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider")));
    }

    public void openHelp(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.doc_url))));
    }

    public void joinDevGroup(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/amarok_app")));
    }

    public void showDebugInfo(View view) {
    }
}

