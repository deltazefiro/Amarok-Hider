package deltazero.amarok.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppInfoUtil.AppInfo;
import deltazero.amarok.utils.ConfigPorter.ImportResult;
import deltazero.amarok.utils.ShortcutUtil;

/**
 * 配置导入结果页。
 * 通过 Intent extras 接收 {@link ImportResult} 中的各列表并展示。
 * 若有需要创建快捷方式的应用，提供"开始创建快捷方式"按钮批量调用。
 */
public class ImportResultActivity extends AmarokActivity {

    public static final String EXTRA_HIDE_IMPORTED = "hide_imported";
    public static final String EXTRA_HIDE_SKIPPED  = "hide_skipped";
    public static final String EXTRA_SC_IMPORTED   = "sc_imported";
    public static final String EXTRA_SC_SKIPPED    = "sc_skipped";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_result);

        MaterialToolbar toolbar = findViewById(R.id.import_result_tb);
        toolbar.setNavigationOnClickListener(v -> finish());

        List<String> hideImported = getIntent().getStringArrayListExtra(EXTRA_HIDE_IMPORTED);
        List<String> hideSkipped  = getIntent().getStringArrayListExtra(EXTRA_HIDE_SKIPPED);
        List<String> scImported   = getIntent().getStringArrayListExtra(EXTRA_SC_IMPORTED);
        List<String> scSkipped    = getIntent().getStringArrayListExtra(EXTRA_SC_SKIPPED);

        if (hideImported == null) hideImported = List.of();
        if (hideSkipped  == null) hideSkipped  = List.of();
        if (scImported   == null) scImported   = List.of();
        if (scSkipped    == null) scSkipped    = List.of();

        TextView tvHideResult = findViewById(R.id.import_result_tv_hide);
        TextView tvScResult   = findViewById(R.id.import_result_tv_sc);
        MaterialButton btnCreateShortcuts = findViewById(R.id.import_result_btn_create_shortcuts);
        MaterialButton btnDone = findViewById(R.id.import_result_btn_done);

        tvHideResult.setText(buildHideText(hideImported, hideSkipped));
        tvScResult.setText(buildScText(scImported, scSkipped));

        // 只有有需要创建快捷方式的应用时才显示按钮
        final List<String> scToCreate = new ArrayList<>(scImported);
        if (scToCreate.isEmpty()) {
            btnCreateShortcuts.setVisibility(View.GONE);
        } else {
            btnCreateShortcuts.setVisibility(View.VISIBLE);
            btnCreateShortcuts.setOnClickListener(v -> {
                createShortcutsFor(scToCreate);
                btnCreateShortcuts.setEnabled(false);
                btnCreateShortcuts.setText(R.string.import_sc_creating);
            });
        }

        btnDone.setOnClickListener(v -> finish());
    }

    /** 逐个调用 requestPinShortcut，在支持静默创建的设备（如小米）上无感完成 */
    private void createShortcutsFor(List<String> packageNames) {
        PackageManager pm = getPackageManager();
        for (String pkg : packageNames) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                AppInfo appInfo = new AppInfo(
                        pkg,
                        ai.loadLabel(pm).toString(),
                        (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                        false,
                        ai.loadIcon(pm)
                );
                ShortcutUtil.createShortcut(this, appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                // 理论上不会发生（导入时已过滤未安装），忽略
            }
        }
    }

    private String buildHideText(List<String> imported, List<String> skipped) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.import_hide_success, imported.size()));
        if (!imported.isEmpty()) {
            sb.append("：").append(String.join(", ", imported));
        }
        sb.append("\n");
        sb.append(getString(R.string.import_hide_skipped, skipped.size()));
        if (!skipped.isEmpty()) {
            sb.append("（").append(getString(R.string.import_not_installed)).append("）：")
              .append(String.join(", ", skipped));
        }
        return sb.toString();
    }

    private String buildScText(List<String> imported, List<String> skipped) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.import_sc_pending, imported.size()));
        sb.append("\n");
        sb.append(getString(R.string.import_sc_skipped, skipped.size()));
        if (!skipped.isEmpty()) {
            sb.append("（").append(getString(R.string.import_not_installed)).append("）：")
              .append(String.join(", ", skipped));
        }
        return sb.toString();
    }

    /** 构建用于启动本 Activity 的 Intent */
    public static Intent buildIntent(Context context, ImportResult result) {
        Intent intent = new Intent(context, ImportResultActivity.class);
        intent.putStringArrayListExtra(EXTRA_HIDE_IMPORTED, new ArrayList<>(result.hideImported));
        intent.putStringArrayListExtra(EXTRA_HIDE_SKIPPED,  new ArrayList<>(result.hideSkipped));
        intent.putStringArrayListExtra(EXTRA_SC_IMPORTED,   new ArrayList<>(result.scImported));
        intent.putStringArrayListExtra(EXTRA_SC_SKIPPED,    new ArrayList<>(result.scSkipped));
        return intent;
    }
}
