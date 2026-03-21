package deltazero.amarok.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import deltazero.amarok.R;
import deltazero.amarok.ui.ImportResultActivity;
import deltazero.amarok.utils.ConfigPorter;
import deltazero.amarok.utils.ConfigPorter.ImportResult;

/**
 * 设置页"数据"分类：配置导出 / 导入。
 *
 * <p>注意：ActivityResultLauncher 必须在 Activity onCreate 之前注册，
 * 因此 launcher 由外部（SettingsFragment）预先注册后通过构造函数传入。
 * 参见 {@link SettingsActivity.SettingsFragment}。
 */
public class DataCategory extends BaseCategory {

    public interface ExportLauncher {
        void launch(String fileName);
    }

    public interface ImportLauncher {
        void launch();
    }

    public DataCategory(
            @NonNull FragmentActivity activity,
            @NonNull PreferenceScreen screen,
            @NonNull ExportLauncher exportLauncher,
            @NonNull ImportLauncher importLauncher) {
        super(activity, screen);
        setTitle(R.string.data_category_title);

        // --- 导出按钮 ---
        Preference exportPref = new Preference(activity);
        exportPref.setTitle(R.string.export_config);
        exportPref.setSummary(R.string.export_config_description);
        exportPref.setIcon(R.drawable.settings_backup_restore_black_24dp);
        exportPref.setOnPreferenceClickListener(pref -> {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            exportLauncher.launch("amarok_config_" + timestamp + ".json");
            return true;
        });
        addPreference(exportPref);

        // --- 导入按钮 ---
        Preference importPref = new Preference(activity);
        importPref.setTitle(R.string.import_config);
        importPref.setSummary(R.string.import_config_description);
        importPref.setIcon(R.drawable.settings_backup_restore_black_24dp);
        importPref.setOnPreferenceClickListener(pref -> {
            importLauncher.launch();
            return true;
        });
        addPreference(importPref);
    }
}
