package deltazero.amarok.ui;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.utils.AppInfoUtil.AppInfo;
import deltazero.amarok.utils.ConfigPorter.ImportResult;
import deltazero.amarok.utils.ShortcutUtil;

/**
 * 配置导入结果页。
 *
 * <p>快捷方式批量创建通过 PendingIntent 回调串行化：每次调用
 * {@link ShortcutManager#requestPinShortcut} 时传入一个 PendingIntent，
 * 系统在用户确认（或设备静默确认）后发送广播，收到广播后触发下一个，
 * 避免连续调用被节流/丢弃。
 */
public class ImportResultActivity extends AmarokActivity {

    private static final String TAG = "ImportResultActivity";

    public static final String EXTRA_HIDE_IMPORTED = "hide_imported";
    public static final String EXTRA_HIDE_SKIPPED  = "hide_skipped";
    public static final String EXTRA_SC_IMPORTED   = "sc_imported";
    public static final String EXTRA_SC_SKIPPED    = "sc_skipped";
    public static final String EXTRA_FILE_IMPORTED = "file_imported";
    public static final String EXTRA_FILE_SKIPPED  = "file_skipped";

    private static final String ACTION_SHORTCUT_PINNED =
            "deltazero.amarok.ACTION_SHORTCUT_PINNED";

    private BroadcastReceiver shortcutPinnedReceiver;
    private List<String> pendingShortcuts;
    private AtomicInteger shortcutIndex;
    private MaterialButton btnCreateShortcuts;

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
        List<String> fileImported = getIntent().getStringArrayListExtra(EXTRA_FILE_IMPORTED);
        List<String> fileSkipped  = getIntent().getStringArrayListExtra(EXTRA_FILE_SKIPPED);

        if (hideImported == null) hideImported = List.of();
        if (hideSkipped  == null) hideSkipped  = List.of();
        if (scImported   == null) scImported   = List.of();
        if (scSkipped    == null) scSkipped    = List.of();
        if (fileImported == null) fileImported = List.of();
        if (fileSkipped  == null) fileSkipped  = List.of();

        TextView tvHideResult = findViewById(R.id.import_result_tv_hide);
        TextView tvScResult   = findViewById(R.id.import_result_tv_sc);
        TextView tvFileResult = findViewById(R.id.import_result_tv_file);
        btnCreateShortcuts    = findViewById(R.id.import_result_btn_create_shortcuts);
        MaterialButton btnDone = findViewById(R.id.import_result_btn_done);

        tvHideResult.setText(buildHideText(hideImported, hideSkipped));
        tvScResult.setText(buildScText(scImported, scSkipped));
        tvFileResult.setText(buildFileText(fileImported, fileSkipped));

        pendingShortcuts = new ArrayList<>(scImported);
        shortcutIndex = new AtomicInteger(0);

        if (pendingShortcuts.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            btnCreateShortcuts.setVisibility(View.GONE);
        } else {
            btnCreateShortcuts.setVisibility(View.VISIBLE);
            btnCreateShortcuts.setOnClickListener(v -> startShortcutCreation());
        }

        btnDone.setOnClickListener(v -> finish());
    }

    // -------------------------------------------------------------------------
    // 串行快捷方式创建
    // -------------------------------------------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startShortcutCreation() {
        btnCreateShortcuts.setEnabled(false);
        btnCreateShortcuts.setText(R.string.import_sc_creating);

        // 注册广播接收器：每次 pin 完成后收到通知，驱动下一个
        shortcutPinnedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pinNextShortcut();
            }
        };
        ContextCompat.registerReceiver(
                this, shortcutPinnedReceiver,
                new IntentFilter(ACTION_SHORTCUT_PINNED),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        // 触发第一个
        pinNextShortcut();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pinNextShortcut() {
        int idx = shortcutIndex.getAndIncrement();
        if (idx >= pendingShortcuts.size()) {
            // 全部完成
            if (shortcutPinnedReceiver != null) {
                unregisterReceiver(shortcutPinnedReceiver);
                shortcutPinnedReceiver = null;
            }
            runOnUiThread(() -> {
                btnCreateShortcuts.setText(R.string.import_sc_done);
            });
            return;
        }

        String pkg = pendingShortcuts.get(idx);
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            AppInfo appInfo = new AppInfo(
                    pkg,
                    ai.loadLabel(pm).toString(),
                    (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                    false,
                    ai.loadIcon(pm)
            );
            pinShortcutWithCallback(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App not found, skip: " + pkg);
            // 跳过这个，继续下一个
            pinNextShortcut();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pinShortcutWithCallback(AppInfo app) {
        ShortcutManager sm = getSystemService(ShortcutManager.class);
        if (!sm.isRequestPinShortcutSupported()) {
            // 不支持 pin，直接全部标记完成
            shortcutIndex.set(pendingShortcuts.size());
            pinNextShortcut();
            return;
        }

        Intent launchIntent = new Intent(this,
                deltazero.amarok.ui.ShortcutLaunchActivity.class);
        launchIntent.setAction(Intent.ACTION_VIEW);
        launchIntent.putExtra(ShortcutLaunchActivity.EXTRA_TARGET_PACKAGE, app.packageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this,
                ShortcutUtil.shortcutId(app.packageName()))
                .setShortLabel(app.label())
                .setLongLabel(app.label())
                .setIcon(Icon.createWithBitmap(drawableToBitmap(app.icon())))
                .setIntent(launchIntent)
                .build();

        // 创建回调 PendingIntent：pin 完成后系统发此广播
        Intent callbackIntent = new Intent(ACTION_SHORTCUT_PINNED);
        callbackIntent.setPackage(getPackageName());
        PendingIntent callback = PendingIntent.getBroadcast(
                this, idx(app.packageName()),
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        sm.requestPinShortcut(shortcut, callback.getIntentSender());

        // 同步更新 Prefs（ShortcutUtil.createShortcut 内部会做，这里直接复用逻辑）
        // 不调 ShortcutUtil.createShortcut 是因为需要自定义 callback
        java.util.Set<String> shortcuts = PrefMgr.getShortcutApps();
        shortcuts.add(app.packageName());
        PrefMgr.setShortcutApps(shortcuts);
    }

    /** 为每个包名生成稳定的 requestCode，避免 PendingIntent 冲突 */
    private int idx(String pkg) {
        return (pkg.hashCode() & 0x7fffffff) % 10000;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shortcutPinnedReceiver != null) {
            try { unregisterReceiver(shortcutPinnedReceiver); } catch (Exception ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // 文本构建
    // -------------------------------------------------------------------------

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

    private String buildFileText(List<String> imported, List<String> skipped) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.import_file_success, imported.size()));
        if (!imported.isEmpty()) {
            sb.append("：").append(String.join(", ", imported));
        }
        sb.append("\n");
        sb.append(getString(R.string.import_file_skipped, skipped.size()));
        if (!skipped.isEmpty()) {
            sb.append("（").append(getString(R.string.import_not_exist)).append("）：")
              .append(String.join(", ", skipped));
        }
        return sb.toString();
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        int w = drawable.getIntrinsicWidth()  > 0 ? drawable.getIntrinsicWidth()  : 96;
        int h = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 96;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bmp;
    }

    // -------------------------------------------------------------------------
    // Intent builder
    // -------------------------------------------------------------------------

    public static Intent buildIntent(Context context, ImportResult result) {
        Intent intent = new Intent(context, ImportResultActivity.class);
        intent.putStringArrayListExtra(EXTRA_HIDE_IMPORTED, new ArrayList<>(result.hideImported));
        intent.putStringArrayListExtra(EXTRA_HIDE_SKIPPED,  new ArrayList<>(result.hideSkipped));
        intent.putStringArrayListExtra(EXTRA_SC_IMPORTED,   new ArrayList<>(result.scImported));
        intent.putStringArrayListExtra(EXTRA_SC_SKIPPED,    new ArrayList<>(result.scSkipped));
        intent.putStringArrayListExtra(EXTRA_FILE_IMPORTED, new ArrayList<>(result.fileImported));
        intent.putStringArrayListExtra(EXTRA_FILE_SKIPPED,  new ArrayList<>(result.fileSkipped));
        return intent;
    }
}
