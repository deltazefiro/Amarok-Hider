package deltazero.amarok.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Objects;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.ImportResultActivity;
import deltazero.amarok.utils.ConfigPorter;

public class SettingsActivity extends AmarokActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getTheme().applyStyle(R.style.Theme_Amarok_Preference, true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.setting_container, new SettingsFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                setTitle(R.string.more_settings);
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        MaterialToolbar toolbar = findViewById(R.id.settings_tb_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate())
            return true;
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory()
                .instantiate(getClassLoader(), Objects.requireNonNull(pref.getFragment()));

        fragment.setArguments(pref.getExtras());
        fragment.setTargetFragment(caller, 0);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.setting_container, fragment)
                .addToBackStack(null)
                .commit();

        setTitle(pref.getTitle());
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private BaseCategory[] categories;

        // ActivityResultLauncher 必须在 Fragment attached 后、STARTED 前注册
        private final ActivityResultLauncher<String> exportLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.CreateDocument("application/json"),
                        this::onExportUriSelected);

        private final ActivityResultLauncher<String[]> importLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        this::onImportUriSelected);

        private void onExportUriSelected(Uri uri) {
            if (uri == null) return;
            boolean ok = ConfigPorter.export(requireContext(), uri);
            Toast.makeText(requireContext(),
                    ok ? R.string.export_success : R.string.export_failed,
                    Toast.LENGTH_SHORT).show();
        }

        private void onImportUriSelected(Uri uri) {
            if (uri == null) return;
            ConfigPorter.ImportResult result = ConfigPorter.importFrom(requireContext(), uri);
            if (result == null) {
                Toast.makeText(requireContext(), R.string.import_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = ImportResultActivity.buildIntent(requireContext(), result);
            startActivity(intent);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(PrefMgr.MAIN_PREF_FILENAME);
            Context context = getPreferenceManager().getContext();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

            categories = new BaseCategory[]{
                    new WorkmodeCategory(requireActivity(), screen),
                    new XHideCategory(requireActivity(), screen),
                    new PrivacyCategory(requireActivity(), screen),
                    new QuickHideCategory(requireActivity(), screen),
                    new AppearanceCategory(requireActivity(), screen),
                    new DataCategory(
                            requireActivity(), screen,
                            fileName -> exportLauncher.launch(fileName),
                            () -> importLauncher.launch(new String[]{"application/json", "*/*"})),
                    new UpdateCategory(requireActivity(), screen),
                    new AboutCategory(requireActivity(), screen),
            };

            setPreferenceScreen(screen);
        }

        @Override
        public void onResume() {
            super.onResume();
            for (var c : categories) c.notifyUpdate();
        }
    }
}