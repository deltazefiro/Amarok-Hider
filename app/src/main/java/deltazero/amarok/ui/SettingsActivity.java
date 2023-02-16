package deltazero.amarok.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.hjq.permissions.OnPermissionCallback;

import java.util.List;

import deltazero.amarok.BuildConfig;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.QuickHideService;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppCenterUtil;
import deltazero.amarok.utils.PermissionUtil;

public class SettingsActivity extends AppCompatActivity {

    private PrefMgr prefMgr;
    private Context context;
    private String appVersionName;
    private MaterialSwitch swAnalytics, swAutoUpdate, swPanicButton, swQuickHideNotification;
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
        swQuickHideNotification = findViewById(R.id.settings_sw_quick_hide_notification);
        swPanicButton = findViewById(R.id.settings_sw_panic_button);
        swAnalytics = findViewById(R.id.settings_sw_analytics);
        swAutoUpdate = findViewById(R.id.settings_sw_auto_update);
        tbToolBar = findViewById(R.id.settings_tb_toolbar);
        rlDebugInfo = findViewById(R.id.settings_rl_debug_info);

        // Init view
        updateUI();

        // Setup Listeners
        swQuickHideNotification.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (!buttonView.isPressed())
                return; // Triggered by setCheck

            if (isChecked) {
                PermissionUtil.requestNotificationPermission(this, new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        Log.d("QuickHideNotification", "Granted: NOTIFICATION");
                        prefMgr.setEnableQuickHideService(true);
                        QuickHideService.startService(context);
                        updateUI();
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        Log.w("QuickHideNotification", "User denied: NOTIFICATION");
                        Toast.makeText(context, R.string.notification_permission_denied, Toast.LENGTH_LONG).show();

                        prefMgr.setEnableQuickHideService(false);
                        prefMgr.setEnablePanicButton(false);
                        updateUI();
                    }
                });
            } else {
                prefMgr.setEnableQuickHideService(false);
                prefMgr.setEnablePanicButton(false);
                QuickHideService.stopService(this);
                updateUI();
            }
        }));

        swPanicButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed())
                return; // Triggered by setCheck

            if (isChecked) {
                PermissionUtil.requestSystemAlertPermission(this, new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        Log.d("PanicButton", "Granted: SYSTEM_ALERT_WINDOW");
                        prefMgr.setEnablePanicButton(true);
                        QuickHideService.startService(context);
                        updateUI();
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        Log.w("PanicButton", "User denied: SYSTEM_ALERT_WINDOW");
                        Toast.makeText(context, R.string.alert_permission_denied, Toast.LENGTH_LONG).show();
                        prefMgr.setEnablePanicButton(false);
                        updateUI();
                    }
                });
            } else {
                prefMgr.setEnablePanicButton(false);
                QuickHideService.startService(context); // Restart service
                updateUI();
            }
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

    private void updateUI() {
        tvCurrAppHider.setText(getString(R.string.current_mode, prefMgr.getAppHider().getName()));
        tvCurrFileHider.setText(getString(R.string.current_mode,
                (prefMgr.getEnableObfuscateTextFile() ? getString(R.string.filename_and_header) : getString(R.string.filename_only))));

        tvCurrVer.setText(getString(R.string.check_update_description, appVersionName));

        swQuickHideNotification.setChecked(prefMgr.getEnableQuickHideService());
        swPanicButton.setChecked(prefMgr.getEnablePanicButton());
        swPanicButton.setEnabled(prefMgr.getEnableQuickHideService());

        if (AppCenterUtil.isAvailable()) {
            swAnalytics.setChecked(AppCenterUtil.isAnalyticsEnabled());
            swAutoUpdate.setChecked(prefMgr.getEnableAutoUpdate());
        } else {
            swAnalytics.setEnabled(false);
            swAutoUpdate.setEnabled(false);
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        updateUI();
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
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/amarok_dev")));
    }

    public void showDebugInfo(View view) {
    }
}

