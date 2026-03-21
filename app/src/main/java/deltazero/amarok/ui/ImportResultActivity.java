package deltazero.amarok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.R;
import deltazero.amarok.utils.ConfigPorter.ImportResult;

/**
 * 配置导入结果页。
 * 通过 Intent extras 接收 {@link ImportResult} 中的各列表并展示。
 */
public class ImportResultActivity extends AmarokActivity {

    public static final String EXTRA_HIDE_IMPORTED  = "hide_imported";
    public static final String EXTRA_HIDE_SKIPPED   = "hide_skipped";
    public static final String EXTRA_SC_IMPORTED    = "sc_imported";
    public static final String EXTRA_SC_SKIPPED     = "sc_skipped";

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
        MaterialButton btnDone = findViewById(R.id.import_result_btn_done);

        tvHideResult.setText(buildHideText(hideImported, hideSkipped));
        tvScResult.setText(buildScText(scImported, scSkipped));

        btnDone.setOnClickListener(v -> finish());
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
        sb.append(getString(R.string.import_sc_marked, imported.size()));
        if (!imported.isEmpty()) {
            sb.append("：").append(String.join(", ", imported));
            sb.append("\n").append(getString(R.string.import_sc_hint));
        }
        sb.append("\n");
        sb.append(getString(R.string.import_sc_skipped, skipped.size()));
        if (!skipped.isEmpty()) {
            sb.append("（").append(getString(R.string.import_not_installed)).append("）：")
              .append(String.join(", ", skipped));
        }
        return sb.toString();
    }

    /** 构建用于启动本 Activity 的 Intent */
    public static Intent buildIntent(android.content.Context context, ImportResult result) {
        Intent intent = new Intent(context, ImportResultActivity.class);
        intent.putStringArrayListExtra(EXTRA_HIDE_IMPORTED, new java.util.ArrayList<>(result.hideImported));
        intent.putStringArrayListExtra(EXTRA_HIDE_SKIPPED,  new java.util.ArrayList<>(result.hideSkipped));
        intent.putStringArrayListExtra(EXTRA_SC_IMPORTED,   new java.util.ArrayList<>(result.scImported));
        intent.putStringArrayListExtra(EXTRA_SC_SKIPPED,    new java.util.ArrayList<>(result.scSkipped));
        return intent;
    }
}
