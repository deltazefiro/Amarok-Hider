package deltazero.amarok;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "Main";
    private static final int PICK_PATH_RESULT_CODE = 1;
    public Hider hider;
    private MaterialButton btPrimary, btSecondary;
    private TextView tvStatusInfo, tvStatus;
    private String appVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        hider = new Hider(this);
        if (hider.getIsNight()) {
            this.setTheme(R.style.Theme_Amarok_night);
        } else {
            this.setTheme(R.style.Theme_Amarok_day);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init UI
        btPrimary = findViewById(R.id.main_bt_primary);
        btSecondary = findViewById(R.id.main_bt_secondary);
        tvStatus = findViewById(R.id.main_tv_status);
        tvStatusInfo = findViewById(R.id.main_tv_statusinfo);
        updateUi(true);

        // Get app version
        try {
            appVersionName = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            // Make compiler happy
        }

        // Check Permissions
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No WRITE_EXTERNAL_STORAGE permission. Requesting...");
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (checkSelfPermission(IceBox.SDK_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No IceBox permission. Requesting...");
            listPermissionsNeeded.add(IceBox.SDK_PERMISSION);
        }

        if (!listPermissionsNeeded.isEmpty())
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        switch (requestCode) {
            case PICK_PATH_RESULT_CODE:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    File file = new File(resultData.getData().getPath());
                    String path = Environment.getExternalStorageDirectory() + "/" + file.getPath().split(":")[1];
                    hider.setEncodePath(path);
                    Log.i(TAG, "Set encode path: " + path);
                    Toast.makeText(this, "Set encode path: " + path, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void buttonDusk(View view) {
        hider.dusk();
        updateUi(false);
    }

    public void buttonDawn(View view) {
        hider.dawn();
        updateUi(false);
    }

    public void buttonSetHideApps(View view) {

        if (!hider.getIsNight()) {
            Toast.makeText(this, R.string.ava_at_night, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Icebox availability
        switch (IceBox.querySupportSilentInstall(this)) {
            case SUPPORTED:
                break;
            case NOT_INSTALLED:
                Toast.makeText(this, R.string.icebox_not_installed, Toast.LENGTH_LONG).show();
                return;
            case NOT_DEVICE_OWNER:
                Toast.makeText(this, R.string.icebox_not_active, Toast.LENGTH_LONG).show();
                return;
            default:
                Toast.makeText(this, R.string.icebox_not_supported, Toast.LENGTH_LONG).show();
                return;
        }

        // Set up the etInput
        final EditText etInput = new EditText(this);
        Set<String> originalPkgNames = hider.getHidePkgNames();
        etInput.setText(String.join("\n", (originalPkgNames != null ? originalPkgNames : new HashSet<String>())));
        etInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.get_app_names_title))
                .setMessage(R.string.get_app_names_info)
                .setView(etInput)
                .setPositiveButton("OK", (dialog, which) -> {
                    String input = etInput.getText().toString();
                    hider.setHidePkgNames(new HashSet<>(Arrays.asList(input.split("\n", -1))));
                    Toast.makeText(this, "Hide App set: " + input, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel())
                .show();
    }

    public void buttonShowAbout(View view) {

        String iceboxAvailability;
        switch (IceBox.querySupportSilentInstall(this)) {
            case SUPPORTED:
                iceboxAvailability = getString(R.string.icebox_available);
                break;
            case NOT_INSTALLED:
                iceboxAvailability = getString(R.string.icebox_not_installed);
                break;
            case NOT_DEVICE_OWNER:
                iceboxAvailability = getString(R.string.icebox_not_active);
                break;
            default:
                iceboxAvailability = getString(R.string.icebox_not_supported);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about))
                .setMessage(String.format(getString(R.string.app_about), appVersionName, iceboxAvailability))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void buttonSetEncodeFile(View view) {

        if (!hider.getIsNight()) {
            Toast.makeText(this, R.string.ava_at_night, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, PICK_PATH_RESULT_CODE);
    }


    public void updateUi(boolean isInitializing) {

        if (!isInitializing) {
            boolean uiIsNight = (tvStatus.getText() == getText(R.string.night_status));
            if (hider.getIsNight() != uiIsNight) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

        if (hider.getIsNight()) {
            // Night

            btPrimary.setText(getText(R.string.dawn));
            btPrimary.setOnClickListener(this::buttonDawn);
            btSecondary.setText(getText(R.string.dusk));
            btSecondary.setOnClickListener(this::buttonDusk);

            tvStatus.setText(getText(R.string.night_status));
            tvStatusInfo.setText(getText(R.string.night_info));
        }

        if (!hider.getIsNight()) {
            // Day

            btPrimary.setText(getText(R.string.dusk));
            btPrimary.setOnClickListener(this::buttonDusk);
            btSecondary.setText(getText(R.string.dawn));
            btSecondary.setOnClickListener(this::buttonDawn);

            tvStatus.setText(getText(R.string.day_status));
            tvStatusInfo.setText(getText(R.string.day_info));
        }
    }


    @Override
    protected void onResume() {
        updateUi(false);
        super.onResume();
    }

}




