package deltazero.amarok.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SettingsActivity extends Activity {

    private PrefMgr prefMgr;
    private Context context;
    private String appVersionName;
    private MaterialSwitch swAnalytics, swAutoUpdate;
    private MaterialToolbar tbToolBar;
    private TextView tvCurrAppHider, tvCurrFileHider, tvCurrVer;

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
        tvCurrFileHider.setText(getString(R.string.current_mode, "HideOnly"));
        tvCurrVer.setText(getString(R.string.check_update_description, appVersionName));
        swAnalytics.setChecked(Crashes.isEnabled().get());
        swAutoUpdate.setChecked(prefMgr.getEnableAutoUpdate());

        swAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Crashes.setEnabled(isChecked);
            Analytics.setEnabled(isChecked);
            Toast.makeText(context, R.string.apply_on_restart, Toast.LENGTH_SHORT).show();
        });

        swAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefMgr.setEnableAutoUpdate(isChecked);

            // Clean postpone
            if (isChecked) {
                Distribute.setEnabled(false);
                Distribute.setEnabled(true);
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
    }

    public void showAbout(View view) {

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about))
                .setMessage(String.format(getString(R.string.app_about), appVersionName))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void switchAppHider(View view) {
        CharSequence[] hiders = {"None", "RootHider", "DsmHider"};
        final int[] choice = {prefMgr.getAppHiderCode()};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.switch_app_hider)
                // .setMessage(R.string.switch_app_hider_description)
                .setSingleChoiceItems(hiders, choice[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        choice[0] = which;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefMgr.setAppHiderMode(choice[0]);
                        AppHiderBase hider = prefMgr.getAppHider();
                        if (!(hider instanceof NoneAppHider || hider.checkAvailability())) {
                            // Not available.
                            prefMgr.setAppHiderMode(0);
                        }
                        tvCurrAppHider.setText(getString(R.string.current_mode, prefMgr.getAppHider().getName()));
                    }
                })
                .show();
    }

    public void showDebugInfo(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Debug Info")
                .setMessage(String.format("DSMClient.getOwnerSDKVersion: %s\n" +
                                "DSMClient.getOwnerPackageName: %s",
                        DSMClient.getOwnerSDKVersion(this), DSMClient.getOwnerPackageName(this)))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void checkUpdate(View view) {
        // To clean postpone
        Distribute.setEnabled(false);
        Distribute.setEnabled(true);

        Distribute.checkForUpdate();
    }

}